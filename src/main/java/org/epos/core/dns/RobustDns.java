package org.epos.core.dns;

import okhttp3.Dns;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;

public class RobustDns implements Dns {
    private static final Logger logger = LoggerFactory.getLogger(RobustDns.class);

    // Reverse DNS APIs (Multiple fallback providers)
    private static final String[] REVERSE_DNS_APIS = {
            "https://api.hackertarget.com/reversedns/?q=",
            "https://rdnslookup.com/api?ip=",
            "https://dns.google/resolve?name="
    };

    // Static mappings for internal networks
    private static final Map<String, String> STATIC_HOST_MAPPING = new ConcurrentHashMap<>() {{
        put("192.168.1.1", "router.local");
        put("10.0.0.1", "internal.gateway");
    }};

    // Local cache (hostname -> resolved IPs)
    private static final Map<String, List<InetAddress>> DNS_CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_TTL = 300_000; // Cache duration (5 minutes)

    // Executors
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(1);

    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        if (hostname == null || hostname.isEmpty()) {
            throw new UnknownHostException("Hostname is empty");
        }

        // Check local cache
        if (DNS_CACHE.containsKey(hostname)) {
            logger.info("Cache hit for {}", hostname);
            return DNS_CACHE.get(hostname);
        }

        // If hostname is an IP, try reverse lookup
        if (hostname.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            String resolvedHost = getReverseDns(hostname);
            if (resolvedHost != null) {
                return resolveWithRetries(resolvedHost);
            }
        }

        // Resolve using default system DNS (with retries)
        return resolveWithRetries(hostname);
    }

    private List<InetAddress> resolveWithRetries(String hostname) throws UnknownHostException {
        int maxRetries = 3;
        int delay = 100; // milliseconds

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                List<InetAddress> addresses = Arrays.asList(InetAddress.getAllByName(hostname));
                if (!addresses.isEmpty()) {
                    // Cache successful lookups
                    DNS_CACHE.put(hostname, addresses);
                    scheduleCacheEviction(hostname);
                    return addresses;
                }
            } catch (UnknownHostException e) {
                logger.warn("DNS lookup failed for {} (Attempt {}/{})", hostname, attempt, maxRetries);
                try {
                    Thread.sleep(delay);
                    delay *= 2; // Exponential backoff
                } catch (InterruptedException ignored) {}
            }
        }

        // Last resort: Try Google DNS (8.8.8.8) or Cloudflare (1.1.1.1)
        return resolveWithPublicDns(hostname);
    }

    private String getReverseDns(String ip) {
        if (STATIC_HOST_MAPPING.containsKey(ip)) {
            return STATIC_HOST_MAPPING.get(ip);
        }

        List<Future<String>> futures = new ArrayList<>();

        for (String api : REVERSE_DNS_APIS) {
            futures.add(EXECUTOR.submit(() -> fetchReverseDns(api + ip)));
        }

        for (Future<String> future : futures) {
            try {
                String resolvedHost = future.get(3, TimeUnit.SECONDS);
                if (resolvedHost != null) {
                    return resolvedHost;
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.warn("Reverse DNS lookup failed: {}", e.getMessage());
            }
        }

        return null;
    }

    private String fetchReverseDns(String url) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(3, TimeUnit.SECONDS)
                .build();

        Request request = new Request.Builder().url(url).build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String body = response.body().string().trim();
                if (!body.isEmpty() && !body.contains("error")) {
                    return body.split("\n")[0]; // Extract first valid hostname
                }
            }
        } catch (IOException e) {
            logger.error("Reverse DNS API request failed: {}", e.getMessage());
        }
        return null;
    }

    private List<InetAddress> resolveWithPublicDns(String hostname) throws UnknownHostException {
        String[] publicDnsServers = {"8.8.8.8", "1.1.1.1"};
        for (String dnsServer : publicDnsServers) {
            try {
                Process process = new ProcessBuilder("nslookup", hostname, dnsServer).start();
                process.waitFor();
                return Arrays.asList(InetAddress.getAllByName(hostname));
            } catch (Exception e) {
                logger.warn("Public DNS {} failed for {}", dnsServer, hostname);
            }
        }
        throw new UnknownHostException("Unable to resolve hostname: " + hostname);
    }

    private void scheduleCacheEviction(String hostname) {
        SCHEDULED_EXECUTOR.schedule(() -> {
            logger.info("Evicting cache entry for {}", hostname);
            DNS_CACHE.remove(hostname);
        }, CACHE_TTL, TimeUnit.MILLISECONDS);
    }

}
