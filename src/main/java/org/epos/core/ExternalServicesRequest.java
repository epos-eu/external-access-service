package org.epos.core;

import okhttp3.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExternalServicesRequest {
    private static final Logger LOGGER = Logger.getLogger(ExternalServicesRequest.class.getName());

    private final OkHttpClient client;
    private final int MAX_RETRIES = 3;
    private final int CONNECTION_TIMEOUT = 30; // seconds
    private final int READ_TIMEOUT = 30; // seconds
    private final int WRITE_TIMEOUT = 30; // seconds

    // Map to store host-to-IP mappings for services that need direct IP access
    private final Map<String, String> hostToIpMap = new ConcurrentHashMap<>();

    // Cache resolved IPs to avoid repeated lookups
    private final Map<String, IpMappingEntry> ipCache = new ConcurrentHashMap<>();

    // Set to remember hosts that have had DNS resolution problems
    private final Set<String> problematicHostsCache = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Configurable cache TTL in milliseconds
    private final long ipCacheTtlMs;

    // Properties file name for IPs
    private static final String IP_MAPPINGS_FILE = "host-ip-mappings.properties";
    private final String ipMappingsPath;

    // Environment variable prefix for IP mappings
    private static final String ENV_PREFIX = "SERVICE_IP_";

    // External DNS resolvers - order matters (tried in sequence)
    private static final String[] EXTERNAL_DNS_SERVICES = {
            "https://dns.google/resolve?name=%s&type=A",       // Google DNS
            "https://cloudflare-dns.com/dns-query?name=%s&type=A", // Cloudflare DNS
            "https://1.1.1.1/dns-query?name=%s&type=A",        // Cloudflare alternate
            "https://8.8.8.8/resolve?name=%s&type=A"           // Google DNS alternate
    };

    // Flag to enable/disable direct IP connection attempts
    private final boolean enableDirectIpConnections;

    /**
     * Default constructor with a 1-hour cache TTL
     */
    public ExternalServicesRequest() {
        this(3600000, null, true);
    }

    /**
     * Constructor with configurable cache TTL and optional custom mappings file path
     *
     * @param ipCacheTtlMs Time-to-live for IP cache entries in milliseconds
     * @param ipMappingsPath Custom path to host-IP mappings properties file (null for default)
     */
    public ExternalServicesRequest(long ipCacheTtlMs, String ipMappingsPath) {
        this(ipCacheTtlMs, ipMappingsPath, true);
    }

    /**
     * Constructor with configurable cache TTL, mappings file path, and direct IP connection option
     *
     * @param ipCacheTtlMs Time-to-live for IP cache entries in milliseconds
     * @param ipMappingsPath Custom path to host-IP mappings properties file (null for default)
     * @param enableDirectIpConnections Whether to enable direct IP connection attempts
     */
    public ExternalServicesRequest(long ipCacheTtlMs, String ipMappingsPath, boolean enableDirectIpConnections) {
        this.ipCacheTtlMs = ipCacheTtlMs;
        this.ipMappingsPath = ipMappingsPath;
        this.enableDirectIpConnections = enableDirectIpConnections;

        // Load IP mappings from various sources
        loadMappingsFromEnvironment();
        loadMappingsFromProperties();

        // Configure DNS resolver with automatic IP resolution
        AdvancedDnsResolver advancedDns = new AdvancedDnsResolver(hostToIpMap, ipCache, ipCacheTtlMs);

        // Build the OkHttp client with our custom configurations
        client = new OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .dns(advancedDns)
                .hostnameVerifier((hostname, session) -> {
                    // For direct IP connections, skip hostname verification
                    if (isValidIpAddress(hostname)) {
                        return true;
                    }

                    // For normal domain names, use standard verification
                    return HttpsURLConnection.getDefaultHostnameVerifier().verify(hostname, session);
                })
                .addInterceptor(new RetryInterceptor(MAX_RETRIES))
                .build();
    }

    /**
     * Load IP mappings from environment variables
     * Format: SERVICE_IP_[hostname with dots replaced by underscores] = [IP address]
     * Example: SERVICE_IP_api_example_com=192.168.1.100
     */
    private void loadMappingsFromEnvironment() {
        Map<String, String> env = System.getenv();
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith(ENV_PREFIX)) {
                String hostname = key.substring(ENV_PREFIX.length())
                        .toLowerCase()
                        .replace('_', '.');
                String ip = entry.getValue();
                if (isValidIpAddress(ip)) {
                    hostToIpMap.put(hostname, ip);
                    LOGGER.info("Loaded IP mapping from environment: " + hostname + " -> " + ip);
                } else {
                    LOGGER.warning("Invalid IP format in environment variable " + key + ": " + ip);
                }
            }
        }
    }

    /**
     * Load IP mappings from properties file
     * Format is hostname=ipaddress
     */
    private void loadMappingsFromProperties() {
        Properties props = new Properties();

        // Try to load from custom path if provided
        if (ipMappingsPath != null) {
            loadPropertiesFromPath(props, ipMappingsPath);
        } else {
            // Try multiple standard locations
            String[] paths = {
                    IP_MAPPINGS_FILE,
                    System.getProperty("user.dir") + File.separator + IP_MAPPINGS_FILE,
                    System.getProperty("user.home") + File.separator + IP_MAPPINGS_FILE,
                    "/etc/" + IP_MAPPINGS_FILE,
                    "/config/" + IP_MAPPINGS_FILE  // Common mount point in containers
            };

            boolean loaded = false;
            for (String path : paths) {
                if (loadPropertiesFromPath(props, path)) {
                    loaded = true;
                    break;
                }
            }

            if (!loaded) {
                LOGGER.info("No IP mappings file found in standard locations");
            }
        }

        // Process the loaded properties
        for (String hostname : props.stringPropertyNames()) {
            String ip = props.getProperty(hostname);
            if (isValidIpAddress(ip)) {
                hostToIpMap.put(hostname, ip);
                LOGGER.info("Loaded IP mapping from properties: " + hostname + " -> " + ip);
            } else {
                LOGGER.warning("Invalid IP format in properties for " + hostname + ": " + ip);
            }
        }
    }

    /**
     * Helper method to load properties from a specific path
     */
    private boolean loadPropertiesFromPath(Properties props, String path) {
        File file = new File(path);
        if (file.exists() && file.canRead()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                LOGGER.info("Loaded IP mappings from " + path);
                return true;
            } catch (IOException e) {
                LOGGER.warning("Failed to load IP mappings from " + path + ": " + e.getMessage());
            }
        }
        return false;
    }

    /**
     * Validate IP address format
     */
    private boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }

        try {
            // Simple validation - convert to InetAddress to verify format
            InetAddress.getByName(ip);
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Executes a GET request to the provided URL with automatic IP resolution and extensive fallbacks
     */
    public String getResponseBodyWithFallback(String url) {
        String result = "";
        List<String> errors = new ArrayList<>();

        try {
            // Extract hostname for potential special handling
            String hostname = new java.net.URL(url).getHost();

            // Check if this is a problematic host that might need direct IP connection
            // Instead of waiting for failure, try to pre-resolve the IP first
            if (isLikelyProblematicHost(hostname)) {
                // Try to resolve the hostname using external methods first
                String resolvedIp = resolveUsingExternalDns(hostname);
                if (resolvedIp != null && enableDirectIpConnections) {
                    LOGGER.info("Pre-emptively using direct IP connection for likely problematic host: " + hostname);

                    try {
                        URL originalUrl = new URL(url);
                        boolean isHttps = "https".equalsIgnoreCase(originalUrl.getProtocol());
                        int port = originalUrl.getPort();
                        String portPart = (port != -1) ? ":" + port : "";
                        String path = originalUrl.getPath() + (originalUrl.getQuery() != null ? "?" + originalUrl.getQuery() : "");

                        // Use our direct IP method
                        String response = getByIp(resolvedIp + portPart, hostname, path, isHttps);

                        // Store the IP mapping for future use
                        hostToIpMap.put(hostname, resolvedIp);
                        ipCache.put(hostname, new IpMappingEntry(resolvedIp, System.currentTimeMillis() + ipCacheTtlMs));
                        LOGGER.info("Successfully connected to " + hostname + " using IP " + resolvedIp + " (pre-emptive)");

                        return response;
                    } catch (Exception directIpEx) {
                        errors.add("Pre-emptive direct IP connection failed: " + directIpEx.getMessage());
                        // Fall through to standard flow
                    }
                }
            }

            // Standard attempt with normal flow (will use our custom DNS resolver)
            try {
                result = getResponseBody(url);
                return result;
            } catch (IOException standardEx) {
                errors.add("Standard attempt failed: " + standardEx.getMessage());
            }
        } catch (IOException e) {
            errors.add("URL parsing failed: " + e.getMessage());

            try {
                // Extract hostname for fallback methods
                String hostname = new java.net.URL(url).getHost();

                // Try again with connection close header
                Request request = new Request.Builder()
                        .url(url)
                        .header("Connection", "close") // Force connection close
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected response code: " + response);
                    }

                    ResponseBody body = response.body();
                    return body != null ? body.string() : "";
                }
            } catch (IOException retryEx) {
                errors.add("Retry with connection close failed: " + retryEx.getMessage());
            }

            // Last resort: Try advanced external resolution methods
            try {
                String hostname = new java.net.URL(url).getHost();

                // Try to resolve using external DNS services if not already resolved
                String resolvedIp = resolveUsingExternalDns(hostname);

                if (resolvedIp != null && enableDirectIpConnections) {
                    // Cache this resolution for future use
                    ipCache.put(hostname, new IpMappingEntry(resolvedIp, System.currentTimeMillis() + ipCacheTtlMs));

                    // Try connection with the resolved IP using our specialized method
                    try {
                        URL originalUrl = new URL(url);
                        boolean isHttps = "https".equalsIgnoreCase(originalUrl.getProtocol());
                        int port = originalUrl.getPort();
                        String portPart = (port != -1) ? ":" + port : "";
                        String path = originalUrl.getPath() + (originalUrl.getQuery() != null ? "?" + originalUrl.getQuery() : "");

                        // Use our direct IP method
                        String response = getByIp(resolvedIp + portPart, hostname, path, isHttps);

                        // Store the IP mapping for future use
                        hostToIpMap.put(hostname, resolvedIp);
                        LOGGER.info("Successfully connected to " + hostname + " using IP " + resolvedIp);

                        return response;
                    } catch (Exception connEx) {
                        errors.add("Direct IP connection with resolved IP failed: " + connEx.getMessage());
                    }

                    // Fall back to manual connection if our specialized method fails
                    try {
                        // Create a modified URL with the IP instead of hostname
                        URL originalUrl = new URL(url);
                        URL ipUrl = new URL(originalUrl.getProtocol(), resolvedIp,
                                originalUrl.getPort(), originalUrl.getFile());

                        HttpURLConnection conn = (HttpURLConnection) ipUrl.openConnection();
                        conn.setRequestProperty("Host", hostname); // Set original hostname for SNI
                        conn.setConnectTimeout(CONNECTION_TIMEOUT * 1000);
                        conn.setReadTimeout(READ_TIMEOUT * 1000);

                        int responseCode = conn.getResponseCode();
                        if (responseCode >= 200 && responseCode < 300) {
                            // Read the response
                            java.io.InputStream in = conn.getInputStream();
                            java.util.Scanner s = new java.util.Scanner(in).useDelimiter("\\A");
                            String response = s.hasNext() ? s.next() : "";

                            // Store the IP mapping for future use
                            hostToIpMap.put(hostname, resolvedIp);
                            LOGGER.info("Successfully connected to " + hostname + " using manual IP connection");

                            return response;
                        } else {
                            throw new IOException("Unexpected response code: " + responseCode);
                        }
                    } catch (Exception connEx) {
                        errors.add("Manual connection with resolved IP failed: " + connEx.getMessage());
                    }
                }
            } catch (Exception urlEx) {
                errors.add("Advanced resolution failed: " + urlEx.getMessage());
            }
        }

        // If all attempts failed, return error summary
        throw new RuntimeException("All attempts to access URL failed. Errors: " + String.join("; ", errors));
    }

    /**
     * Resolve a hostname using external DNS services
     * This is a fallback when all other methods fail
     */
    private String resolveUsingExternalDns(String hostname) {
        // First check if we already know the IP from our mapping
        String staticIp = hostToIpMap.get(hostname);
        if (staticIp != null && isValidIpAddress(staticIp)) {
            LOGGER.info("Using known IP mapping for " + hostname + ": " + staticIp);
            return staticIp;
        }

        // Try system DNS - most reliable
        try {
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            if (addresses.length > 0) {
                return addresses[0].getHostAddress();
            }
        } catch (UnknownHostException e) {
            // Continue to external services
            LOGGER.info("System DNS failed for " + hostname + ", trying external services");
        }

        // Try our CNAME-aware resolution
        String resolvedIp = resolveWithCnameSupport(hostname);
        if (resolvedIp != null) {
            LOGGER.info("Resolved " + hostname + " to " + resolvedIp + " using CNAME-aware resolution");
            return resolvedIp;
        }

        // Try public DNS over HTTPS services
        for (String dnsServiceUrl : EXTERNAL_DNS_SERVICES) {
            try {
                resolvedIp = queryPublicDnsApi(String.format(dnsServiceUrl, hostname));
                if (resolvedIp != null) {
                    LOGGER.info("Resolved " + hostname + " to " + resolvedIp + " using external DNS");
                    return resolvedIp;
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "External DNS query failed: " + e.getMessage(), e);
                // Continue to next service
            }
        }

        // Try public DNS lookup APIs (these are more likely to work in restricted environments)
        try {
            resolvedIp = queryDnsApi("https://dns.google.com/resolve?name=" + hostname + "&type=A");
            if (resolvedIp != null) return resolvedIp;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Google DNS API failed", e);
        }

        try {
            resolvedIp = queryDnsApi("https://cloudflare-dns.com/dns-query?name=" + hostname + "&type=A");
            if (resolvedIp != null) return resolvedIp;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Cloudflare DNS API failed", e);
        }

        // Try manual ping (works in some environments)
        try {
            Process process = Runtime.getRuntime().exec("ping -c 1 " + hostname);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("(") && line.contains(")")) {
                    int start = line.indexOf("(") + 1;
                    int end = line.indexOf(")", start);
                    if (end > start) {
                        String ip = line.substring(start, end);
                        if (isValidIpAddress(ip)) {
                            LOGGER.info("Resolved " + hostname + " to " + ip + " using ping");
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Ping resolution failed", e);
        }

        // Try nslookup if available
        try {
            Process process = Runtime.getRuntime().exec("nslookup " + hostname);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("Address:") || line.contains("Address: ")) {
                    String[] parts = line.split(":\\s*");
                    if (parts.length >= 2) {
                        String ip = parts[1].trim();
                        if (isValidIpAddress(ip)) {
                            LOGGER.info("Resolved " + hostname + " to " + ip + " using nslookup");
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "nslookup resolution failed", e);
        }

        // Try different public DNS resolvers via command line (useful on Linux systems)
        String[] publicDnsServers = {"8.8.8.8", "1.1.1.1", "9.9.9.9", "208.67.222.222"};
        for (String dnsServer : publicDnsServers) {
            try {
                Process process = Runtime.getRuntime().exec("dig @" + dnsServer + " " + hostname + " +short");
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (isValidIpAddress(line)) {
                        LOGGER.info("Resolved " + hostname + " to " + line + " using dig and " + dnsServer);
                        return line;
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "Dig resolution with " + dnsServer + " failed", e);
            }
        }

        // Try to deduce IP from related domains
        String relatedDomainIp = resolveFromRelatedDomain(hostname);
        if (relatedDomainIp != null) {
            return relatedDomainIp;
        }

        // Try to deduce IP from related domains
        try {
            relatedDomainIp = resolveFromRelatedDomain(hostname);
            if (relatedDomainIp != null) {
                LOGGER.info("Resolved " + hostname + " to " + relatedDomainIp + " using related domain");
                return relatedDomainIp;
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Related domain resolution failed", e);
        }

        // No resolution found
        return null;
    }

    /**
     * Specific handler for BGS (British Geological Survey) domains that have known issues
     */
    private String resolveBgsSpecificDomain(String hostname) {
        // Known BGS domains and their IPs
        Map<String, String> bgsDomainsMap = new HashMap<>();
        bgsDomainsMap.put("gifswebapi.bgs.ac.uk", "194.66.252.155");
        bgsDomainsMap.put("wdcapi.bgs.ac.uk", "194.66.252.156");
        bgsDomainsMap.put("wdc.bgs.ac.uk", "194.66.252.157");
        bgsDomainsMap.put("geo.irdr.ucl.ac.uk", "193.60.251.153");


        // Check for exact matches
        String ip = bgsDomainsMap.get(hostname);
        if (ip != null) {
            LOGGER.info("Using known BGS domain mapping for " + hostname + ": " + ip);
            return ip;
        }

        // For unknown BGS subdomains, try the main BGS IP range
        if (hostname.endsWith("bgs.ac.uk")) {
            // Try a common BGS IP
            String commonBgsIp = "194.66.252.155";
            LOGGER.info("Using common BGS IP range for " + hostname + ": " + commonBgsIp);
            return commonBgsIp;
        }

        return null;
    }

    /**
     * Try to deduce IP based on related domains that might be on the same server
     */
    private String resolveFromRelatedDomain(String hostname) {
        // Extract domain parts
        String[] parts = hostname.split("\\.");
        if (parts.length < 3) return null;

        // Generate possible related domains
        List<String> relatedDomains = new ArrayList<>();

        // Try with different subdomains
        if (hostname.startsWith("wdcapi.") || hostname.startsWith("api.")) {
            String baseDomain = hostname.substring(hostname.indexOf('.') + 1);
            relatedDomains.add("www." + baseDomain);
            relatedDomains.add("data." + baseDomain);
            relatedDomains.add("portal." + baseDomain);
        } else if (hostname.startsWith("www.")) {
            String baseDomain = hostname.substring(4);
            relatedDomains.add("api." + baseDomain);
            relatedDomains.add("data." + baseDomain);
        }

        // Try generic variations of the domain
        String domainWithoutSubdomain = parts.length > 2 ?
                String.join(".", Arrays.copyOfRange(parts, 1, parts.length)) : hostname;
        relatedDomains.add(domainWithoutSubdomain);

        // Try to resolve each related domain
        for (String relatedDomain : relatedDomains) {
            try {
                InetAddress[] addresses = InetAddress.getAllByName(relatedDomain);
                if (addresses.length > 0) {
                    String ip = addresses[0].getHostAddress();
                    LOGGER.info("Deduced IP for " + hostname + " from related domain " +
                            relatedDomain + ": " + ip);
                    return ip;
                }
            } catch (UnknownHostException e) {
                // Continue to next related domain
            }
        }

        return null;
    }

    /**
     * Query a public DNS API for hostname resolution
     */
    private String queryPublicDnsApi(String apiUrl) throws IOException {
        // Create a separate client just for DNS API queries
        OkHttpClient dnsClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .dns(Dns.SYSTEM) // Use system DNS for API queries
                .build();

        Request request = new Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/dns-json")
                .build();

        try (Response response = dnsClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return null;
            }

            ResponseBody body = response.body();
            if (body == null) return null;

            // Return the full response text for detailed parsing
            return body.string();
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Public DNS API query failed: " + e.getMessage(), e);
            return null;
        }
    }
    private String resolveWithCnameSupport(String hostname) {
        String currentName = hostname;
        Set<String> seenNames = new HashSet<>();
        seenNames.add(currentName);

        for (int i = 0; i < 5; i++) { // Limit to 5 redirections to prevent infinite loops
            try {
                String dnsResponse = queryDnsApi("https://dns.google.com/resolve?name=" + currentName + "&type=A");

                // Parse for CNAME first
                String cname = extractCnameTarget(dnsResponse);
                if (cname != null && !seenNames.contains(cname)) {
                    currentName = cname;
                    seenNames.add(cname);
                    continue;
                }

                // Look for A record
                String ip = extractIpFromResponse(dnsResponse);
                if (ip != null) {
                    return ip;
                }

                // If we got here, we couldn't find a resolution path
                break;
            } catch (Exception e) {
                LOGGER.log(Level.FINE, "DNS resolution failed", e);
                break;
            }
        }

        return null;
    }

    private String extractCnameTarget(String response) {
        // Look for type 5 (CNAME) records
        if (response.contains("\"type\":5")) {
            int typeIndex = response.indexOf("\"type\":5");
            int dataIndex = response.indexOf("\"data\":\"", typeIndex);
            if (dataIndex > 0) {
                int startIndex = dataIndex + 8;
                int endIndex = response.indexOf("\"", startIndex);
                if (endIndex > startIndex) {
                    String cname = response.substring(startIndex, endIndex);
                    // Remove trailing dot if present
                    if (cname.endsWith(".")) {
                        cname = cname.substring(0, cname.length() - 1);
                    }
                    return cname;
                }
            }
        }
        return null;
    }

    private String extractIpFromResponse(String response) {
        // Look for type 1 (A record)
        if (response.contains("\"type\":1")) {
            int typeIndex = response.indexOf("\"type\":1");
            int dataIndex = response.indexOf("\"data\":\"", typeIndex);
            if (dataIndex > 0) {
                int startIndex = dataIndex + 8;
                int endIndex = response.indexOf("\"", startIndex);
                if (endIndex > startIndex) {
                    String ip = response.substring(startIndex, endIndex);
                    if (isValidIpAddress(ip)) {
                        return ip;
                    }
                }
            }
        }
        return null;
    }
    /**
     * Query a DNS API for hostname resolution
     */
    private String queryDnsApi(String apiUrl) throws IOException {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/dns-json");

        if (conn.getResponseCode() != 200) {
            return null;
        }

        java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return response.toString();  // Return the full response for more detailed parsing
    }

    /**
     * Executes a GET request to the provided URL and returns the response body as a string
     */
    public String getResponseBody(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }

            ResponseBody body = response.body();
            return body != null ? body.string() : "";
        }
    }

    /**
     * Gets the headers from a URL
     */
    public Headers getHeaders(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .head() // HEAD request only gets headers
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }

            return response.headers();
        }
    }

    /**
     * Gets the content type from a URL
     */
    public String getContentType(String url) throws IOException {
        Headers headers = getHeaders(url);
        return headers.get("Content-Type");
    }

    /**
     * Retrieves both the content type and HTTP response code in a single request
     * @return A Response object containing both HTTP code and headers
     */
    public Response getResponseMetadata(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .head() // HEAD request to avoid downloading the body
                .build();

        return client.newCall(request).execute();
        // Note: The caller is responsible for closing this response
    }

    public Map<String, Object> getRedirect(String url) throws IOException {
        Response response = getResponseMetadata(url);

        Map<String, Object> responseMap = new HashMap<>();
        String contentType = response.headers().get("Content-Type");
        String httpStatusCode = String.valueOf(response.code());

        responseMap.put("content-type", contentType);
        responseMap.put("httpStatusCode", httpStatusCode);
        responseMap.put("redirect-url", url);
        return responseMap;
    }

    /**
     * Simplified version of executeRequestByIp for GET requests with enhanced error handling
     *
     * @param ipAddress The IP address to connect to
     * @param hostname The original hostname (for SNI/Host header)
     * @param path The path of the request
     * @param isHttps Whether to use HTTPS
     * @return The response body as a string
     * @throws IOException If an I/O error occurs
     */
    public String getByIp(String ipAddress, String hostname, String path, boolean isHttps) throws IOException {
        // Validate IP address
        if (!isValidIpAddress(ipAddress)) {
            throw new IllegalArgumentException("Invalid IP address: " + ipAddress);
        }

        // Construct URL with IP instead of hostname
        String protocol = isHttps ? "https" : "http";
        String url = protocol + "://" + ipAddress + path;

        LOGGER.info("Attempting direct IP connection to: " + url + " with hostname: " + hostname);

        // Create a custom OkHttpClient for this specific request with appropriate SSL settings
        OkHttpClient.Builder clientBuilder = client.newBuilder()
                .hostnameVerifier((host, session) -> {
                    // Always accept for direct IP connections
                    return true;
                });

        // For HTTPS connections, create a trust manager that doesn't validate certificate chains
        if (isHttps) {
            try {
                // Create a trust manager that doesn't validate certificate chains
                final javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[] {
                        new javax.net.ssl.X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[]{};
                            }
                        }
                };

                // Install the all-trusting trust manager
                final javax.net.ssl.SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

                // Create an SSL socket factory with our all-trusting manager
                final javax.net.ssl.SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

                clientBuilder.sslSocketFactory(sslSocketFactory, (javax.net.ssl.X509TrustManager)trustAllCerts[0]);
            } catch (Exception e) {
                LOGGER.warning("Failed to create custom SSL context: " + e.getMessage());
            }
        }

        OkHttpClient ipClient = clientBuilder.build();

        // Build request with the Host header
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Host", hostname) // Important for SNI
                .header("Connection", "close"); // Try to avoid connection reuse issues

        try (Response response = ipClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response.code() + " for " + url);
            }

            ResponseBody body = response.body();
            return body != null ? body.string() : "";
        } catch (SSLPeerUnverifiedException e) {
            LOGGER.warning("SSL verification failed for direct IP connection: " + e.getMessage());
            // Fallback to HTTP if HTTPS failed due to cert issues
            if (isHttps) {
                LOGGER.info("Trying fallback to HTTP for: " + hostname);
                return getByIp(ipAddress, hostname, path, false);
            }
            throw e;
        } catch (Exception e) {
            LOGGER.warning("Direct IP connection failed to " + url + ": " + e.getMessage());
            throw e;
        }
    }


    /**
     * Determine if a hostname is likely to be problematic for DNS resolution
     * This method uses heuristics to identify hosts that might need direct IP connection
     */
    private boolean isLikelyProblematicHost(String hostname) {
        // Check if we have previously failed to resolve this host
        if (problematicHostsCache.contains(hostname)) {
            return true;
        }

        // Check if this host has a static IP mapping
        if (hostToIpMap.containsKey(hostname)) {
            return true;
        }

        // Check for British Geological Survey domains which are known to be problematic
        if (hostname.contains("bgs.ac.uk")) {
            return true;
        }

        // Check for common patterns in problematic hosts
        // 1. Unusual TLDs that might cause DNS issues
        if (hostname.endsWith(".local") || hostname.endsWith(".internal") ||
                hostname.endsWith(".test") || hostname.endsWith(".example")) {
            return true;
        }

        // 2. Hostnames that contain IP-like patterns
        if (hostname.matches(".*\\d{1,3}-\\d{1,3}-\\d{1,3}-\\d{1,3}.*")) {
            return true;
        }

        // Otherwise, not a known problematic host
        return false;
    }

    /**
     * Record a hostname as problematic for future reference
     */
    private void markHostAsProblematic(String hostname) {
        problematicHostsCache.add(hostname);
    }

    /**
     * Entry in the IP mapping cache with expiration
     */
    private static class IpMappingEntry {
        private final String ipAddress;
        private final long expirationTimeMs;

        public IpMappingEntry(String ipAddress, long expirationTimeMs) {
            this.ipAddress = ipAddress;
            this.expirationTimeMs = expirationTimeMs;
        }

        public String getIpAddress() {
            return ipAddress;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTimeMs;
        }
    }

    /**
     * Advanced DNS resolver with multiple fallback methods
     */
    private class AdvancedDnsResolver implements Dns {
        private final Dns systemDns = Dns.SYSTEM;
        private final Map<String, String> hostToIpMap;
        private final Map<String, IpMappingEntry> ipCache;
        private final long ipCacheTtlMs;

        public AdvancedDnsResolver(Map<String, String> hostToIpMap, Map<String, IpMappingEntry> ipCache, long ipCacheTtlMs) {
            this.hostToIpMap = hostToIpMap;
            this.ipCache = ipCache;
            this.ipCacheTtlMs = ipCacheTtlMs;
        }

        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            // Check if we have a static mapping
            String staticIp = hostToIpMap.get(hostname);
            if (staticIp != null) {
                try {
                    LOGGER.fine("Using static IP mapping for " + hostname + ": " + staticIp);
                    return Collections.singletonList(InetAddress.getByName(staticIp));
                } catch (UnknownHostException e) {
                    LOGGER.warning("Invalid IP mapping for " + hostname + ": " + staticIp);
                    // Continue to other resolution methods
                }
            }

            // Check dynamic cache
            IpMappingEntry cacheEntry = ipCache.get(hostname);
            if (cacheEntry != null && !cacheEntry.isExpired()) {
                try {
                    LOGGER.fine("Using cached IP for " + hostname + ": " + cacheEntry.getIpAddress());
                    return Collections.singletonList(InetAddress.getByName(cacheEntry.getIpAddress()));
                } catch (UnknownHostException e) {
                    // Invalid cached entry, remove it
                    ipCache.remove(hostname);
                }
            } else if (cacheEntry != null) {
                // Entry expired, remove it
                ipCache.remove(hostname);
            }

            // Try system DNS first
            try {
                List<InetAddress> addresses = systemDns.lookup(hostname);
                if (!addresses.isEmpty()) {
                    // Cache successful resolution
                    String resolvedIp = addresses.get(0).getHostAddress();
                    LOGGER.fine("Resolved " + hostname + " to " + resolvedIp + " using system DNS");
                    ipCache.put(hostname, new IpMappingEntry(resolvedIp, System.currentTimeMillis() + ipCacheTtlMs));
                    return addresses;
                }
            } catch (UnknownHostException e) {
                // Fall through to try other methods
            }

            // Check if hostname is already an IP address
            try {
                InetAddress ipAddress = InetAddress.getByName(hostname);
                if (ipAddress.getHostAddress().equals(hostname)) {
                    return Collections.singletonList(ipAddress);
                }
            } catch (UnknownHostException e) {
                // Not an IP address, continue
            }

            // Log the resolution failure for visibility
            LOGGER.info("Standard DNS resolution failed for " + hostname + ", falling back to external methods");

            // Mark this hostname as problematic for future reference
            markHostAsProblematic(hostname);

            // All attempts failed, throw exception
            throw new UnknownHostException("Unable to resolve host " + hostname);
        }
    }

    /**
     * Interceptor for automatic retries on transient network failures
     */
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;

        public RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            IOException lastException = null;

            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    // Add exponential backoff between retries
                    if (attempt > 0) {
                        try {
                            long backoffMillis = (long) Math.pow(2, attempt) * 1000;
                            Thread.sleep(backoffMillis);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Retry interrupted", e);
                        }
                    }

                    return chain.proceed(request);
                } catch (IOException e) {
                    // Only retry on specific exceptions that might be transient
                    if (isRetryableError(e)) {
                        lastException = e;

                        // If we've exhausted retries, throw the last exception
                        if (attempt == maxRetries - 1) {
                            throw lastException;
                        }
                    } else {
                        // Non-retryable exception, just throw
                        throw e;
                    }
                }
            }

            // Should never reach here, but just in case
            throw new IOException("Unknown error after retry attempts");
        }

        private boolean isRetryableError(IOException e) {
            // Define which exceptions are considered retryable
            // Examples include SocketTimeoutException, ConnectException
            return e instanceof java.net.SocketTimeoutException ||
                    e instanceof java.net.ConnectException ||
                    e instanceof java.net.UnknownHostException ||
                    e.getMessage() != null && e.getMessage().contains("connection");
        }
    }
}