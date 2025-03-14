package org.epos.core;

import okhttp3.*;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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

    // Configurable cache TTL in milliseconds
    private final long ipCacheTtlMs;

    // Properties file name for IPs
    private static final String IP_MAPPINGS_FILE = "host-ip-mappings.properties";
    private final String ipMappingsPath;

    // Environment variable prefix for IP mappings
    private static final String ENV_PREFIX = "SERVICE_IP_";

    /**
     * Default constructor with a 1-hour cache TTL
     */
    public ExternalServicesRequest() {
        this(3600000, null);
    }

    /**
     * Constructor with configurable cache TTL and optional custom mappings file path
     *
     * @param ipCacheTtlMs Time-to-live for IP cache entries in milliseconds
     * @param ipMappingsPath Custom path to host-IP mappings properties file (null for default)
     */
    public ExternalServicesRequest(long ipCacheTtlMs, String ipMappingsPath) {
        this.ipCacheTtlMs = ipCacheTtlMs;
        this.ipMappingsPath = ipMappingsPath;

        // Load IP mappings from various sources
        loadMappingsFromEnvironment();
        loadMappingsFromProperties();

        // Configure DNS resolver with automatic IP resolution
        AutoIpDns autoIpDns = new AutoIpDns(hostToIpMap, ipCache, ipCacheTtlMs);

        // Build the OkHttp client with our custom configurations
        client = new OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .dns(autoIpDns)
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
     * Executes a GET request to the provided URL with automatic IP resolution
     */
    public String getResponseBodyWithFallback(String url) {
        String result = "";
        List<String> errors = new ArrayList<>();

        try {
            // First try with normal flow (will use our custom DNS resolver)
            result = getResponseBody(url);
            return result;
        } catch (IOException e) {
            errors.add("Primary attempt failed: " + e.getMessage());

            try {
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

            // Try to resolve and cache IP if it's a DNS issue
            try {
                String hostname = new java.net.URL(url).getHost();
                String resolvedIp = resolveHostnameDirectly(hostname);

                if (resolvedIp != null) {
                    // Cache this resolution for future use
                    ipCache.put(hostname, new IpMappingEntry(resolvedIp, System.currentTimeMillis() + ipCacheTtlMs));

                    // Try again with the resolved IP
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
            } catch (Exception urlEx) {
                errors.add("IP fallback failed: " + urlEx.getMessage());
            }
        }

        // If all attempts failed, return error summary
        throw new RuntimeException("All attempts to access URL failed. Errors: " + String.join("; ", errors));
    }

    /**
     * Attempt to resolve a hostname directly using system DNS
     */
    private String resolveHostnameDirectly(String hostname) {
        try {
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            if (addresses.length > 0) {
                return addresses[0].getHostAddress();
            }
        } catch (Exception e) {
            // DNS resolution failed
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
     * DNS resolver that automatically resolves and caches IPs
     */
    private static class AutoIpDns implements Dns {
        private final Dns systemDns = Dns.SYSTEM;
        private final Map<String, String> hostToIpMap;
        private final Map<String, IpMappingEntry> ipCache;
        private final long ipCacheTtlMs;

        public AutoIpDns(Map<String, String> hostToIpMap, Map<String, IpMappingEntry> ipCache, long ipCacheTtlMs) {
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