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
    private static final String INSTANCE_PATH = "/instances/%s.tsp";
    private static final String OPTIMAL_PATH = "/instances/%s.opt.tour";

    private FileUtils() {}

    public static List<Point> readInstance(String instance) {
        ArrayList<Point> points = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Main.class.getResourceAsStream(INSTANCE_PATH.formatted(instance)))
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

    public static ArrayList<Integer> readOptTour(String instance) {
        ArrayList<Integer> indexes = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(Main.class.getResourceAsStream(OPTIMAL_PATH.formatted(instance))))
        )) {
            String line = br.readLine();
            while (line != null) {
                indexes.add((Integer.parseInt(line.trim()) - 1));
                line = br.readLine();
            }
            return indexes;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
