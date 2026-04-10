public class Philosopher extends Thread {
    private GraphicTable table;
    private Chopstick left;
    private Chopstick right;
    private int ID;
    final int timeThink_max = 5000;
    final int timeNextFork = 100;
    final int timeEat_max = 5000;

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

            // ---- DETERMINE ACQUISITION ORDER ----
            // Odd philosophers: left first, then right.
            // Even philosophers: right first, then left.
            //
            // Why this breaks deadlock: the circular wait condition requires
            // every philosopher to be waiting on their neighbor in the SAME
            // rotational direction. By flipping the acquisition order for even
            // IDs, at least one adjacent pair will be competing for the same
            // chopstick as their FIRST pick rather than their second — meaning
            // one of them acquires it outright and proceeds, while the other
            // waits. The cycle cannot close across all 5 philosophers.
            //
            // Concretely: philosopher 0 (even) grabs right first, philosopher
            // 1 (odd) grabs left first. Their shared chopstick (chopstick 0)
            // is the FIRST target of both — one wins immediately, breaking
            // the hold-and-wait chain before it can form a full circle.
            Chopstick first  = (ID % 2 != 0) ? left  : right;
            Chopstick second = (ID % 2 != 0) ? right : left;

            String firstName  = (ID % 2 != 0) ? "left"  : "right";
            String secondName = (ID % 2 != 0) ? "right" : "left";

            // ---- ACQUIRE FIRST CHOPSTICK ----
            System.out.println(getName() + " wants " + firstName + " chopstick first");
            first.take();
            table.takeChopstick(ID, first.getID());
            System.out.println(getName() + " got " + firstName + " chopstick");

            // Etiquette pause before reaching for the second chopstick.
            try {
                sleep(timeNextFork);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                // Release held resource before exiting — never leak a chopstick.
                table.releaseChopstick(ID, first.getID());
                first.release();
                return;
            }

            // ---- ACQUIRE SECOND CHOPSTICK ----
            System.out.println(getName() + " wants " + secondName + " chopstick second");
            second.take();
            table.takeChopstick(ID, second.getID());
            System.out.println(getName() + " got " + secondName + " chopstick");

            // ---- EATING ----
            System.out.println(getName() + " eats");
            try {
                sleep((long) (Math.random() * timeEat_max));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                table.releaseChopstick(ID, first.getID());
                first.release();
                table.releaseChopstick(ID, second.getID());
                second.release();
                return;
            }
            System.out.println(getName() + " finished eating");

            // ---- RELEASE CHOPSTICKS ----
            // Release order does not affect correctness here, but releasing
            // in reverse acquisition order is a good general habit — it mirrors
            // lock ordering conventions and makes auditing easier.
            table.releaseChopstick(ID, second.getID());
            second.release();
            System.out.println(getName() + " released " + secondName + " chopstick");
            table.releaseChopstick(ID, first.getID());
            first.release();
            System.out.println(getName() + " released " + firstName + " chopstick");
        }
    }
}
