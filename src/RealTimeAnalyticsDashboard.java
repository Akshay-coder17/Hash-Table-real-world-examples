import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RealTimeAnalyticsDashboard {

    static class PageStats {
        AtomicInteger visitCount = new AtomicInteger(0);
        Set<String> uniqueVisitors = ConcurrentHashMap.newKeySet();
    }

    private ConcurrentHashMap<String, PageStats> pageStatsMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, AtomicInteger> sourceCountMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public RealTimeAnalyticsDashboard() {
        scheduler.scheduleAtFixedRate(this::printDashboard, 5, 5, TimeUnit.SECONDS);
    }

    public void processEvent(String url, String userId, String source) {
        PageStats stats = pageStatsMap.computeIfAbsent(url, k -> new PageStats());
        stats.visitCount.incrementAndGet();
        stats.uniqueVisitors.add(userId);
        sourceCountMap.computeIfAbsent(source, k -> new AtomicInteger(0))
                      .incrementAndGet();
    }

    public void printDashboard() {

        System.out.println("\n===== REAL-TIME DASHBOARD =====");

        PriorityQueue<Map.Entry<String, PageStats>> pq =
                new PriorityQueue<>(
                        (a, b) -> b.getValue().visitCount.get() - a.getValue().visitCount.get()
                );

        pq.addAll(pageStatsMap.entrySet());

        System.out.println("Top Pages:");

        int rank = 1;
        while (!pq.isEmpty() && rank <= 10) {
            Map.Entry<String, PageStats> entry = pq.poll();
            System.out.println(rank + ". " + entry.getKey() +
                    " - " + entry.getValue().visitCount.get() +
                    " views (" + entry.getValue().uniqueVisitors.size() + " unique)");
            rank++;
        }

        int totalSourceVisits = sourceCountMap.values()
                .stream()
                .mapToInt(AtomicInteger::get)
                .sum();

        System.out.println("\nTraffic Sources:");

        for (Map.Entry<String, AtomicInteger> entry : sourceCountMap.entrySet()) {
            double percent = totalSourceVisits == 0 ? 0 :
                    (entry.getValue().get() * 100.0) / totalSourceVisits;
            System.out.println(entry.getKey() + ": " +
                    String.format("%.2f", percent) + "%");
        }

        System.out.println("================================\n");
    }

    public static void main(String[] args) throws InterruptedException {

        RealTimeAnalyticsDashboard dashboard = new RealTimeAnalyticsDashboard();
        ExecutorService executor = Executors.newFixedThreadPool(20);

        String[] urls = {
                "/article/breaking-news",
                "/sports/championship",
                "/tech/ai-update",
                "/health/tips"
        };

        String[] sources = {"google", "facebook", "direct", "twitter"};

        Random random = new Random();

        for (int i = 0; i < 100000; i++) {
            final int userId = i;
            executor.execute(() -> {
                String url = urls[random.nextInt(urls.length)];
                String source = sources[random.nextInt(sources.length)];
                dashboard.processEvent(url, "user_" + userId, source);
            });
        }

        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }
}