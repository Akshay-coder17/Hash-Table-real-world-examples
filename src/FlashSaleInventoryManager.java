import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FlashSaleInventoryManager {

    private ConcurrentHashMap<String, AtomicInteger> stockMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, ConcurrentLinkedQueue<Integer>> waitingListMap = new ConcurrentHashMap<>();

    public void addProduct(String productId, int stock) {
        stockMap.put(productId, new AtomicInteger(stock));
        waitingListMap.put(productId, new ConcurrentLinkedQueue<>());
    }

    public int checkStock(String productId) {
        AtomicInteger stock = stockMap.get(productId);
        return stock != null ? stock.get() : 0;
    }

    public String purchaseItem(String productId, int userId) {
        AtomicInteger stock = stockMap.get(productId);
        if (stock == null) {
            return "Product not found";
        }

        while (true) {
            int currentStock = stock.get();
            if (currentStock > 0) {
                if (stock.compareAndSet(currentStock, currentStock - 1)) {
                    return "Success, " + (currentStock - 1) + " units remaining";
                }
            } else {
                ConcurrentLinkedQueue<Integer> queue = waitingListMap.get(productId);
                queue.add(userId);
                return "Added to waiting list, position #" + queue.size();
            }
        }
    }

    public int getWaitingListPosition(String productId, int userId) {
        ConcurrentLinkedQueue<Integer> queue = waitingListMap.get(productId);
        if (queue == null) return -1;

        int position = 1;
        for (Integer id : queue) {
            if (id == userId) return position;
            position++;
        }
        return -1;
    }

    public static void main(String[] args) throws InterruptedException {

        FlashSaleInventoryManager manager = new FlashSaleInventoryManager();
        manager.addProduct("IPHONE15_256GB", 100);

        System.out.println("Initial Stock: " + manager.checkStock("IPHONE15_256GB") + " units available");

        ExecutorService executor = Executors.newFixedThreadPool(50);

        for (int i = 1; i <= 150; i++) {
            final int userId = i;
            executor.execute(() -> {
                String result = manager.purchaseItem("IPHONE15_256GB", userId);
                System.out.println("User " + userId + ": " + result);
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        System.out.println("Final Stock: " + manager.checkStock("IPHONE15_256GB"));
    }
}