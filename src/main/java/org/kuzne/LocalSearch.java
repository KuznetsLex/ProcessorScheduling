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
     * 4. List<List<Integer> jobs. Массив n массивов, где n - число ядер.
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
//            List<List<Integer>> jobs = scheduleToJobs(result);
//            List<Integer> D = jobsOnCoresToD(jobs_orig, jobs, jobsSlowdown, configs);
            List<Integer> D = scheduleToD(result, configs);
            List<Integer> locSearch = localSearch(jobs_orig, D, order, configs, jobsSlowdown);
            System.out.println(locSearch);
        }
    }
    public static List<Integer> localSearch(Map<Integer, Integer> jobs_orig, List<Integer> D, double[][] order, Map<Integer, Integer[]> configs, double[][] jobsSlowdown) {
        List<Integer> curOpt = new ArrayList<>(D);
        double curOptValue = evaluateSchedule(curOpt, jobs_orig, order, configs);
        List<Integer> bestNeighbor = null;
        double bestNeighborValue;

        boolean improved;
        do {
            improved = false;
            List<List<Integer>> neighbors = generateNeighbors(curOpt, configs, order, jobs_orig, jobsSlowdown);

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

    private static List<List<Integer>> generateNeighbors(
            List<Integer> D,
            Map<Integer, Integer[]> configs,
            double[][] order,
            Map<Integer, Integer> jobs_orig,
            double[][] jobsSlowdown) {

        List<List<Integer>> neighbors = new ArrayList<>();

        // Преобразуем расписание D в jobsOnCores
        List<List<Integer>> jobsOnCores = dToJobs(jobs_orig, D, configs);

        int numCores = jobsOnCores.size();

        for (int fromCore = 0; fromCore < numCores; fromCore++) {
            for (int fromIdx = 0; fromIdx < jobsOnCores.get(fromCore).size(); fromIdx++) {
                int jobA = jobsOnCores.get(fromCore).get(fromIdx);
                if (jobA == -1) continue;

                // Попробовать переместить работу на другое ядро или место
                for (int toCore = 0; toCore < numCores; toCore++) {
                    for (int toIdx = 0; toIdx <= jobsOnCores.get(toCore).size(); toIdx++) {

                        // Пропустить если та же позиция
                        if (fromCore == toCore && fromIdx == toIdx) continue;

                        // Клонируем jobsOnCores
                        List<List<Integer>> newJobs = deepCopy(jobsOnCores);

                        // Удалим jobA из исходной позиции
                        newJobs.get(fromCore).remove(fromIdx);

                        // Вставим jobA в новую позицию
                        if (toIdx > newJobs.get(toCore).size()) continue; // защита от выхода за границы
                        newJobs.get(toCore).add(toIdx, jobA);

                        // Преобразуем обратно в D
                        List<Integer> newD = jobsOnCoresToD(jobs_orig, newJobs, jobsSlowdown, configs);

                        // Проверка корректности порядка
                        if (isValidSchedule(newD, order, configs)) {
                            neighbors.add(newD);
                        }
                    }
                }

                // Попробовать обмен местами с другой работой
                for (int toCore = fromCore; toCore < numCores; toCore++) {
                    for (int toIdx = (toCore == fromCore ? fromIdx + 1 : 0); toIdx < jobsOnCores.get(toCore).size(); toIdx++) {
                        int jobB = jobsOnCores.get(toCore).get(toIdx);
                        if (jobB == -1) continue;

                        // Копируем расписание
                        List<List<Integer>> swappedJobs = deepCopy(jobsOnCores);

                        // Меняем местами
                        swappedJobs.get(fromCore).set(fromIdx, jobB);
                        swappedJobs.get(toCore).set(toIdx, jobA);

                        // Преобразуем обратно
                        List<Integer> newD = jobsOnCoresToD(jobs_orig, swappedJobs, jobsSlowdown, configs);

                        if (isValidSchedule(newD, order, configs)) {
                            neighbors.add(newD);
                        }
                    }
                }
            }
        }

        return neighbors;
    }

    private static List<List<Integer>> deepCopy(List<List<Integer>> original) {
        List<List<Integer>> copy = new ArrayList<>();
        for (List<Integer> list : original) {
            copy.add(new ArrayList<>(list));
        }
        return copy;
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

    public static List<Integer> scheduleToD(List<List<Integer>> schedule, Map<Integer, Integer[]> configs) {
        List<Integer> D = new ArrayList<>();
        for (int eventpoint = 0; eventpoint < schedule.size(); eventpoint++) {
            Integer[] ep = new Integer[schedule.get(eventpoint).size()];
            for (int i =0; i < ep.length; i++) {
                ep[i] = schedule.get(eventpoint).get(i);
            }
            Arrays.sort(ep);
            for(Map.Entry<Integer, Integer[]> entry: configs.entrySet()) {
               if(Arrays.equals(ep, entry.getValue())) {
                   D.add(entry.getKey());
               }
            }
        }
        return D;
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

        int numCores = jobsOnCores.size();
        int[] taskIndices = new int[numCores];
        Integer[] currentTasks = new Integer[numCores];
        double[] remainingTimes = new double[numCores];

        // Инициализация текущих задач и времени
        for (int core = 0; core < numCores; core++) {
            if (!jobsOnCores.get(core).isEmpty()) {
                currentTasks[core] = jobsOnCores.get(core).get(0);
            } else {
                currentTasks[core] = -1;
            }
        }

        while (true) {
            boolean allDone = true;
            for (int i = 0; i < numCores; i++) {
                if (currentTasks[i] != -1) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) break;

            // Обновляем оставшиеся времена, если они равны 0
            for (int i = 0; i < numCores; i++) {
                if (remainingTimes[i] <= 1e-9 && currentTasks[i] != -1) {
                    int task = currentTasks[i];
                    int other = currentTasks[1 - i];
                    double slowdown = (other != -1) ? jobsSlowdown[task][other] : 1.0;
                    if (slowdown == 0.0) slowdown = 1e-6;
                    remainingTimes[i] = jobs_orig.get(task) / slowdown;
                }
            }

            // Запоминаем текущую конфигурацию
            Integer[] config = new Integer[numCores];
            System.arraycopy(currentTasks, 0, config, 0, numCores);
            D.add(findMatchingConfig(config, configs));

            // Найти минимальное положительное время
            double minTime = Double.MAX_VALUE;
            for (double t : remainingTimes) {
                if (t > 1e-9 && t < minTime) {
                    minTime = t;
                }
            }

            if (minTime == Double.MAX_VALUE) break; // ничего не выполняется, избегаем зацикливания

            // Сдвигаем время
            for (int i = 0; i < numCores; i++) {
                if (remainingTimes[i] > 0) {
                    remainingTimes[i] -= minTime;
                }
            }

            // Обновляем завершённые задачи
            for (int i = 0; i < numCores; i++) {
                if (currentTasks[i] != -1 && remainingTimes[i] <= 1e-6) {
                    taskIndices[i]++;
                    if (taskIndices[i] < jobsOnCores.get(i).size()) {
                        currentTasks[i] = jobsOnCores.get(i).get(taskIndices[i]);
                        remainingTimes[i] = 0; // на следующей итерации пересчитаем
                    } else {
                        currentTasks[i] = -1;
                        remainingTimes[i] = 0;
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

    private static double calculateMakespan(
            Map<Integer, Integer> jobs_orig,
            List<List<Integer>> jobsOnCores,
            double[][] jobsSlowdown) {

        // Время завершения каждой работы
        Map<Integer, Double> jobFinishTime = new HashMap<>();

        // Текущее время на каждом ядре
        double[] coreTime = new double[jobsOnCores.size()];

        for (int core = 0; core < jobsOnCores.size(); core++) {
            List<Integer> jobs = jobsOnCores.get(core);

            for (int idx = 0; idx < jobs.size(); idx++) {
                int job = jobs.get(idx);
                if (job == -1) continue;

                // Найти работу, параллельно выполняющуюся с текущей
                int interferingJob = findParallelJob(jobsOnCores, core, idx);

                // Вычислить замедление
                double slowdown = 1.0;
                if (interferingJob != -1) {
                    slowdown = jobsSlowdown[job][interferingJob];
                    if (slowdown == 0.0) slowdown = 1e-6; // защита от деления на 0
                }

                double duration = jobs_orig.get(job) / slowdown;

                // Начало работы: после завершения предыдущей на ядре
                double start = coreTime[core];
                double finish = start + duration;

                jobFinishTime.put(job, finish);
                coreTime[core] = finish;
            }
        }

        // Время завершения расписания = макс из всех окончаний
        return jobFinishTime.values().stream().max(Double::compareTo).orElse(0.0);
    }

    private static int findParallelJob(List<List<Integer>> jobsOnCores, int core, int idx) {
        for (int otherCore = 0; otherCore < jobsOnCores.size(); otherCore++) {
            if (otherCore == core) continue;

            List<Integer> jobs = jobsOnCores.get(otherCore);
            if (idx < jobs.size()) {
                int otherJob = jobs.get(idx);
                if (otherJob != -1) return otherJob;
            }
        }
        return -1;
    }

}
