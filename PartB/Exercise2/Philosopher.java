import java.util.concurrent.Semaphore;

public class Philosopher extends Thread {
    private GraphicTable table;
    private Chopstick left;
    private Chopstick right;
    private int ID;
    final int timeThink_max = 5000;
    final int timeNextFork = 100;
    final int timeEat_max = 5000;

    /**
     * Counting semaphore shared across ALL philosopher instances.
     * Permits = 4: at most 4 philosophers may attempt to pick up chopsticks
     * simultaneously, guaranteeing at least one chopstick pair is always
     * acquirable and deadlock cannot occur by circular wait exhaustion.
     *
     * Static so all Philosopher instances share the same semaphore instance
     * — if it were an instance field, each philosopher would have their own
     * private semaphore, which would be meaningless.
     *
     * Fair = true: FIFO ordering prevents starvation of any single philosopher
     * that keeps losing the acquire() race.
     */
    private static final Semaphore diningPermit = new Semaphore(4, true);

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

            // Acquire a dining permit before touching any chopstick.
            // At most 4 philosophers pass this gate concurrently, so at least
            // one chopstick pair is always free — deadlock is structurally impossible.
            try {
                diningPermit.acquire();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            // ---- ACQUIRE CHOPSTICKS (now safe: max 4 competing) ----
            System.out.println(getName() + " wants left chopstick");
            left.take();
            table.takeChopstick(ID, left.getID());
            System.out.println(getName() + " got left chopstick");

            try {
                sleep(timeNextFork);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Release resources already held before exiting
                left.release();
                diningPermit.release();
                return;
            }

            System.out.println(getName() + " wants right chopstick");
            right.take();
            table.takeChopstick(ID, right.getID());
            System.out.println(getName() + " got right chopstick");

            // ---- EATING ----
            System.out.println(getName() + " eats");
            try {
                sleep((long) (Math.random() * timeEat_max));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Release all resources before exiting
                table.releaseChopstick(ID, left.getID());
                left.release();
                table.releaseChopstick(ID, right.getID());
                right.release();
                diningPermit.release();
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

            // Release the dining permit AFTER putting down both chopsticks,
            // not before — releasing early would let a 5th philosopher enter
            // before chopsticks are actually free, undermining the guarantee.
            diningPermit.release();
        }
    }
}
