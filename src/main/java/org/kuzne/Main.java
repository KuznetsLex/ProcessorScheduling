package org.kuzne;

import java.io.File;
import java.io.IOException;
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

                System.out.println("Работы: " + jobs_orig);
                System.out.println("Конфигурации: " + configs.keySet());
                System.out.println("Матрица slowdown: " + slowdown.length + "x" + slowdown[0].length);
                System.out.println("Количество ограничений порядка: " + countOrderConstraints(order));

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
