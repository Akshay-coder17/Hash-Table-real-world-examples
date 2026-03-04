import java.util.*;

public class ParkingLotManagement {

    enum Status { EMPTY, OCCUPIED, DELETED }

    static class ParkingSpot {
        String licensePlate;
        long entryTime;
        Status status = Status.EMPTY;
    }

    private ParkingSpot[] table;
    private int capacity;
    private int size = 0;
    private int totalProbes = 0;
    private Map<Integer, Integer> hourlyCount = new HashMap<>();

    public ParkingLotManagement(int capacity) {
        this.capacity = capacity;
        table = new ParkingSpot[capacity];
        for (int i = 0; i < capacity; i++) {
            table[i] = new ParkingSpot();
        }
    }

    private int hash(String license) {
        return Math.abs(license.hashCode()) % capacity;
    }

    public void parkVehicle(String license) {
        int index = hash(license);
        int probes = 0;

        while (probes < capacity) {
            int spot = (index + probes) % capacity;
            if (table[spot].status == Status.EMPTY || table[spot].status == Status.DELETED) {
                table[spot].licensePlate = license;
                table[spot].entryTime = System.currentTimeMillis();
                table[spot].status = Status.OCCUPIED;
                size++;
                totalProbes += probes;
                int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                hourlyCount.put(hour, hourlyCount.getOrDefault(hour, 0) + 1);
                System.out.println("Assigned spot #" + spot + " (" + probes + " probes)");
                return;
            }
            probes++;
        }
        System.out.println("Parking Full");
    }

    public void exitVehicle(String license) {
        int index = hash(license);
        int probes = 0;

        while (probes < capacity) {
            int spot = (index + probes) % capacity;
            if (table[spot].status == Status.EMPTY) break;
            if (table[spot].status == Status.OCCUPIED &&
                table[spot].licensePlate.equals(license)) {

                long exitTime = System.currentTimeMillis();
                long durationMillis = exitTime - table[spot].entryTime;
                double hours = durationMillis / (1000.0 * 60 * 60);
                double fee = Math.ceil(hours) * 5;

                table[spot].status = Status.DELETED;
                table[spot].licensePlate = null;
                size--;

                System.out.println("Spot #" + spot + " freed");
                System.out.println("Duration: " + String.format("%.2f", hours) + " hours");
                System.out.println("Fee: $" + String.format("%.2f", fee));
                return;
            }
            probes++;
        }
        System.out.println("Vehicle not found");
    }

    public int findNearestAvailable() {
        for (int i = 0; i < capacity; i++) {
            if (table[i].status == Status.EMPTY) return i;
        }
        return -1;
    }

    public void getStatistics() {
        double occupancy = (size * 100.0) / capacity;
        double avgProbes = size == 0 ? 0 : (double) totalProbes / size;

        int peakHour = -1;
        int max = 0;
        for (Map.Entry<Integer, Integer> entry : hourlyCount.entrySet()) {
            if (entry.getValue() > max) {
                max = entry.getValue();
                peakHour = entry.getKey();
            }
        }

        System.out.println("Occupancy: " + String.format("%.2f", occupancy) + "%");
        System.out.println("Avg Probes: " + String.format("%.2f", avgProbes));
        if (peakHour != -1)
            System.out.println("Peak Hour: " + peakHour + ":00 - " + (peakHour + 1) + ":00");
    }

    public static void main(String[] args) throws InterruptedException {

        ParkingLotManagement parking = new ParkingLotManagement(500);

        parking.parkVehicle("ABC-1234");
        parking.parkVehicle("ABC-1235");
        parking.parkVehicle("XYZ-9999");

        Thread.sleep(3000);

        parking.exitVehicle("ABC-1234");

        parking.getStatistics();

        int nearest = parking.findNearestAvailable();
        System.out.println("Nearest Available Spot: #" + nearest);
    }
}