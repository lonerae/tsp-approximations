package gr.lonerae;

import gr.lonerae.model.Point;
import gr.lonerae.phases.PathFindingPhase;
import gr.lonerae.phases.PreProcessingPhase;
import gr.lonerae.phases.QuadtreePhase;
import gr.lonerae.utils.CommonUtils;
import gr.lonerae.utils.FileUtils;

import java.util.*;

import static java.util.Map.entry;

public class Main {
//    BASE TEST (kept for sentimental reasons)
//    private static final List<Point> points = new ArrayList<>(List.of(
//            new Point(1,3),
//            new Point(2,5),
//            new Point(-1,7),
//            new Point(4,4),
//            new Point(-3,6),
//            new Point(8,-2),
//            new Point(5,-5),
//            new Point(1,7)
//    ));

    private static final int MAXIMUM_PORTALS_PER_NODE_SIDE = 1;

    private static Map<String, Integer> instances = Map.ofEntries(
            entry("/instances/a280.tsp", 2579),
            entry("/instances/berlin52.tsp", 7542),
            entry("/instances/bier127.tsp", 118282),
            entry("/instances/ch130.tsp", 6110),
            entry("/instances/ch150.tsp", 6528),
            entry("/instances/dsj1000.tsp", 18659688),
            entry("/instances/eil101.tsp", 629),
            entry("/instances/kroA100.tsp", 21282),
            entry("/instances/kroA150.tsp", 26524),
            entry("/instances/kroA200.tsp", 29368),
            entry("/instances/kroB100.tsp", 22141),
            entry("/instances/kroB150.tsp", 26130),
            entry("/instances/kroB200.tsp", 29437),
            entry("/instances/kroD100.tsp", 21294),
            entry("/instances/kroC100.tsp", 20749),
            entry("/instances/kroE100.tsp", 22068),
            entry("/instances/lin105.tsp", 14379),
            entry("/instances/lin318.tsp", 42029),
            entry("/instances/nrw1379.tsp", 56638),
            entry("/instances/pr76.tsp", 108159),
            entry("/instances/pr107.tsp", 44303),
            entry("/instances/pr124.tsp", 59030),
            entry("/instances/pr136.tsp", 96772),
            entry("/instances/pr144.tsp", 58537),
            entry("/instances/pr152.tsp", 73682),
            entry("/instances/pr226.tsp", 80369),
            entry("/instances/pr264.tsp", 49135),
            entry("/instances/pr299.tsp", 48191),
            entry("/instances/pr439.tsp", 107217),
            entry("/instances/pr1002.tsp", 259045),
            entry("/instances/rat99.tsp", 1211),
            entry("/instances/rat195.tsp", 2323),
            entry("/instances/rat575.tsp", 6773),
            entry("/instances/rat783.tsp", 8806),
            entry("/instances/st70.tsp", 675),
            entry("/instances/ts225.tsp", 126643),
            entry("/instances/tsp225.tsp", 3916),
            entry("/instances/usa13509.tsp", 19982859)
    );


    public static void main(String[] args) {
        for (Map.Entry<String,Integer> entry : instances.entrySet()) {
            System.out.println("INSTANCE: " + entry.getKey());
            List<Point> points = FileUtils.readInstance(entry.getKey());

            PreProcessingPhase preProcessingPhaseInstance;
            preProcessingPhaseInstance = new PreProcessingPhase(points);
            preProcessingPhaseInstance.preprocess();

            double res;
            for (int portalsPerNodeSide = 1; portalsPerNodeSide <= MAXIMUM_PORTALS_PER_NODE_SIDE; portalsPerNodeSide++) {
                System.out.println("********************");
                System.out.println("PORTALS: " + portalsPerNodeSide + "\n");

                QuadtreePhase quadtreePhaseInstance;
                PathFindingPhase pathFindingPhase;

                quadtreePhaseInstance = new QuadtreePhase(preProcessingPhaseInstance.getSquareSide(), preProcessingPhaseInstance.getPoints(), portalsPerNodeSide);
                quadtreePhaseInstance.assignPortals();

                pathFindingPhase = new PathFindingPhase(preProcessingPhaseInstance.getSquareSide(), points.size());
                res = pathFindingPhase.iterateNodes(quadtreePhaseInstance);
                double optimal = entry.getValue();
                System.out.println("OPTIMAL: " + optimal);
                System.out.println("PERFORMANCE: " + -1 * CommonUtils.round((res - optimal)/optimal) * 100 + "%");
            }
        }
    }
}