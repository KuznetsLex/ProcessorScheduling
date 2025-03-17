package org.kuzne;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
     */

    public static List<Integer> localSearch(Map<Integer, Integer> jobs_orig, List<Integer> D, double[][] order, Map<Integer, Integer[]> configs) {
        List<Integer> newD = List.copyOf(D);

        return newD;
    }

    public static List<List<Integer>> dToJobs(Map<Integer, Integer> jobs_orig, List<Integer> D, Map<Integer, Integer[]> configs) {
        List<List<Integer>> jobsOnCores = new ArrayList<>();
        jobsOnCores.add(new ArrayList<>());
        jobsOnCores.get(0).add(0);
        jobsOnCores.add(new ArrayList<>());
        jobsOnCores.get(1).add(0);
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
        return jobsOnCores;
    }

    public static List<Integer> jobsOnCoresToD(Map<Integer, Integer> jobs_orig, List<List<Integer>> jobsOnCores, Map<Integer, Integer[]> configs) {
        List<Integer> D = new ArrayList<>();
        int core1Index = 0, core2Index = 0;
    
        // Iterate through the jobsOnCores lists
        while (core1Index < jobsOnCores.get(0).size() || core2Index < jobsOnCores.get(1).size()) {
            int core1Job = (core1Index < jobsOnCores.get(0).size()) ? jobsOnCores.get(0).get(core1Index) : -1;
            int core2Job = (core2Index < jobsOnCores.get(1).size()) ? jobsOnCores.get(1).get(core2Index) : -1;
    
            // Find the configuration that matches the current jobs on cores
            boolean configFound = false;
            for (Map.Entry<Integer, Integer[]> entry : configs.entrySet()) {
                Integer[] configJobs = entry.getValue();
                if (configJobs.length == 2) {
                    if ((configJobs[0] == core1Job && configJobs[1] == core2Job) ||
                        (configJobs[0] == core2Job && configJobs[1] == core1Job)) {
                        D.add(entry.getKey());
                        configFound = true;
                        break;
                    }
                } else if (configJobs.length == 1) {
                    if ((configJobs[0] == core1Job && core2Job == -1) ||
                        (configJobs[0] == core2Job && core1Job == -1)) {
                        D.add(entry.getKey());
                        configFound = true;
                        break;
                    }
                }
            }
    
            // If no matching configuration is found, add 0 to D (indicating no configuration)
            if (!configFound) {
                D.add(0);
            }
    
            // Move to the next jobs on cores
            if (core1Index < jobsOnCores.get(0).size()) core1Index++;
            if (core2Index < jobsOnCores.get(1).size()) core2Index++;
        }
    
        return D;
    }
}
