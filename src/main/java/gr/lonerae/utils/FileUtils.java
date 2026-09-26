package gr.lonerae.utils;

import gr.lonerae.Main;
import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;

import java.io.*;
import java.util.*;

public class FileUtils {
    private static final String TSP_INSTANCE_PATH = "/instances/TSP/%s.tsp";
    private static final String TSP_OPTIMAL_PATH = "/instances/TSP/%s.opt.tour";
    private static final String TSP_CACHE_PATH = "cache/%s.cache";
    private static final String ORIENTEERING_CACHE_PATH = "cache/%s_%s.cache";

    private static final String ORIENTEERING_INSTANCE_PATH = "/instances/Orienteering/%s.txt";

    private FileUtils() {}

    public static List<Point> readTSPInstance(String instance) {
        ArrayList<Point> points = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Main.class.getResourceAsStream(TSP_INSTANCE_PATH.formatted(instance)))
        ))) {
            String line = br.readLine();
            while (line != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length == 3) {
                    points.add(new Point(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
                } else {
                    points.add(new Point(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
                }
                line = br.readLine();
            }
            return points;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveKTSPSolution(String instance, Map<Integer, Solution> kTSPSolutions) {
        File solutionFile = new File(TSP_CACHE_PATH.formatted(instance));
        try (FileOutputStream outputStream = new FileOutputStream(solutionFile)) {
            StringBuilder builder = new StringBuilder();
            // start end e portals
            for (Map.Entry<Integer, Solution> entry : kTSPSolutions.entrySet()) {
                builder
                        .append(entry.getKey())
                        .append(" ")
                        .append(String.join("," , entry.getValue().getSequence().stream().map(Point::toString).toList()))
                        .append(" ")
                        .append(entry.getValue().getLength())
                        .append("\n");
            }
            byte[] strToBytes = builder.toString().getBytes();
            outputStream.write(strToBytes);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static List<Point> readOrienteeringInstance(String instance) {
        ArrayList<Point> points = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Main.class.getResourceAsStream(ORIENTEERING_INSTANCE_PATH.formatted(instance)))
        ))) {
            String line = br.readLine();
            while (line != null) {
                String[] parts = line.trim().split("\\s+");
                points.add(new Point(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]), parts.length > 2 ? Integer.parseInt(parts[2]) : 1));
                line = br.readLine();
            }
            return points;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void saveOrienteeringSolution(String instance, Solution solution, double timeLimit) {
        File solutionFile;
        if (timeLimit == Double.MAX_VALUE) {
            solutionFile = new File(ORIENTEERING_CACHE_PATH.formatted(instance, "MAX"));
        }
        else solutionFile = new File(ORIENTEERING_CACHE_PATH.formatted(instance, timeLimit));
        try (FileOutputStream outputStream = new FileOutputStream(solutionFile)) {
            int plurality = solution.getSequence().stream().mapToInt(Point::getPlurality).sum();
            String builder = String.join(",", solution.getSequence().stream().map(Point::toString).toList()) +
                    " " +
                    plurality +
                    " " +
                    solution.getLength();
            byte[] strToBytes = builder.getBytes();
            outputStream.write(strToBytes);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public static List<Integer> readTSPOptTour(String instance) {
        ArrayList<Integer> indexes = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Main.class.getResourceAsStream(TSP_OPTIMAL_PATH.formatted(instance))))
        )) {
            String line = br.readLine();
            while (line != null) {
                indexes.add((Integer.parseInt(line.trim()) - 1));
                line = br.readLine();
            }
            return indexes;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
