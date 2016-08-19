package lsfusion.utils;

import lsfusion.utils.prim.Prim;
import lsfusion.utils.prim.UndirectedGraph;
import org.junit.Assert;
import org.junit.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class SpanningTreeWithBlackjackTest {

    @Test
    public void testZeroWeights() {

        int MAXNODES = 40;
        int MAXCOST = 20;
        int ITERATIONS = 1000;

        for (int nodeCount = 2; nodeCount < MAXNODES; nodeCount++) {
            for (int i = 1; i < nodeCount; i++) {
                int edgeCount = nodeCount + i * 2;

                SpanningTreeWithBlackjack<Integer> tree = new SpanningTreeWithBlackjack<>();
                UndirectedGraph<Integer> prim = new UndirectedGraph<>();

                addNode(tree, prim, -1);

                for (int j = 0; j < nodeCount; j++) {
                    addNode(tree, prim, j);
                    addEdge(tree, prim, -1, j, 0);
                }

                Set<Integer> edgesExist = new HashSet<>();
                for (int j = 0; j < edgeCount; j++) {
                    int maxEdge = nodeCount * nodeCount;
                    int edgeI = random(maxEdge);
                    while (!edgesExist.add(edgeI)) {
                        edgeI++;
                        if (edgeI >= maxEdge)
                            edgeI = 0;
                    }

                    int edgeFrom = edgeI / nodeCount;
                    int edgeTo = edgeI % nodeCount;

                    if (!edgesExist.contains(edgeTo * nodeCount + edgeFrom)) {
                        int cost = random(MAXCOST);
                        addEdge(tree, prim, edgeFrom, edgeTo, cost);
                    }
                }

                checkResult(nodeCount, edgeCount, tree, ITERATIONS, prim);
            }
        }
    }

    private static void checkResult(int nodeCount, int edgeCount, SpanningTreeWithBlackjack<Integer> tree, int iterations, UndirectedGraph<Integer> prim) {
        long stime = System.currentTimeMillis();
        int primResult = -Prim.mst(prim).calculateTotalEdgeCost();
        long primTime = System.currentTimeMillis() - stime;
        stime = System.currentTimeMillis();
        int treeResult = tree.calculate(iterations);
        long treeTime = System.currentTimeMillis() - stime;
        String time = ", nodecount : " + nodeCount + ", edgecount: " + edgeCount + ", treetime : " + treeTime + ", primtime : " + primTime;
        assertEquals(primResult, treeResult);
        
        if(primResult != treeResult)
            System.out.println("tree : " + treeResult + ", prim : " + primResult + time);
        else
            System.out.println("passed : " + primResult + time);
    }

    @Test
    public void testSample() {
        SpanningTreeWithBlackjack<String> tree = new SpanningTreeWithBlackjack<>();
        UndirectedGraph<String> prim = new UndirectedGraph<>();

        addNode(tree, prim, "A", 0);
        addNode(tree, prim, "B", 7);
        addNode(tree, prim, "C", 0);
        addNode(tree, prim, "D", 0);
        addNode(tree, prim, "E", 6);
        addNode(tree, prim, "F", 13);
        addNode(tree, prim, "G", 13);
        addNode(tree, prim, "H", 0);
        addNode(tree, prim, "I", 0);

        addEdge(tree, prim, "A", "B", 5);
        addEdge(tree, prim, "C", "B", 7);
        addEdge(tree, prim, "C", "F", 7);
        addEdge(tree, prim, "D", "B", 6);
        addEdge(tree, prim, "D", "E", 6);
        addEdge(tree, prim, "I", "E", 6);
        addEdge(tree, prim, "E", "F", 6);
        addEdge(tree, prim, "F", "G", 13);
        addEdge(tree, prim, "H", "G", 13);

        int resPrim = (-Prim.mst(prim).calculateTotalEdgeCost());
        System.out.println("prim : " + resPrim);
        assertEquals(resPrim, 63);
        int resTree = tree.calculate();
        System.out.println("tree : " + resTree);
        assertEquals(resTree, 69);
    }

    private static void addEdge(SpanningTreeWithBlackjack<String> tree, UndirectedGraph<String> prim, String a, String b, int weight) {
        tree.addEdge(a, b, weight);
        prim.addEdge(a, b, -weight);
    }

    private static void addNode(SpanningTreeWithBlackjack<String> tree, UndirectedGraph<String> prim, String a, int weight) {
        tree.addNode(a, weight);
        prim.addNode(a);
    }
    
    
    private static void addEdge(SpanningTreeWithBlackjack<Integer> tree, UndirectedGraph<Integer> prim, int edgeFrom, int edgeTo, int cost) {
        tree.addEdge(edgeFrom, edgeTo, cost);
        prim.addEdge(edgeFrom, edgeTo, - cost);
    }

    private static void addNode(SpanningTreeWithBlackjack<Integer> tree, UndirectedGraph<Integer> prim, int j) {
        tree.addNode(j, 0);
        prim.addNode(j);
    }

    private static int random(int count) {
        return (int)(Math.random() * count);
    }

    private static SpanningTreeWithBlackjack<Integer> initWithRandomGraph(int nodeCount, int edgeCount, int maxCost) {

        SpanningTreeWithBlackjack<Integer> algo = new SpanningTreeWithBlackjack<>();
        Random generator = new Random(nodeCount + edgeCount);

        for (int i = 0; i < nodeCount; ++i) {
            algo.addNode(i, 0);
        }
        List<Integer> maxWeights = new ArrayList<>(Collections.nCopies(nodeCount, 0));

        Set<Integer> edgesExist = new HashSet<>();
        for (int j = 0; j < edgeCount; j++) {
            int maxEdge = nodeCount * nodeCount;
            int edgeI = generator.nextInt(maxEdge);
            while (!edgesExist.add(edgeI)) {
                edgeI++;
                if (edgeI >= maxEdge)
                    edgeI = 0;
            }

            int edgeFrom = edgeI / nodeCount;
            int edgeTo = edgeI % nodeCount;

            if (!edgesExist.contains(edgeTo * nodeCount + edgeFrom)) {
                int cost = generator.nextInt(maxCost + 1);
                algo.addEdge(edgeFrom, edgeTo, cost);
                maxWeights.set(edgeTo, maxWeights.get(edgeTo) + cost);
            }
        }

        for (int i = 0; i < nodeCount; ++i) {
            algo.setNodeWeight(i, generator.nextInt(Math.max(1, maxWeights.get(i))));
        }
        return algo;
    }

    @Test
    public void testTimes() throws IOException {
        final int MAXNODES = 40;
        final int MAXCOST = 20;
        final int ITERATIONS = 10000;

        FileWriter writer = new FileWriter("D:/spt.txt");

        for (int nodeCount = 2; nodeCount < MAXNODES; nodeCount++) {
            for (int i = 0; i + 1 < nodeCount; i++) {
                int edgeCount = nodeCount - 1 + i * 2;
                if (edgeCount <= (nodeCount * (nodeCount - 1)) / 2) {
                    SpanningTreeWithBlackjack<Integer> tree = initWithRandomGraph(nodeCount, edgeCount, MAXCOST);
                    checkTimesResult(nodeCount, edgeCount, tree, ITERATIONS, writer);
                }
            }
        }
        writer.close();
    }

    private static void checkTimesResult(int nodeCount, int edgeCount, SpanningTreeWithBlackjack<Integer> tree, int iterations, FileWriter writer) {
        long stime = System.currentTimeMillis();
        int treeResult = tree.calculate(iterations);
        long treeTime = System.currentTimeMillis() - stime;
        String str = "" + nodeCount + "," + edgeCount + " t:" + treeTime;
        String result = " r:" + treeResult + "\n";
        try {
            writer.write(str + result);
        } catch (IOException e) {
            Assert.assertFalse(true);
            e.printStackTrace();
        }
    }
    
}