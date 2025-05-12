package org.kuzne;

import java.util.Map;

public class InputData {
    public Map<Integer, Integer> jobs_orig;
    public Map<Integer, Integer[]> configs;
    public double[][] slowdown;
    public double[][] order;

    public InputData(Map<Integer, Integer> jobs_orig,
                     Map<Integer, Integer[]> configs,
                     double[][] slowdown,
                     double[][] order) {
        this.jobs_orig = jobs_orig;
        this.configs = configs;
        this.slowdown = slowdown;
        this.order = order;
    }
}

