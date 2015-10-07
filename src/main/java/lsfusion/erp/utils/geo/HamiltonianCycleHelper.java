package lsfusion.erp.utils.geo;

import com.google.common.collect.Lists;
import org.jgrapht.alg.HamiltonianCycle;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

import java.util.List;

/**
 * Using jgrpaht library
 * @author pratyush
 */

public class HamiltonianCycleHelper {

    private SimpleWeightedGraph<Long, DefaultWeightedEdge> g = new SimpleWeightedGraph<>(
            DefaultWeightedEdge.class);

    public void addVertex(List<Long> ids) {
        for (Long id : ids) {
            g.addVertex(id);
        }
    }

    public void addEdge(Long source, Long destination, Long weight) {
        DefaultWeightedEdge edge = g.addEdge(source, destination);
        g.setEdgeWeight(edge, weight);
    }

    public List<Long> execute() {
        return Lists.reverse(HamiltonianCycle.getApproximateOptimalForCompleteGraph(g));
    }
}