import java.util.*;

public class MultiLevelCacheSystem {

    static class VideoData {
        String videoId;
        String content;
        int accessCount;

        VideoData(String videoId, String content) {
            this.videoId = videoId;
            this.content = content;
            this.accessCount = 0;
        }
    }

    private final int L1_CAPACITY = 10000;
    private final int L2_CAPACITY = 100000;
    private final int PROMOTION_THRESHOLD = 3;

    private LinkedHashMap<String, VideoData> L1 =
            new LinkedHashMap<>(16, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<String, VideoData> eldest) {
                    return size() > L1_CAPACITY;
                }
            };

    private LinkedHashMap<String, VideoData> L2 =
            new LinkedHashMap<>(16, 0.75f, true) {
                protected boolean removeEldestEntry(Map.Entry<String, VideoData> eldest) {
                    return size() > L2_CAPACITY;
                }
            };

    private Map<String, VideoData> L3Database = new HashMap<>();

    private long L1Hits = 0, L2Hits = 0, L3Hits = 0;
    private long L1Miss = 0, L2Miss = 0;
    private long totalTime = 0, totalRequests = 0;

    public MultiLevelCacheSystem() {
        for (int i = 1; i <= 200000; i++) {
            L3Database.put("video_" + i,
                    new VideoData("video_" + i, "Content_of_video_" + i));
        }
    }

    public String getVideo(String videoId) {

        long start = System.nanoTime();
        totalRequests++;

        if (L1.containsKey(videoId)) {
            L1Hits++;
            simulateDelay(0.5);
            totalTime += elapsed(start);
            return "L1 Cache HIT";
        }

        L1Miss++;

        if (L2.containsKey(videoId)) {
            L2Hits++;
            VideoData data = L2.get(videoId);
            data.accessCount++;
            if (data.accessCount >= PROMOTION_THRESHOLD) {
                L1.put(videoId, data);
            }
            simulateDelay(5);
            totalTime += elapsed(start);
            return "L2 Cache HIT → Promoted to L1";
        }

        L2Miss++;

        if (L3Database.containsKey(videoId)) {
            L3Hits++;
            VideoData data = L3Database.get(videoId);
            data.accessCount++;
            L2.put(videoId, data);
            simulateDelay(150);
            totalTime += elapsed(start);
            return "L3 Database HIT → Added to L2";
        }

        totalTime += elapsed(start);
        return "Video Not Found";
    }

    public void invalidate(String videoId) {
        L1.remove(videoId);
        L2.remove(videoId);
        L3Database.remove(videoId);
    }

    private void simulateDelay(double millis) {
        try {
            Thread.sleep((long) millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private long elapsed(long start) {
        return System.nanoTime() - start;
    }

    public void getStatistics() {

        double L1HitRate = percentage(L1Hits, totalRequests);
        double L2HitRate = percentage(L2Hits, totalRequests);
        double L3HitRate = percentage(L3Hits, totalRequests);
        double overallHitRate = percentage(L1Hits + L2Hits + L3Hits, totalRequests);
        double avgTimeMs = totalRequests == 0 ? 0 :
                (totalTime / 1_000_000.0) / totalRequests;

        System.out.println("L1: Hit Rate " + format(L1HitRate) + "%");
        System.out.println("L2: Hit Rate " + format(L2HitRate) + "%");
        System.out.println("L3: Hit Rate " + format(L3HitRate) + "%");
        System.out.println("Overall: Hit Rate " + format(overallHitRate) +
                "%, Avg Time: " + format(avgTimeMs) + "ms");
    }

    private double percentage(long part, long total) {
        return total == 0 ? 0 : (part * 100.0 / total);
    }

    private String format(double val) {
        return String.format("%.2f", val);
    }

    public static void main(String[] args) {

        MultiLevelCacheSystem cache = new MultiLevelCacheSystem();

        System.out.println(cache.getVideo("video_123"));
        System.out.println(cache.getVideo("video_123"));
        System.out.println(cache.getVideo("video_123"));
        System.out.println(cache.getVideo("video_123"));

        System.out.println(cache.getVideo("video_999999"));

        cache.getStatistics();
    }
}