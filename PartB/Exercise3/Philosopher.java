public class Philosopher extends Thread {
    private GraphicTable table;
    private Chopstick left;
    private Chopstick right;
    private int ID;
    final int timeThink_max = 5000;
    final int timeNextFork = 100;
    final int timeEat_max = 5000;

    /**
     * Maximum time (ms) a philosopher will wait for a single chopstick.
     * If this deadline is missed, they release any held chopstick and
     * return to thinking — breaking the hold-and-wait condition that
     * contributes to deadlock.
     *
     * Tuning note: too small a value causes excessive contention/livelock
     * as philosophers constantly retry; too large approaches the blocking
     * behavior of Part A. A value meaningfully larger than timeNextFork
     * (100ms) but small enough to recover quickly is a reasonable balance.
     */
    private static final int WAIT_TIMEOUT_MS = 500;

    Philosopher(int ID, GraphicTable table, Chopstick left, Chopstick right) {
        this.ID = ID;
        this.table = table;
        this.left = left;
        this.right = right;
        setName("Philosopher " + ID);
    }

    public void run() {
        while (true) {

            // ---- THINKING ----
            table.isThinking(ID);
            System.out.println(getName() + " thinks");
            try {
                sleep((long) (Math.random() * timeThink_max));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            System.out.println(getName() + " finished thinking");

            // ---- HUNGRY ----
            System.out.println(getName() + " is hungry");
            table.isHungry(ID);

            // ---- ATTEMPT LEFT CHOPSTICK ----
            System.out.println(getName() + " wants left chopstick (will wait up to " + WAIT_TIMEOUT_MS + "ms)");

            if (!left.tryTake(WAIT_TIMEOUT_MS)) {
                // Timed out on left chopstick — holding nothing, just retry.
                System.out.println(getName() + " timed out waiting for left chopstick, retrying...");
                // Loop back to thinking state; a small yield avoids a tight
                // busy-retry loop hammering the scheduler.
                Thread.yield();
                continue;
            }

            // Got the left chopstick.
            table.takeChopstick(ID, left.getID());
            System.out.println(getName() + " got left chopstick");

            // Etiquette pause before reaching for the right chopstick.
            try {
                sleep(timeNextFork);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                table.releaseChopstick(ID, left.getID());
                left.release();
                return;
            }

            // ---- ATTEMPT RIGHT CHOPSTICK ----
            System.out.println(getName() + " wants right chopstick (will wait up to " + WAIT_TIMEOUT_MS + "ms)");

            if (!right.tryTake(WAIT_TIMEOUT_MS)) {
                // Timed out on right chopstick — MUST release the left one
                // we are already holding, otherwise we leak a resource and
                // the neighbor philosopher blocks forever.
                System.out.println(getName() + " timed out waiting for right chopstick, releasing left and retrying...");
                table.releaseChopstick(ID, left.getID());
                left.release();
                System.out.println(getName() + " released left chopstick due to timeout");
                Thread.yield();
                continue;
            }

            // Got both chopsticks.
            table.takeChopstick(ID, right.getID());
            System.out.println(getName() + " got right chopstick");

            // ---- EATING ----
            System.out.println(getName() + " eats");
            try {
                sleep((long) (Math.random() * timeEat_max));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                table.releaseChopstick(ID, left.getID());
                left.release();
                table.releaseChopstick(ID, right.getID());
                right.release();
                return;
            }
            System.out.println(getName() + " finished eating");

            // ---- RELEASE CHOPSTICKS ----
            table.releaseChopstick(ID, left.getID());
            left.release();
            System.out.println(getName() + " released left chopstick");
            table.releaseChopstick(ID, right.getID());
            right.release();
            System.out.println(getName() + " released right chopstick");
        }
    }
}
