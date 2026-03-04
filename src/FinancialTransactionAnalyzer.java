import java.util.*;

public class FinancialTransactionAnalyzer {

    static class Transaction {
        int id;
        double amount;
        String merchant;
        String account;
        long timestamp;

        Transaction(int id, double amount, String merchant, String account, long timestamp) {
            this.id = id;
            this.amount = amount;
            this.merchant = merchant;
            this.account = account;
            this.timestamp = timestamp;
        }

        public String toString() {
            return "id:" + id + " amt:" + amount;
        }
    }

    public static List<List<Transaction>> findTwoSum(List<Transaction> transactions, double target) {
        Map<Double, Transaction> map = new HashMap<>();
        List<List<Transaction>> result = new ArrayList<>();

        for (Transaction t : transactions) {
            double complement = target - t.amount;
            if (map.containsKey(complement)) {
                result.add(Arrays.asList(map.get(complement), t));
            }
            map.put(t.amount, t);
        }
        return result;
    }

    public static List<List<Transaction>> findTwoSumWithTimeWindow(
            List<Transaction> transactions, double target, long windowMillis) {

        transactions.sort(Comparator.comparingLong(t -> t.timestamp));
        List<List<Transaction>> result = new ArrayList<>();
        Map<Double, List<Transaction>> map = new HashMap<>();

        int left = 0;
        for (int right = 0; right < transactions.size(); right++) {

            while (transactions.get(right).timestamp - 
                   transactions.get(left).timestamp > windowMillis) {
                List<Transaction> list = map.get(transactions.get(left).amount);
                list.remove(transactions.get(left));
                if (list.isEmpty()) map.remove(transactions.get(left).amount);
                left++;
            }

            Transaction t = transactions.get(right);
            double complement = target - t.amount;

            if (map.containsKey(complement)) {
                for (Transaction match : map.get(complement)) {
                    result.add(Arrays.asList(match, t));
                }
            }

            map.computeIfAbsent(t.amount, k -> new ArrayList<>()).add(t);
        }
        return result;
    }

    public static List<List<Transaction>> findKSum(
            List<Transaction> transactions, int k, double target) {

        transactions.sort(Comparator.comparingDouble(t -> t.amount));
        return kSumHelper(transactions, 0, k, target);
    }

    private static List<List<Transaction>> kSumHelper(
            List<Transaction> transactions, int start, int k, double target) {

        List<List<Transaction>> result = new ArrayList<>();

        if (k == 2) {
            Map<Double, Transaction> map = new HashMap<>();
            for (int i = start; i < transactions.size(); i++) {
                Transaction t = transactions.get(i);
                double complement = target - t.amount;
                if (map.containsKey(complement)) {
                    result.add(Arrays.asList(map.get(complement), t));
                }
                map.put(t.amount, t);
            }
            return result;
        }

        for (int i = start; i < transactions.size(); i++) {
            Transaction current = transactions.get(i);
            List<List<Transaction>> sub = 
                    kSumHelper(transactions, i + 1, k - 1, target - current.amount);
            for (List<Transaction> list : sub) {
                List<Transaction> newList = new ArrayList<>();
                newList.add(current);
                newList.addAll(list);
                result.add(newList);
            }
        }
        return result;
    }

    public static List<Map<String, Object>> detectDuplicates(
            List<Transaction> transactions) {

        Map<String, List<Transaction>> map = new HashMap<>();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Transaction t : transactions) {
            String key = t.amount + "|" + t.merchant;
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        for (Map.Entry<String, List<Transaction>> entry : map.entrySet()) {
            Set<String> accounts = new HashSet<>();
            for (Transaction t : entry.getValue()) {
                accounts.add(t.account);
            }
            if (accounts.size() > 1) {
                Map<String, Object> dup = new HashMap<>();
                dup.put("amount", entry.getValue().get(0).amount);
                dup.put("merchant", entry.getValue().get(0).merchant);
                dup.put("accounts", accounts);
                result.add(dup);
            }
        }
        return result;
    }

    public static void main(String[] args) {

        long baseTime = System.currentTimeMillis();

        List<Transaction> transactions = Arrays.asList(
                new Transaction(1, 500, "Store A", "acc1", baseTime),
                new Transaction(2, 300, "Store B", "acc2", baseTime + 900000),
                new Transaction(3, 200, "Store C", "acc3", baseTime + 1800000),
                new Transaction(4, 500, "Store A", "acc2", baseTime + 2000000)
        );

        System.out.println("Two-Sum:");
        System.out.println(findTwoSum(transactions, 500));

        System.out.println("\nTwo-Sum within 1 hour:");
        System.out.println(findTwoSumWithTimeWindow(transactions, 500, 3600000));

        System.out.println("\nK-Sum (k=3, target=1000):");
        System.out.println(findKSum(transactions, 3, 1000));

        System.out.println("\nDuplicate Detection:");
        System.out.println(detectDuplicates(transactions));
    }
}