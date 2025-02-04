package gr.lonerae;

import gr.lonerae.graphics.Plotter;
import gr.lonerae.model.Point;
import gr.lonerae.model.Solution;
import gr.lonerae.phases.PathFindingPhase;
import gr.lonerae.phases.PreProcessingPhase;
import gr.lonerae.phases.QuadtreePhase;
import gr.lonerae.utils.CommonUtils;
import gr.lonerae.utils.FileUtils;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

public class Main {
    private static int maximumPortalsPerNodeSide = 1;


    private static final Map<String, Integer> solvedInstances = Map.ofEntries(
            entry("a280", 2579),
            entry("berlin52", 7542),
            entry("ch130", 6110),
            entry("ch150", 6528),
            entry("eil101", 629),
            entry("kroA100", 21282),
            entry("kroC100", 20749), //last circle
            entry("kroD100", 21294),
            entry("lin105", 14379),
            entry("pr76", 108159),
            entry("tsp225", 3916) //visual issues?
    );
    private static final Map<String, Integer> otherInstances = Map.ofEntries(
            entry("without_optimal_route/bier127", 118282),
            entry("without_optimal_route/dsj1000", 18659688),
            entry("without_optimal_route/kroA150", 26524),
            entry("without_optimal_route/kroA200", 29368),
            entry("without_optimal_route/kroB100", 22141),
            entry("without_optimal_route/kroB150", 26130),
            entry("without_optimal_route/kroB200", 29437),
            entry("without_optimal_route/kroE100", 22068),
            entry("without_optimal_route/lin318", 42029),
            entry("without_optimal_route/nrw1379", 56638),
            entry("without_optimal_route/pr107", 44303),
            entry("without_optimal_route/pr124", 59030),
            entry("without_optimal_route/pr136", 96772),
            entry("without_optimal_route/pr144", 58537),
            entry("without_optimal_route/pr152", 73682),
            entry("without_optimal_route/pr226", 80369),
            entry("without_optimal_route/pr264", 49135),
            entry("without_optimal_route/pr299", 48191),
            entry("without_optimal_route/pr439", 107217),
            entry("without_optimal_route/pr1002", 259045),
            entry("without_optimal_route/rat99", 1211),
            entry("without_optimal_route/rat195", 2323),
            entry("without_optimal_route/rat575", 6773),
            entry("without_optimal_route/rat783", 8806),
            entry("without_optimal_route/st70", 675),
            entry("without_optimal_route/ts225", 126643),
            entry("without_optimal_route/usa13509", 19982859)
    );


    public static void main(String[] args) {
        double avgPerfomance = 0.0;
        for (String key : solvedInstances.keySet()) {
            System.out.println("********************");
            System.out.println("INSTANCE: " + key);
            List<Point> points = FileUtils.readInstance(key);
            List<Point> original = FileUtils.readInstance(key);
            ArrayList<Integer> opt = FileUtils.readOptTour(key);

            PreProcessingPhase preProcessingPhaseInstance;
            preProcessingPhaseInstance = new PreProcessingPhase(points);
            double[] dimensions = preProcessingPhaseInstance.preprocess();

            Solution res;
            for (int portalsPerNodeSide = 1; portalsPerNodeSide <= maximumPortalsPerNodeSide; portalsPerNodeSide++) {
                System.out.println("PORTALS: " + portalsPerNodeSide + "\n");

                QuadtreePhase quadtreePhaseInstance;
                PathFindingPhase pathFindingPhase;

                quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getSquareSide(), preProcessingPhaseInstance.getPoints(), portalsPerNodeSide);
                quadtreePhaseInstance.assignPortals();

                pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getSquareSide(), points.size());
                res = pathFindingPhase.iterateNodes(quadtreePhaseInstance);
                double optimal = solvedInstances.get(key);
                System.out.println("OPTIMAL: " + optimal);
                double perfomance = -1 * CommonUtils.round((res.getLength() - optimal) * 100 /optimal);
                avgPerfomance += perfomance;
                System.out.println("PERFORMANCE: " + perfomance + "%");

//                Plotter.PlotSolution plotSolution = new Plotter.PlotSolution(res.getSequence(), dimensions);
//                plotSolution.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//                plotSolution.setVisible(true);
//                plotSolution.setTitle(key);

//                Plotter.PlotOptimal plotOptimal = new Plotter.PlotOptimal(original, opt, dimensions);
//                plotOptimal.setVisible(true);
//                plotOptimal.setTitle(key + " optimal");
            }
        }
        if (solvedInstances.size() > 1) {
            System.out.println("********************");
            System.out.println("AVG PERFORMANCE: " + CommonUtils.round(avgPerfomance/solvedInstances.size()) + "%");
        }
    }
}