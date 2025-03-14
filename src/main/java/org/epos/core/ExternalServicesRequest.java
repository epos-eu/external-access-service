package org.epos.core;

import okhttp3.*;

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
        // First try system DNS - most reliable
        try {
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            if (addresses.length > 0) {
                return addresses[0].getHostAddress();
            }
        } catch (UnknownHostException e) {
            // Continue to external services
            LOGGER.info("System DNS failed for " + hostname + ", trying external services");
        }

        // Try public DNS over HTTPS services
        for (String dnsServiceUrl : EXTERNAL_DNS_SERVICES) {
            try {
                String resolvedIp = queryPublicDnsApi(String.format(dnsServiceUrl, hostname));
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
            String resolvedIp = queryDnsApi("https://dns.google.com/resolve?name=" + hostname + "&type=A");
            if (resolvedIp != null) return resolvedIp;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Google DNS API failed", e);
        }

        try {
            String resolvedIp = queryDnsApi("https://cloudflare-dns.com/dns-query?name=" + hostname + "&type=A");
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

        // No resolution found
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

            String responseText = body.string();

            // Simple JSON parsing for IP extraction
            if (responseText.contains("\"Answer\"")) {
                // Extract the first IP address from the response
                int dataIndex = responseText.indexOf("\"data\":\"");
                if (dataIndex > 0) {
                    int startIndex = dataIndex + 8;
                    int endIndex = responseText.indexOf("\"", startIndex);
                    if (endIndex > startIndex) {
                        String ip = responseText.substring(startIndex, endIndex);
                        if (isValidIpAddress(ip)) {
                            return ip;
                        }
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

        // Very simple JSON parsing for IP address
        String responseText = response.toString();
        if (responseText.contains("\"Answer\"")) {
            // Extract the first IP address from the response
            int dataIndex = responseText.indexOf("\"data\":\"");
            if (dataIndex > 0) {
                int startIndex = dataIndex + 8;
                int endIndex = responseText.indexOf("\"", startIndex);
                if (endIndex > startIndex) {
                    String ip = responseText.substring(startIndex, endIndex);
                    if (isValidIpAddress(ip)) {
                        return ip;
                    }
                }
            }
        }

        return null;
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
     * Execute a general request with the robust client
     */
    public Response executeRequest(Request request) throws IOException {
        return client.newCall(request).execute();
    }

    /**
     * Executes a request directly using an IP address instead of hostname
     * This is useful for bypassing DNS resolution completely
     *
     * @param ipAddress The IP address to connect to
     * @param hostname The original hostname (for SNI/Host header)
     * @param path The path of the request (e.g., "/api/data")
     * @param method The HTTP method (GET, POST, etc.)
     * @param isHttps Whether to use HTTPS (true) or HTTP (false)
     * @param headers Additional HTTP headers to include
     * @param requestBody The request body for POST/PUT requests (null for GET/HEAD)
     * @return The response body as a string
     * @throws IOException If an I/O error occurs
     */
    public String executeRequestByIp(String ipAddress, String hostname, String path,
                                     String method, boolean isHttps,
                                     Map<String, String> headers,
                                     RequestBody requestBody) throws IOException {
        // Validate IP address
        if (!isValidIpAddress(ipAddress)) {
            throw new IllegalArgumentException("Invalid IP address: " + ipAddress);
        }

        // Construct URL with IP instead of hostname
        String protocol = isHttps ? "https" : "http";
        String url = protocol + "://" + ipAddress + path;

        // Build request with the Host header
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .method(method, requestBody);

        // Add Host header for SNI/virtual hosting
        requestBuilder.header("Host", hostname);

        // Add other headers if provided
        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }
        }

        // Create custom client that bypasses DNS resolution for this IP
        OkHttpClient ipClient = client.newBuilder()
                .dns(new Dns() {
                    @Override
                    public List<InetAddress> lookup(String host) throws UnknownHostException {
                        // For the target IP, return it directly
                        if (host.equals(ipAddress)) {
                            return Collections.singletonList(InetAddress.getByName(ipAddress));
                        }
                        // For other hostnames, use the default resolver
                        return Dns.SYSTEM.lookup(host);
                    }
                })
                .build();

        try (Response response = ipClient.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }

            ResponseBody body = response.body();
            return body != null ? body.string() : "";
        }
    }

    /**
     * Simplified version of executeRequestByIp for GET requests
     *
     * @param ipAddress The IP address to connect to
     * @param hostname The original hostname (for SNI/Host header)
     * @param path The path of the request
     * @param isHttps Whether to use HTTPS
     * @return The response body as a string
     * @throws IOException If an I/O error occurs
     */
    public String getByIp(String ipAddress, String hostname, String path, boolean isHttps) throws IOException {
        return executeRequestByIp(ipAddress, hostname, path, "GET", isHttps, null, null);
    }

    /**
     * POST request using direct IP
     *
     * @param ipAddress The IP address to connect to
     * @param hostname The original hostname (for SNI/Host header)
     * @param path The path of the request
     * @param isHttps Whether to use HTTPS
     * @param contentType The content type of the request body
     * @param body The request body as string
     * @return The response body as a string
     * @throws IOException If an I/O error occurs
     */
    public String postByIp(String ipAddress, String hostname, String path, boolean isHttps,
                           String contentType, String body) throws IOException {
        RequestBody requestBody = RequestBody.create(
                MediaType.parse(contentType),
                body != null ? body : ""
        );

        return executeRequestByIp(ipAddress, hostname, path, "POST", isHttps, null, requestBody);
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
    private static class AdvancedDnsResolver implements Dns {
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
            ((ExternalServicesRequest) hostToIpMap.getClass().getEnclosingClass()).markHostAsProblematic(hostname);

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