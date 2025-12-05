package scheduler2022.server;

import java.io.Serializable;

public class SchedulerInstruction implements Serializable {

    public String SchedulerStrategy;
    public int Interval;

    public SchedulerInstruction(String newSchedulerStrategy, int newInterval) {
        this.SchedulerStrategy = newSchedulerStrategy;
        this.Interval = newInterval;
    }

    @Override
    public String toString() {
        return "SchedulerInstruction{" +
               "SchedulerStrategy='" + SchedulerStrategy + '\'' +
               ", interval=" + Interval +
               '}';
    }
}
