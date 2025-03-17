package org.kuzne;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Main {
    public static void main(String[] args) {
        // D = {0,5,7,8,4}
        // int[][] D = new int[][]{{1, 0, 0, 0, 0, 0, 0, 0, 0},{0, 0, 0, 0, 0, 1, 0, 0, 0},{0, 0, 0, 0, 0, 0, 0, 1, 0},{0, 0, 0, 0, 0, 0, 0, 0, 1},{0, 0, 0, 0, 1, 0, 0, 0, 0}};
        // List<Integer> D = Arrays.asList(1, 0, 0, 0, 0, 0, 0, 0, 0,0, 0, 0, 0, 0, 1, 0, 0, 0,0, 0, 0, 0, 0, 0, 0, 1, 0,0, 0, 0, 0, 0, 0, 0, 0, 1,0, 0, 0, 0, 1, 0, 0, 0, 0);
        List<Integer> D = Arrays.asList(0,5,7,8,4);

        Map<Integer, Integer> jobs_orig = new HashMap<>();
        jobs_orig.put(1, 9951);  // 0       0       0       0
        jobs_orig.put(2, 13206); // 3255    0       0       0
        jobs_orig.put(3, 3831);  // 3831    576     0       0
        jobs_orig.put(4, 4209);  // 4209    4209    3633    0

        Map<Integer, Integer[]> configs = new HashMap<>();
        configs.put(1, new Integer[]{1});
        configs.put(2, new Integer[]{2});
        configs.put(3, new Integer[]{3});
        configs.put(4, new Integer[]{4});
        configs.put(5, new Integer[]{1, 2});
        configs.put(6, new Integer[]{1, 4});
        configs.put(7, new Integer[]{2, 3});
        configs.put(8, new Integer[]{3, 4});

        List<List<Integer>> result = LocalSearch.dToJobs(jobs_orig, D, configs);
        for (List<Integer> core : result) {
            for (Integer job : core) {
                System.out.print(job);
                System.out.print(" ");
            }
            System.out.println();
        }
        
    }
}