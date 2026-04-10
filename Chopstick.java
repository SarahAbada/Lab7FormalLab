public class Chopstick {
    private int ID;
    private boolean free = true;

    Chopstick(int ID) {
        this.ID = ID;
    }

    /**
     * Blocking take — used internally; same as Part A.
     */
    synchronized void take() {
        while (!free) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        free = false;
    }

    /**
     * Timed take — attempts to acquire the chopstick within the given timeout.
     *
     * Design note: we track ELAPSED time manually across wait() calls because
     * a single wait(timeout) is not sufficient — spurious wakeups can return
     * early, and another thread may grab the chopstick before us after a
     * legitimate notify. We must re-check `free` every time we wake up, and
     * recalculate remaining time to avoid extending the deadline on each loop.
     *
     * @param timeoutMs maximum milliseconds to wait
     * @return true if chopstick was acquired, false if timeout expired
     */
    synchronized boolean tryTake(long timeoutMs) {
        // Record the absolute deadline rather than counting down a mutable
        // variable — this is safer across spurious wakeups.
        final long deadline = System.currentTimeMillis() + timeoutMs;

        while (!free) {
            long remaining = deadline - System.currentTimeMillis();

            // Timeout has expired and chopstick is still not free — give up.
            if (remaining <= 0) {
                return false;
            }

            try {
                // Wait only for the remaining window, not a fresh full timeout.
                // This is the critical correctness point: wait(timeoutMs) alone
                // would reset the clock on every spurious wakeup.
                wait(remaining);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        // Chopstick is free — claim it atomically while still holding the monitor.
        free = false;
        return true;
    }

    /**
     * Releases the chopstick and wakes all waiters.
     */
    synchronized void release() {
        free = true;
        notifyAll();
    }

    public int getID() {
        return ID;
    }
}
