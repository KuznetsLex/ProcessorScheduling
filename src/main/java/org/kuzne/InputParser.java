package org.kuzne;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public class InputParser {
    public static InputData readInputFile(String filename) throws IOException {
        Map<Integer, Integer> jobs_orig = new HashMap<>();
        Map<Integer, List<Integer>> configs_raw = new HashMap<>();
        Map<Integer, Map<Integer, Double>> slowdown_raw = new HashMap<>();
        List<int[]> orderPairs = new ArrayList<>();

        int maxJobId = -1;
        int maxConfigId = -1;

        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;

        Pattern sPattern = Pattern.compile("s\\(\"(\\d+)\"\\)=(\\d+);");
        Pattern qPattern = Pattern.compile("q\\(\"(\\d+)\",\"(\\d+)\"\\)=1;");
        Pattern vPattern = Pattern.compile("v\\(\"(\\d+)\",\"(\\d+)\"\\)=([0-9.]+);");
        Pattern aPattern = Pattern.compile("a\\(\"(\\d+)\",\"(\\d+)\"\\)=1;");

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            Matcher sm = sPattern.matcher(line);
            Matcher qm = qPattern.matcher(line);
            Matcher vm = vPattern.matcher(line);
            Matcher am = aPattern.matcher(line);

            if (sm.matches()) {
                int jobId = Integer.parseInt(sm.group(1));
                int value = Integer.parseInt(sm.group(2));
                jobs_orig.put(jobId, value);
                maxJobId = Math.max(maxJobId, jobId);

            } else if (qm.matches()) {
                int jobId = Integer.parseInt(qm.group(1));
                int configId = Integer.parseInt(qm.group(2));
                configs_raw.computeIfAbsent(configId, k -> new ArrayList<>()).add(jobId);
                maxConfigId = Math.max(maxConfigId, configId);

            } else if (vm.matches()) {
                int jobId = Integer.parseInt(vm.group(1));
                int configId = Integer.parseInt(vm.group(2));
                double slowdown = Double.parseDouble(vm.group(3));
                slowdown_raw.computeIfAbsent(jobId, k -> new HashMap<>()).put(configId, slowdown);

            } else if (am.matches()) {
                int after = Integer.parseInt(am.group(1));
                int before = Integer.parseInt(am.group(2));

                if (configs_raw.containsKey(after) && configs_raw.containsKey(before)) {
                    List<Integer> jobsAfter = configs_raw.get(after);
                    List<Integer> jobsBefore = configs_raw.get(before);
                    if (jobsAfter.size() == 1 && jobsBefore.size() == 1) {
                        orderPairs.add(new int[]{jobsAfter.get(0), jobsBefore.get(0)});
                    }
                }
            }
        }
        reader.close();

        // Преобразуем configs_raw в Map<Integer, Integer[]>
        Map<Integer, Integer[]> configs = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : configs_raw.entrySet()) {
            configs.put(entry.getKey(), entry.getValue().toArray(new Integer[0]));
        }

        int jobCount = maxJobId + 1;
        double[][] slowdown = new double[jobCount][jobCount];
        for (int i = 0; i < jobCount; i++) Arrays.fill(slowdown[i], 1.0);

        for (int i = 0; i < jobCount; i++) {
            for (int j = 0; j < jobCount; j++) {
                for (Map.Entry<Integer, Map<Integer, Double>> entry : slowdown_raw.entrySet()) {
                    int job = entry.getKey();
                    for (Map.Entry<Integer, Double> inner : entry.getValue().entrySet()) {
                        Integer config = inner.getKey();
                        if (configs.containsKey(config)) {
                            List<Integer> jobsInConfig = Arrays.asList(configs.get(config));
                            if (jobsInConfig.contains(i) && jobsInConfig.contains(j)) {
                                slowdown[i][j] = inner.getValue();
                            }
                        }
                    }
                }
            }
        }

        double[][] order = new double[jobCount][jobCount];
        for (int[] pair : orderPairs) {
            order[pair[0]][pair[1]] = 1.0;
        }

        return new InputData(jobs_orig, configs, slowdown, order);
    }
}






