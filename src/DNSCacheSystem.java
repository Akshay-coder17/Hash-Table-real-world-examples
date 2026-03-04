import java.util.*;
import java.util.concurrent.*;

public class DNSCacheSystem {

    static class DNSEntry {
        String domain;
        String ipAddress;
        long expiryTime;
        long createdTime;

        DNSEntry(String domain, String ipAddress, long ttlSeconds) {
            this.domain = domain;
            this.ipAddress = ipAddress;
            this.createdTime = System.currentTimeMillis();
            this.expiryTime = createdTime + (ttlSeconds * 1000);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    private final int maxSize;
    private final Map<String, DNSEntry> cache;
    private long hits = 0;
    private long misses = 0;
    private long totalLookupTime = 0;

    public DNSCacheSystem(int maxSize) {
        this.maxSize = maxSize;
        this.cache = Collections.synchronizedMap(
                new LinkedHashMap<String, DNSEntry>(16, 0.75f, true) {
                    protected boolean removeEldestEntry(Map.Entry<String, DNSEntry> eldest) {
                        return size() > DNSCacheSystem.this.maxSize;
                    }
                });
        startCleanupThread();
    }

    public String resolve(String domain, long ttlSeconds) {
        long start = System.nanoTime();
        DNSEntry entry;

        synchronized (cache) {
            entry = cache.get(domain);
            if (entry != null) {
                if (!entry.isExpired()) {
                    hits++;
                    long end = System.nanoTime();
                    totalLookupTime += (end - start);
                    return "Cache HIT → " + entry.ipAddress;
                } else {
                    cache.remove(domain);
                }
            }
        }

        misses++;
        String newIp = queryUpstreamDNS(domain);
        DNSEntry newEntry = new DNSEntry(domain, newIp, ttlSeconds);

        synchronized (cache) {
            cache.put(domain, newEntry);
        }

        long end = System.nanoTime();
        totalLookupTime += (end - start);

        return "Cache MISS → Query upstream → " + newIp;
    }

    private String queryUpstreamDNS(String domain) {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "172.217.14." + new Random().nextInt(255);
    }

    private void startCleanupThread() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            synchronized (cache) {
                Iterator<Map.Entry<String, DNSEntry>> iterator = cache.entrySet().iterator();
                while (iterator.hasNext()) {
                    if (iterator.next().getValue().isExpired()) {
                        iterator.remove();
                    }
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    public String getCacheStats() {
        long totalRequests = hits + misses;
        double hitRate = totalRequests == 0 ? 0 : (hits * 100.0 / totalRequests);
        double avgLookupMs = totalRequests == 0 ? 0 :
                (totalLookupTime / 1_000_000.0) / totalRequests;

        return "Hit Rate: " + String.format("%.2f", hitRate) +
                "%, Avg Lookup Time: " + String.format("%.3f", avgLookupMs) + " ms";
    }

    public static void main(String[] args) throws InterruptedException {

        DNSCacheSystem dnsCache = new DNSCacheSystem(5);

        System.out.println(dnsCache.resolve("google.com", 3));
        System.out.println(dnsCache.resolve("google.com", 3));

        Thread.sleep(4000);

        System.out.println(dnsCache.resolve("google.com", 3));

        System.out.println(dnsCache.getCacheStats());
    }
}