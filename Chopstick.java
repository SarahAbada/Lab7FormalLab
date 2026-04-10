public class Chopstick {
    private int ID;
    // Tracks availability; volatile not needed here since all access is via
    // synchronized methods, but documenting intent: this is the shared mutable state.
    private boolean free = true;

    Chopstick(int ID) {
        this.ID = ID;
    }

    /**
     * Blocks the calling thread (philosopher) until this chopstick is available,
     * then atomically claims it. Uses wait/notify for cooperative mutual exclusion.
     *
     * Security/Correctness note: wait() is called inside a while loop (not if)
     * to guard against spurious wakeups — a classic concurrency bug if missed.
     */
    synchronized void take() {
        while (!free) {
            try {
                wait(); // release monitor lock and suspend until notified
            } catch (InterruptedException e) {
                // Restore interrupt status rather than swallowing it.
                // Allows the philosopher thread to respond to shutdown signals.
                Thread.currentThread().interrupt();
                return; // exit gracefully if interrupted
            }
        }
        free = false; // atomically claim the chopstick
    }

    /**
     * Releases the chopstick back to the table and wakes ONE waiting philosopher.
     * notifyAll() is safer here than notify() — with notify(), if multiple
     * philosophers are waiting, a non-eligible one could be woken and re-block,
     * potentially causing starvation in pathological scheduling cases.
     */
    synchronized void release() {
        free = true;
        notifyAll(); // wake all waiters; they re-check the while condition
    }

    public int getID() {
        return ID;
    }
}
