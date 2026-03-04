import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UsernameAvailabilityChecker {

    private ConcurrentHashMap<String, Integer> usernameToUserId = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Integer> attemptFrequency = new ConcurrentHashMap<>();
    private AtomicInteger userIdGenerator = new AtomicInteger(1);

    public boolean checkAvailability(String username) {
        attemptFrequency.merge(username, 1, Integer::sum);
        return !usernameToUserId.containsKey(username);
    }

    public void registerUser(String username) {
        usernameToUserId.put(username, userIdGenerator.getAndIncrement());
    }

    public List<String> suggestAlternatives(String username) {
        List<String> suggestions = new ArrayList<>();
        int count = 1;

        while (suggestions.size() < 3) {
            String suggestion = username + count;
            if (!usernameToUserId.containsKey(suggestion)) {
                suggestions.add(suggestion);
            }
            count++;
        }

        String modified = username.replace("_", ".");
        if (!usernameToUserId.containsKey(modified)) {
            suggestions.add(modified);
        }

        return suggestions;
    }

    public String getMostAttempted() {
        return attemptFrequency.entrySet()
                .stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("No attempts yet");
    }

    public static void main(String[] args) {

        UsernameAvailabilityChecker system = new UsernameAvailabilityChecker();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("\n===== Username Availability System =====");
            System.out.println("1. Check Username Availability");
            System.out.println("2. Register Username");
            System.out.println("3. Get Suggested Usernames");
            System.out.println("4. Get Most Attempted Username");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");

            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {

                case 1:
                    System.out.print("Enter username to check: ");
                    String usernameCheck = sc.nextLine();
                    boolean available = system.checkAvailability(usernameCheck);
                    if (available) {
                        System.out.println("Username '" + usernameCheck + "' is AVAILABLE.");
                    } else {
                        System.out.println("Username '" + usernameCheck + "' is already TAKEN.");
                    }
                    break;

                case 2:
                    System.out.print("Enter username to register: ");
                    String usernameRegister = sc.nextLine();
                    if (system.checkAvailability(usernameRegister)) {
                        system.registerUser(usernameRegister);
                        System.out.println("Username '" + usernameRegister + "' registered successfully.");
                    } else {
                        System.out.println("Username already taken. Try suggestions.");
                    }
                    break;

                case 3:
                    System.out.print("Enter username for suggestions: ");
                    String usernameSuggest = sc.nextLine();
                    List<String> suggestions = system.suggestAlternatives(usernameSuggest);
                    System.out.println("Suggested available usernames:");
                    for (String s : suggestions) {
                        System.out.println(s);
                    }
                    break;

                case 4:
                    System.out.println("Most attempted username: " + system.getMostAttempted());
                    break;

                case 5:
                    System.out.println("Exiting system...");
                    sc.close();
                    System.exit(0);

                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }
}