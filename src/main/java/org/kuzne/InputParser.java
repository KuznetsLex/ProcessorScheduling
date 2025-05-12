package org.kuzne;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.*;

public class InputParser {
    public static InputData readInputFile(String filename) throws IOException {
        Map<Integer, Integer> jobs_orig = new HashMap<>();
        Map<Integer, List<Integer>> configs_raw = new HashMap<>();
        Map<Integer, Map<Integer, Double>> slowdown_raw = new HashMap<>();
        List<int[]> orderPairs = new ArrayList<>();
        List<List<Integer>> initialSchedule = new ArrayList<>();

        int maxJobId = -1;
        int maxConfigId = -1;

        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;

        Pattern sPattern = Pattern.compile("s\\(\"(\\d+)\"\\)=(\\d+);");
        Pattern qPattern = Pattern.compile("q\\(\"(\\d+)\",\"(\\d+)\"\\)=1;");
        Pattern vPattern = Pattern.compile("v\\(\"(\\d+)\",\"(\\d+)\"\\)=([0-9.]+);");
        Pattern aPattern = Pattern.compile("a\\(\"(\\d+)\",\"(\\d+)\"\\)=1;");
        Pattern schedulePattern = Pattern.compile("\\[\\[(.*)]]$", Pattern.DOTALL);

        StringBuilder scheduleBuilder = new StringBuilder();
        boolean readingSchedule = false;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            Matcher sMatcher = sPattern.matcher(line);
            Matcher qMatcher = qPattern.matcher(line);
            Matcher vMatcher = vPattern.matcher(line);
            Matcher aMatcher = aPattern.matcher(line);

            if (sMatcher.matches()) {
                int jobId = Integer.parseInt(sMatcher.group(1));
                int value = Integer.parseInt(sMatcher.group(2));
                jobs_orig.put(jobId, value);
                maxJobId = Math.max(maxJobId, jobId);
            } else if (qMatcher.matches()) {
                int jobId = Integer.parseInt(qMatcher.group(1));
                int configId = Integer.parseInt(qMatcher.group(2));
                configs_raw.computeIfAbsent(configId, k -> new ArrayList<>()).add(jobId);
                maxConfigId = Math.max(maxConfigId, configId);
            } else if (vMatcher.matches()) {
                int jobId = Integer.parseInt(vMatcher.group(1));
                int configId = Integer.parseInt(vMatcher.group(2));
                double slowdown = Double.parseDouble(vMatcher.group(3));
                slowdown_raw.computeIfAbsent(jobId, k -> new HashMap<>()).put(configId, slowdown);
            } else if (aMatcher.matches()) {
                int after = Integer.parseInt(aMatcher.group(1));
                int before = Integer.parseInt(aMatcher.group(2));
                if (configs_raw.containsKey(after) && configs_raw.containsKey(before)) {
                    List<Integer> jobsAfter = configs_raw.get(after);
                    List<Integer> jobsBefore = configs_raw.get(before);
                    if (jobsAfter.size() == 1 && jobsBefore.size() == 1) {
                        orderPairs.add(new int[]{jobsAfter.get(0), jobsBefore.get(0)});
                    }
                }
            } else if (line.startsWith("[[")) {
                readingSchedule = true;
            }

            if (readingSchedule) {
                scheduleBuilder.append(line);
            }
        }
        reader.close();

        // Преобразование raw конфигураций
        Map<Integer, Integer[]> configs = new HashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : configs_raw.entrySet()) {
            configs.put(entry.getKey(), entry.getValue().toArray(new Integer[0]));
        }

        // Создание матрицы slowdown
        int jobCount = maxJobId + 1;
        double[][] slowdown = new double[jobCount][jobCount];
        for (int i = 0; i < jobCount; i++) {
            Arrays.fill(slowdown[i], 1.0);
        }
        for (int i = 0; i < jobCount; i++) {
            for (int j = 0; j < jobCount; j++) {
                for (Map.Entry<Integer, Map<Integer, Double>> entry : slowdown_raw.entrySet()) {
                    int job = entry.getKey();
                    for (Map.Entry<Integer, Double> inner : entry.getValue().entrySet()) {
                        Integer config = inner.getKey();
                        if (configs.containsKey(config)) {
                            Integer[] jobsInConfig = configs.get(config);
                            if (Arrays.asList(jobsInConfig).contains(i) && Arrays.asList(jobsInConfig).contains(j)) {
                                slowdown[i][j] = inner.getValue();
                            }
                        }
                    }
                }
            }
        }

        // Матрица порядка
        double[][] order = new double[jobCount][jobCount];
        for (int[] pair : orderPairs) {
            order[pair[0]][pair[1]] = 1.0;
        }

        // Чтение расписания
        if (scheduleBuilder.length() > 0) {
            String scheduleRaw = scheduleBuilder.toString();
            scheduleRaw = scheduleRaw.replaceAll("\\[\\[|]]", ""); // удалить внешние скобки
            String[] coreLines = scheduleRaw.split("],\\s*\\[");
            for (String coreLine : coreLines) {
                List<Integer> coreJobs = new ArrayList<>();
                for (String token : coreLine.split(",")) {
                    coreJobs.add(Integer.parseInt(token.trim()));
                }
                initialSchedule.add(coreJobs);
            }
        }

        return new InputData(jobs_orig, configs, slowdown, order, initialSchedule);
    }
}






