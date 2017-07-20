package lsfusion.utils;

import lsfusion.utils.GreedyTreeBuilding.*;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static lsfusion.utils.GreedyTreeBuilding.*;
    
public class GreedyTreeBuildingTest {
    private static class WeightedEdge<T> extends GreedyTreeBuilding.SimpleEdge<T> {
        int w;

        public WeightedEdge(T from, T to, int weight) {
            super(from, to);
            w = weight;
        }
    }

    TreeCutComparator<Integer> cutComparator = new TreeCutComparator<Integer>() {
        @Override
        public int compare(Integer a, Integer b) {
            if (a < b) return -1;
            if (a > b) return 1;
            return 0;
        }
    };
    
    @Test
    public void test1() {
        GreedyTreeBuilding<Integer, Integer, WeightedEdge<Integer>> algo = new GreedyTreeBuilding<>();
        int cnt = 5;
        Integer[] num = {0, 1, 2, 3, 4};

        for (int i = 0; i < cnt; ++i) {
            algo.addVertex(num[i], 0);
        }

        algo.addEdge(new WeightedEdge<>(num[0], num[1], 5));
        algo.addEdge(new WeightedEdge<>(num[0], num[2], 10));
        algo.addEdge(new WeightedEdge<>(num[1], num[2], 6));
        algo.addEdge(new WeightedEdge<>(num[1], num[3], 4));
        algo.addEdge(new WeightedEdge<>(num[2], num[3], 2));
        algo.addEdge(new WeightedEdge<>(num[2], num[4], 5));
        algo.addEdge(new WeightedEdge<>(num[3], num[4], 4));

        CalculateCost<Integer, Integer, WeightedEdge<Integer>> func = new CalculateCost<Integer, Integer, WeightedEdge<Integer>>() {
            @Override
            public Integer calculate(Node<Integer, Integer> a, Node<Integer, Integer> b, Iterable<WeightedEdge<Integer>> edges) {
                assert a != null && b != null;
                if (!edges.iterator().hasNext()) return 100;
                int minw = 100;
                for (Edge<Integer> e : edges) minw = Math.min(minw, ((WeightedEdge<Integer>)e).w);
                return a.getCost() + b.getCost() + minw;
            }

            @Override
            public Integer calculateLowerBound(Node<Integer, Integer> a, Node<Integer, Integer> b, Iterable<WeightedEdge<Integer>> edges) {
                return calculate(a, b, edges) - 1;
            }
        };

        TreeNode<Integer, Integer> res = algo.compute(func);
        assertEquals(res.node.getCost().intValue(), 15);
        TreeNode<Integer, Integer> res2 = algo.computeDP(func);
        assertEquals(res2.node.getCost().intValue(), 15);
        TreeNode<Integer, Integer> res3 = algo.computeWithVertex(num[4], func, cutComparator);
        assertEquals(res3.node.getCost().intValue(), 4);

        System.out.println(res.node.getCost());
        drawTree(res);
        System.out.println(res2.node.getCost());
        drawTree(res2);
        System.out.println(res3.node.getCost());
        drawTree(res3);
    }

    @Test
    public void test2() {
        GreedyTreeBuilding<Integer, Integer, WeightedEdge<Integer>> algo = new GreedyTreeBuilding<>();
        int cnt = 8;
        Integer[] num = {0, 1, 2, 3, 4, 5, 6, 7};

        for (int i = 0; i < cnt; ++i) {
            algo.addVertex(num[i], 0);
        }

        algo.addEdge(new WeightedEdge<>(num[0], num[1], 10));
        algo.addEdge(new WeightedEdge<>(num[0], num[2], 5));
        algo.addEdge(new WeightedEdge<>(num[1], num[2], 10));
        algo.addEdge(new WeightedEdge<>(num[1], num[3], 2));
        algo.addEdge(new WeightedEdge<>(num[1], num[4], 7));
        algo.addEdge(new WeightedEdge<>(num[2], num[3], 10));
        algo.addEdge(new WeightedEdge<>(num[2], num[4], 4));
        algo.addEdge(new WeightedEdge<>(num[3], num[5], 6));
        algo.addEdge(new WeightedEdge<>(num[3], num[7], 7));
        algo.addEdge(new WeightedEdge<>(num[4], num[5], 3));
        algo.addEdge(new WeightedEdge<>(num[4], num[6], 7));
        algo.addEdge(new WeightedEdge<>(num[5], num[6], 5));
        algo.addEdge(new WeightedEdge<>(num[5], num[7], 3));

        CalculateCost<Integer, Integer, WeightedEdge<Integer>> func = new CalculateCost<Integer, Integer, WeightedEdge<Integer>>() {
            @Override
            public Integer calculate(Node<Integer, Integer> a, Node<Integer, Integer> b, Iterable<WeightedEdge<Integer>> edges) {
                assert a != null && b != null;
                if (!edges.iterator().hasNext()) return 100;
                int minw = 100;
                for (Edge<Integer> e : edges) minw = Math.min(minw, ((WeightedEdge<Integer>)e).w);
                return Math.min(Math.min(a.isInner() ? a.getCost() : 100, b.isInner() ? b.getCost() : 100), minw);
            }

            public Integer calculateLowerBound(Node<Integer, Integer> a, Node<Integer, Integer> b, Iterable<WeightedEdge<Integer>> edges) {
                return calculate(a, b, edges) - 1;
            }
        };

        TreeNode<Integer, Integer> res = algo.compute(func);
        assertEquals(res.node.getCost().intValue(), 2);
        TreeNode<Integer, Integer> res2 = algo.computeDP(func);
        assertEquals(res2.node.getCost().intValue(), 2);
        TreeNode<Integer, Integer> res3 = algo.computeWithVertex(num[4], func, cutComparator);
        assertEquals(res3.node.getCost().intValue(), 2);

        System.out.println(res.node.getCost());
        drawTree(res);
        System.out.println(res2.node.getCost());
        drawTree(res2);
        System.out.println(res3.node.getCost());
        drawTree(res3);
    }
}