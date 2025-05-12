package org.kuzne;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        File inputFolder = new File("input");

        if (!inputFolder.exists() || !inputFolder.isDirectory()) {
            System.err.println("Папка 'input' не найдена.");
            return;
        }

        File[] files = inputFolder.listFiles((dir, name) -> name.toLowerCase().endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.err.println("Нет файлов .txt в папке 'input'.");
            return;
        }

        for (File file : files) {
            System.out.println("\n===== Чтение файла: " + file.getName() + " =====");

            try {
                InputData data = InputParser.readInputFile(file.getPath());

                Map<Integer, Integer> jobs_orig = data.jobs_orig;
                Map<Integer, Integer[]> configs = data.configs;
                double[][] slowdown = data.slowdown;
                double[][] order = data.order;
                List<List<Integer>> initialSchedule = data.initialSchedule;

                System.out.println("Работы: " + jobs_orig.size());
                System.out.println("Конфигурации: " + configs.size());
                System.out.println("Количество ограничений порядка: " + countOrderConstraints(order));

                long startTime = System.nanoTime();
                List<Integer> finalSchedule = LocalSearch.localSearch(data);
                long endTime = System.nanoTime();

                double durationMs = (endTime - startTime) / 1_000_000.0;
                System.out.printf("Локальный поиск завершён за %.3f мс\n", durationMs);
                System.out.println("Результирующее расписание: " + finalSchedule);
//                System.out.println("Значение Cmax");

            } catch (IOException e) {
                System.err.println("Ошибка при обработке файла " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private static int countOrderConstraints(double[][] order) {
        int count = 0;
        for (double[] row : order)
            for (double val : row)
                if (val > 0)
                    count++;
        return count;
    }
}
