package org.kuzne;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.*;

public class LocalSearch {
    private static final int CORE_NUMBER = 2;

    /*
     * ТИПЫ ДАННЫХ
     * 
     * 1. List<Integer> D. Двумерный массив nXm, где n - число точек событий, m - число конфигураци. 
     * Представлен в виде одномерного динамического массива.
     * 
     * 2. double[][] order. Двумерный массив nX2, где n - число ограничений. 
     * Для любого j для выполнения работы order[0][j] требуется выполнение работы order[1][j].
     * 
     * 3. Map<Integer, Integer[]> configs. 
     * Отображение номера конфигурации в номера работ, содержащихся в данной конфигурации.
     * 
     * 4. double jobs[][]. Массив n массивов, где n - число ядер. 
     * Каждый массив содержит номера работ в порядке их выполнения на процессоре.
     * Число -1 в массиве означает пустую работу, которая заканчивается в ближайшей точке событий.
     *
     * 5.double jobsSlowdown[][]. Массив n массивов, где n - число ядер.
     * Значение jobsSlowdown[i][j] - насколько замедляется i-тая работа при параллельной работе с j.
     */
    public static void main(String[] args) throws IOException {
        Map<Integer, Integer> jobs_orig = new HashMap<>();
        jobs_orig.put(0, 9951);
        jobs_orig.put(1, 13206);
        jobs_orig.put(2, 3831);
        jobs_orig.put(3, 4209);

        double[][] jobsSlowdown = { {1, 0.97, 0, 0.87}, {0.98, 1, 0.96, 0}, {0, 0.93, 1, 0.6}, {0.9, 0, 0.75, 1}};

        Map<Integer, Integer[]> configs = new HashMap<>();
        configs.put(1, new Integer[]{0});
        configs.put(2, new Integer[]{1});
        configs.put(3, new Integer[]{2});
        configs.put(4, new Integer[]{3});
        configs.put(5, new Integer[]{0, 1});
        configs.put(6, new Integer[]{0, 3});
        configs.put(7, new Integer[]{1, 2});
        configs.put(8, new Integer[]{2, 3});

        double[][] order = new double[][] {{2, 0}, {3, 1}};

        List<Path> paths = Files.walk(Paths.get("input")).collect(Collectors.toList());
        paths.removeFirst();

        List<ProblemInstance> problemInstances = paths.stream()
                .map(InstanceReader::readInstance)
                .toList();

        for (ProblemInstance problemInstance : problemInstances) {
            List<List<Integer>> result = GreedyAlgorithm.buildSchedule(problemInstance);
            List<List<Integer>> jobs = scheduleToJobs(result);
            List<Integer> D = jobsOnCoresToD(jobs_orig, jobs, jobsSlowdown, configs);
            List<Integer> locSearch = localSearch(jobs_orig, D, order, configs);
            System.out.println(locSearch);
        }
    }
    public static List<Integer> localSearch(Map<Integer, Integer> jobs_orig, List<Integer> D, double[][] order, Map<Integer, Integer[]> configs) {
        List<Integer> curOpt = new ArrayList<>(D);
        double curOptValue = evaluateSchedule(curOpt, jobs_orig, order, configs);
        List<Integer> bestNeighbor = null;
        double bestNeighborValue;

        boolean improved;
        do {
            improved = false;
            List<List<Integer>> neighbors = generateNeighbors(curOpt, configs, order);

            for (List<Integer> neighbor : neighbors) {
                bestNeighborValue = evaluateSchedule(neighbor, jobs_orig, order, configs);

                if (bestNeighborValue < curOptValue) {
                    curOpt = new ArrayList<>(neighbor);
                    curOptValue = bestNeighborValue;
                    improved = true;
                    break; // Переход к следующей итерации
                }
            }
        } while (improved);

        return curOpt;
    }

    private static List<List<Integer>> generateNeighbors(List<Integer> D, Map<Integer, Integer[]> configs, double[][] order) {
        List<List<Integer>> neighbors = new ArrayList<>();

        for (int i = 0; i < D.size(); i++) {
            int currentConfig = D.get(i);

            for (int newConfig : configs.keySet()) {
                if (newConfig != currentConfig) {
                    List<Integer> newD = new ArrayList<>(D);
                    newD.set(i, newConfig);

                    if (isValidSchedule(newD, order, configs)) {
                        neighbors.add(newD);
                    }
                }
            }
        }
        return neighbors;
    }

    private static boolean isValidSchedule(List<Integer> D, double[][] order, Map<Integer, Integer[]> configs) {
        // Построим отображение <работа, время ее начала в D>
        Map<Integer, Integer> jobStartTimes = new HashMap<>();

        for (int time = 0; time < D.size(); time++) {
            Integer[] jobsInConfig = configs.get(D.get(time));
            if (jobsInConfig != null) {
                for (int job : jobsInConfig) {
                    jobStartTimes.putIfAbsent(job, time);
                }
            }
        }

        // Проверим, что порядок выполнения соблюдается
        for (double[] dependency : order) {
            int jobA = (int) dependency[0];
            int jobB = (int) dependency[1];

            Integer startA = jobStartTimes.get(jobA);
            Integer startB = jobStartTimes.get(jobB);

            if (startA == null || startB == null || startA >= startB) {
                return false; // Нарушение частичного порядка
            }
        }

        return true;
    }

    private static double evaluateSchedule(List<Integer> D, Map<Integer, Integer> jobs_orig, double[][] order, Map<Integer, Integer[]> configs) {
        // Здесь должен быть вызов GAMS и обработка его результата.
        // Например, можно записать D в файл, запустить GAMS, считать результат.
        // Пока заглушка:
        double eval = Math.random() * 1000; // Имитация оценки расписания
        System.out.println(eval);
        return eval;

    }

    /*scheduleToJobs для 2-ух ядер*/
    public static List<List<Integer>> scheduleToJobs(List<List<Integer>> schedule) {
        List<List<Integer>> jobsOnCores = new ArrayList<>();
        jobsOnCores.add(new ArrayList<>());
        jobsOnCores.add(new ArrayList<>());
        for(List<Integer> eventpoint: schedule) {
            if (eventpoint.size() == 1) {
                if (jobsOnCores.get(0).contains(eventpoint.get(0))) {
                    jobsOnCores.get(1).add(-1);
                }
                else if(jobsOnCores.get(1).contains(eventpoint.get(0))) {
                    jobsOnCores.get(0).add(-1);
                }
                else {
                    jobsOnCores.get(0).add(eventpoint.get(0));
                    jobsOnCores.get(1).add(-1);
                }
            }
            else {
                if (jobsOnCores.get(0).contains(eventpoint.get(0))) {
                    jobsOnCores.get(1).add(eventpoint.get(1));
                }
                else if (jobsOnCores.get(1).contains(eventpoint.get(0))) {
                    jobsOnCores.get(0).add(eventpoint.get(1));
                }
                else if (jobsOnCores.get(0).contains(eventpoint.get(1))) {
                    jobsOnCores.get(1).add(eventpoint.get(0));
                }
                else if (jobsOnCores.get(1).contains(eventpoint.get(1))) {
                    jobsOnCores.get(0).add(eventpoint.get(0));
                }
                else {
                    jobsOnCores.get(0).add(eventpoint.get(0));
                    jobsOnCores.get(1).add(eventpoint.get(1));
                }
            }
        }
        return jobsOnCores;
    }

    public static List<List<Integer>> dToJobs(Map<Integer, Integer> jobs_orig, List<Integer> D, Map<Integer, Integer[]> configs) {
        List<List<Integer>> jobsOnCores = new ArrayList<>();
        jobsOnCores.add(new ArrayList<>());
        jobsOnCores.get(0).add(-2);
        jobsOnCores.add(new ArrayList<>());
        jobsOnCores.get(1).add(-2);
        Integer[] jobsFromConfig;
        int j = 0;
        for (int i = 0; i < D.size(); i++) {
            if (D.get(i) != 0) {
                jobsFromConfig = configs.get(D.get(i));
                if (jobsFromConfig.length == 2) {
                    if (jobsOnCores.get(CORE_NUMBER-2).get(jobsOnCores.get(CORE_NUMBER-2).size()-1) != jobsFromConfig[0] &&
                        jobsOnCores.get(CORE_NUMBER-2).get(jobsOnCores.get(CORE_NUMBER-2).size()-1) != jobsFromConfig[1] &&
                        jobsOnCores.get(CORE_NUMBER-1).get(jobsOnCores.get(CORE_NUMBER-1).size()-1) != jobsFromConfig[0] &&
                        jobsOnCores.get(CORE_NUMBER-1).get(jobsOnCores.get(CORE_NUMBER-1).size()-1) != jobsFromConfig[1]) {
                            jobsOnCores.get(CORE_NUMBER-2).add(jobsFromConfig[0]);
                            jobsOnCores.get(CORE_NUMBER-1).add(jobsFromConfig[1]);
                    }
                    else {
                        if      (jobsOnCores.get(CORE_NUMBER-2).get(jobsOnCores.get(CORE_NUMBER-2).size()-1) == jobsFromConfig[0]) { jobsOnCores.get(CORE_NUMBER-1).add(jobsFromConfig[1]); }
                        else if (jobsOnCores.get(CORE_NUMBER-2).get(jobsOnCores.get(CORE_NUMBER-2).size()-1) == jobsFromConfig[1]) { jobsOnCores.get(CORE_NUMBER-1).add(jobsFromConfig[0]); }
                        else if (jobsOnCores.get(CORE_NUMBER-1).get(jobsOnCores.get(CORE_NUMBER-1).size()-1) == jobsFromConfig[0]) { jobsOnCores.get(CORE_NUMBER-2).add(jobsFromConfig[1]); }
                        else if (jobsOnCores.get(CORE_NUMBER-1).get(jobsOnCores.get(CORE_NUMBER-1).size()-1) == jobsFromConfig[1]) { jobsOnCores.get(CORE_NUMBER-2).add(jobsFromConfig[0]); }
                    }
                }
                if (jobsFromConfig.length == 1) {
                    if (jobsOnCores.get(CORE_NUMBER-2).get(jobsOnCores.get(CORE_NUMBER-2).size()-1) != jobsFromConfig[0] &&
                        jobsOnCores.get(CORE_NUMBER-1).get(jobsOnCores.get(CORE_NUMBER-1).size()-1) != jobsFromConfig[0]) {
                            jobsOnCores.get(CORE_NUMBER-2).add(jobsFromConfig[0]);
                            jobsOnCores.get(CORE_NUMBER-1).add(-1);
                    }
                    else {
                        if      (jobsOnCores.get(CORE_NUMBER-2).get(jobsOnCores.get(CORE_NUMBER-2).size()-1) == jobsFromConfig[0]) { jobsOnCores.get(CORE_NUMBER-1).add(-1); }
                        else if (jobsOnCores.get(CORE_NUMBER-1).get(jobsOnCores.get(CORE_NUMBER-1).size()-1) == jobsFromConfig[0]) { jobsOnCores.get(CORE_NUMBER-2).add(-1); }
                    }
                } 
                if (jobsFromConfig.length != 1 && jobsFromConfig.length != 2) {
                    throw new ArrayIndexOutOfBoundsException();
                }
                j+=1;
            }
        }
        List<List<Integer>> jobsOnCoresTrue = new ArrayList<>();
        for(int i = 0; i < jobsOnCores.size(); i++) {
            jobsOnCoresTrue.add(new ArrayList<>());
            for (int k = 1; k < jobsOnCores.get(i).size(); k++) {
                jobsOnCoresTrue.get(i).add(jobsOnCores.get(i).get(k));
            }
        }
        return jobsOnCoresTrue;
    }

    public static List<Integer> jobsOnCoresToD(
            Map<Integer, Integer> jobs_orig,
            List<List<Integer>> jobsOnCores,
            double[][] jobsSlowdown,
            Map<Integer, Integer[]> configs
    ) {
        List<Integer> D = new ArrayList<>();

        int[] taskIndices = new int[]{0, 0};
        double[] remainingTimes = new double[2];
        Integer[] currentTasks = new Integer[]{
                jobsOnCores.get(0).get(0),
                jobsOnCores.get(1).get(0)
        };

        // Начальные значения оставшегося времени
        for (int core = 0; core < 2; core++) {
            int task = currentTasks[core];
            if (task != -1) {
                int other = currentTasks[1 - core];
                double slowdown = (other != -1) ? jobsSlowdown[task][other] : 1.0;
                remainingTimes[core] = jobs_orig.get(task) / slowdown;
            } else {
                remainingTimes[core] = 0;
            }
        }

        while (taskIndices[0] < jobsOnCores.get(0).size() || taskIndices[1] < jobsOnCores.get(1).size()) {
            Integer[] config = new Integer[2];
            config[0] = currentTasks[0];
            config[1] = currentTasks[1];

            double minTime = Double.MAX_VALUE;
            for (double rt : remainingTimes) {
                if (rt > 0 && rt < minTime) {
                    minTime = rt;
                }
            }

            int configId = findMatchingConfig(config, configs);
            D.add(configId);

            for (int i = 0; i < 2; i++) {
                if (remainingTimes[i] > 0) {
                    remainingTimes[i] -= minTime;
                }
            }

            for (int core = 0; core < 2; core++) {
                if (remainingTimes[core] <= 1e-6) {
                    taskIndices[core]++;
                    if (taskIndices[core] < jobsOnCores.get(core).size()) {
                        currentTasks[core] = jobsOnCores.get(core).get(taskIndices[core]);
                    } else {
                        currentTasks[core] = -1;
                    }

                    int task = currentTasks[core];
                    if (task != -1) {
                        int other = currentTasks[1 - core];
                        double slowdown = (other != -1) ? jobsSlowdown[task][other] : 1.0;
                        remainingTimes[core] = jobs_orig.get(task) / slowdown;
                    } else {
                        remainingTimes[core] = 0;
                    }

                    int other = 1 - core;
                    int otherTask = currentTasks[other];
                    if (otherTask != -1 && remainingTimes[other] > 0) {
                        double slowdown = (currentTasks[core] != -1) ? jobsSlowdown[otherTask][currentTasks[core]] : 1.0;
                        remainingTimes[other] = jobs_orig.get(otherTask) / slowdown;
                    }
                }
            }
        }

        return D;
    }

    private static int findMatchingConfig(Integer[] config, Map<Integer, Integer[]> configs) {
        List<Integer> configList =  new ArrayList<>();
        for (int task : config) {
            if (task != -1) configList.add(task);
        }

        for (Map.Entry<Integer, Integer[]> entry : configs.entrySet()) {
            List<Integer> jobs = Arrays.asList(entry.getValue());
            if (jobs.size() == configList.size() && jobs.containsAll(configList)) {
                return entry.getKey();
            }
        }
        return 0; // Пустая конфигурация
    }
}
