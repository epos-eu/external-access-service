package org.epos.core;

import okhttp3.*;

import javax.net.ssl.SSLPeerUnverifiedException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ExternalServicesRequest {
    private final OkHttpClient client;
    private final int MAX_RETRIES = 3;
    private final int CONNECTION_TIMEOUT = 30; // seconds
    private final int READ_TIMEOUT = 30; // seconds
    private final int WRITE_TIMEOUT = 30; // seconds

    // Default fallback DNS servers (configurable)
    private final String[] fallbackDnsServers;

    /**
     * Default constructor that uses system DNS with internal fallbacks
     */
    public ExternalServicesRequest() {
        this(null);
    }

    /**
     * Constructor with configurable fallback DNS servers
     * @param fallbackDnsServers Array of DNS servers to use as fallback, null for defaults
     */
    public ExternalServicesRequest(String[] fallbackDnsServers) {
        // Use provided DNS servers or empty array to rely on system/K8s DNS only
        this.fallbackDnsServers = fallbackDnsServers != null ? fallbackDnsServers : new String[0];

        // Configure DNS resolver with fallback mechanisms
        Dns robustDns = new RobustDns(this.fallbackDnsServers);

        // Build the OkHttp client with our custom configurations
        client = new OkHttpClient.Builder()
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                .dns(robustDns)
                .addInterceptor(new RetryInterceptor(MAX_RETRIES))
                .build();
    }

    /**
     * Executes a GET request to the provided URL and returns the response body as a string
     * with enhanced error handling
     */
    public String getResponseBodyWithFallback(String url) {
        String result = "";
        List<String> errors = new ArrayList<>();

        // Try main request
        try {
            result = getResponseBody(url);
            return result;
        } catch (IOException e) {
            errors.add("Primary attempt failed: " + e.getMessage());

            // If standard request fails, try again with explicit connection close
            // This can help with connection pool issues in containerized environments
            try {
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

            // Last resort - if DNS is the issue and we have fallback servers configured
            if (e instanceof UnknownHostException && fallbackDnsServers.length > 0) {
                try {
                    // Extract hostname from URL to resolve it directly
                    String hostname = new java.net.URL(url).getHost();
                    String resolvedIp = resolveHostnameDirectly(hostname);

                    if (resolvedIp != null) {
                        // Don't replace in URL as that might break TLS/SNI - instead use IP with hostname in header
                        Request request = new Request.Builder()
                                .url(url)
                                .header("Host", hostname)
                                .build();

                        // Create a new client that will connect to the IP but send hostname in TLS SNI
                        OkHttpClient ipClient = client.newBuilder()
                                .dns(hostname1 -> {
                                    if (hostname1.equals(hostname)) {
                                        return Collections.singletonList(InetAddress.getByName(resolvedIp));
                                    }
                                    return client.dns().lookup(hostname1);
                                })
                                .build();

                        try (Response response = ipClient.newCall(request).execute()) {
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
     * Custom DNS resolver that implements fallback mechanisms
     * Respects Kubernetes DNS configurations
     */
    private static class RobustDns implements Dns {
        private final Dns systemDns = Dns.SYSTEM;
        private final OkHttpDnsCache dnsCache = new OkHttpDnsCache();
        private final String[] fallbackDnsServers;

        public RobustDns(String[] fallbackDnsServers) {
            this.fallbackDnsServers = fallbackDnsServers;
        }

        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            // Always check cache first
            List<InetAddress> cachedAddresses = dnsCache.get(hostname);
            if (cachedAddresses != null && !cachedAddresses.isEmpty()) {
                return cachedAddresses;
            }

            // Primary lookup: Use system DNS (this includes Kubernetes DNS in cluster)
            try {
                List<InetAddress> addresses = systemDns.lookup(hostname);
                if (!addresses.isEmpty()) {
                    dnsCache.put(hostname, addresses);
                    return addresses;
                }
            } catch (UnknownHostException e) {
                // System DNS failed, continue to fallbacks
            }

            // Check if hostname is already an IP address
            try {
                InetAddress ipAddress = InetAddress.getByName(hostname);
                if (ipAddress.getHostAddress().equals(hostname)) {
                    return Collections.singletonList(ipAddress);
                }
            } catch (UnknownHostException e) {
                // Not an IP address, continue to alternative resolution
            }

            // We rely on system fallbacks and don't override with custom DNS servers in Kubernetes
            // This prevents conflicts with cluster DNS
            // For non-Kubernetes environments, fallback DNS can be enabled via constructor
            if (fallbackDnsServers.length > 0) {
                try {
                    // This would be an implementation using fallback DNS servers
                    // In production, this would require a proper DNS library
                    InetAddress[] addresses = InetAddress.getAllByName(hostname);
                    List<InetAddress> resultList = Arrays.asList(addresses);
                    if (!resultList.isEmpty()) {
                        dnsCache.put(hostname, resultList);
                        return resultList;
                    }
                } catch (UnknownHostException e) {
                    // Alternative lookup failed
                }
            }

            // All attempts failed, throw exception
            throw new UnknownHostException("Unable to resolve host " + hostname);
        }
    }

    /**
     * Simple DNS cache implementation
     */
    private static class OkHttpDnsCache {
        // In a production system, use a more sophisticated cache with TTL and eviction
        private final java.util.Map<String, List<InetAddress>> cache =
                Collections.synchronizedMap(new java.util.HashMap<>());

        public List<InetAddress> get(String hostname) {
            return cache.get(hostname);
        }

        public void put(String hostname, List<InetAddress> addresses) {
            cache.put(hostname, addresses);
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