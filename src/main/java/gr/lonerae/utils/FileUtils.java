package gr.lonerae.utils;

import gr.lonerae.Main;
import gr.lonerae.model.Point;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FileUtils {
    private static final String TSP_INSTANCE_PATH = "/instances/TSP/%s.tsp";
    private static final String TSP_OPTIMAL_PATH = "/instances/TSP/%s.opt.tour";

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
                points.add(new Point(Double.parseDouble(parts[1]), Double.parseDouble(parts[2])));
                line = br.readLine();
            }
            return points;
        } catch (IOException e) {
            throw new RuntimeException(e);
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
                points.add(new Point(Double.parseDouble(parts[0]), Double.parseDouble(parts[1])));
                line = br.readLine();
            }
            return points;
        } catch (IOException e) {
            throw new RuntimeException(e);
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
