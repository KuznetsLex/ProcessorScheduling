package org.kuzne;
import java.util.List;
import java.util.Map;

public class InputData {
    public final Map<Integer, Integer> jobs_orig;
    public final Map<Integer, Integer[]> configs;
    public final double[][] slowdown;
    public final double[][] order;
    public final List<List<Integer>> initialSchedule;

    public InputData(
            Map<Integer, Integer> jobs_orig,
            Map<Integer, Integer[]> configs,
            double[][] slowdown,
            double[][] order,
            List<List<Integer>> initialSchedule
    ) {
        this.jobs_orig = jobs_orig;
        this.configs = configs;
        this.slowdown = slowdown;
        this.order = order;
        this.initialSchedule = initialSchedule;
    }

    public List<List<Integer>> getInitialSchedule() {
        return initialSchedule;
    }
}

