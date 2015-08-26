package lsfusion.utils;

import java.util.*;

/**
 * Created by DAle on 20.08.2015.
 */

public class SpanningTreeWithBlackjack<T> {
    static final private int defaultIterations = 10000;  
    static final private int ratio = 70;
    
    static private class Edge {
        public Edge(int from, int to, int w, boolean d) { this.from = from; this.to = to; this.weight = w; this.direct = d; }
        public int from;
        public int to;
        public int weight;
        public boolean direct;
    }
    
    private Map<T, Integer> index = new HashMap<>();
    private ArrayList<Integer> weights = new ArrayList<>();
    private List<List<Edge>> graph = new ArrayList<>();
    
    public void addNode(T node, int weight) {
        assert !index.containsKey(node);
        
        index.put(node, index.size());
        weights.add(weight);
        graph.add(new ArrayList<Edge>());
    }
    
    public void addEdge(T nodeFrom, T nodeTo, int weight) {
        assert index.containsKey(nodeFrom) && index.containsKey(nodeTo); 
        int nodeFromIndex = index.get(nodeFrom);
        int nodeToIndex = index.get(nodeTo);
        graph.get(nodeFromIndex).add(new Edge(nodeFromIndex, nodeToIndex, weight, true));
        graph.get(nodeToIndex).add(new Edge(nodeToIndex, nodeFromIndex, weight, false));
    }
        
    private void checkGraph() {
        for (List<Edge> edges : graph) {
            Set<Integer> neighbours = new HashSet<>();
            for (Edge e : edges) {
                if (neighbours.contains(e.to)) {
                    throw new RuntimeException("Duplicated edges are forbidden");    
                }
                neighbours.add(e.to);
            }
        }
    }
    
    private void getComponent(int node, ArrayList<Boolean> visited, List<Integer> outComponent) {
        if (!visited.get(node)) {
            visited.set(node, true);
            outComponent.add(node);
            for (Edge e : graph.get(node)) {
                getComponent(e.to, visited, outComponent);
            }
        }
    }  
    
    private class BestTreeFinder {
        private int totalIterations;
        private ArrayList<Integer> component;
        private List<Edge> allComponentEdges;

        // обошли все варианты
        private boolean searchCompleted;
        
        private int bestResult;
        
        private int curIteration;
        
        public BestTreeFinder(ArrayList<Integer> component, List<Edge> edges, int iterations) {
            this.component = component;
            this.allComponentEdges = edges;
            this.totalIterations = iterations;
        }

        // параметром идет список ребер, ребра направленные
        List<List<Integer>> getSimpleGraph(Collection<Edge> edges) {
            Map<Integer, Integer> index = new HashMap<>();
            List<List<Integer>> graph = new ArrayList<>();
            for (Edge e : edges) {
                if (index.get(e.to) == null) {
                    index.put(e.to, index.size());
                    graph.add(new ArrayList<Integer>());
                }
                if (index.get(e.from) == null) {
                    index.put(e.from, index.size());
                    graph.add(new ArrayList<Integer>());
                }
                
                int vfrom = index.get(e.from);
                int vto = index.get(e.to);
                graph.get(vfrom).add(vto);
                graph.get(vto).add(vfrom);
            }
            return graph;
        }
        
        private void dfs(Integer node, List<List<Integer>> graph, Set<Integer> visited) {
            visited.add(node);
            for (Integer next : graph.get(node)) {
                if (!visited.contains(next)) {
                    dfs(next, graph, visited);
                }
            }
        }
        
        // параметром идет список ребер, ребра направленные
        private boolean graphIsConnected(Collection<Edge> edges) {
            List<List<Integer>> graph = getSimpleGraph(edges);
            Set<Integer> visited = new HashSet<>();
            dfs(0, graph, visited);
            return visited.size() != graph.size();
        }

        private boolean isAcyclicDfs(Integer node, Integer prev, List<List<Integer>> graph, Map<Integer, Integer> visitedColor) {
            visitedColor.put(node, 2);
            for (Integer next : graph.get(node)) {
                if (!next.equals(prev)) {
                    if (visitedColor.get(next) != null && visitedColor.get(next) == 2) {
                        return false;
                    }
                    if (visitedColor.get(next) == null) {
                        if (!isAcyclicDfs(next, node, graph, visitedColor)) {
                            return false;
                        }
                    }
                }
            }
            visitedColor.put(node, 1);
            return true;           
        }
        
        // параметром идет список ребер, ребра направленные
        private boolean graphIsAcyclic(Collection<Edge> edges) {
            List<List<Integer>> graph = getSimpleGraph(edges);
            Map<Integer, Integer> visitedColor = new HashMap<>();
            boolean isAcyclic = true;
            for (int i = 0; i < graph.size(); ++i) {
                if (visitedColor.get(i) == null) {
                    isAcyclic &= isAcyclicDfs(i, -1, graph, visitedColor);
                }
            }
            return isAcyclic;
        }
        
        private void find(List<Edge> edges, int index, int iterations, Map<Integer, Integer> curResults, int curResult, List<Edge> edgesToAdd, Set<Edge> edgesWithoutToRemove) {
            if (curResult <= bestResult) return;
            if (curIteration > iterations) return;
            
            if (edgesWithoutToRemove.size() == component.size() - 1) {
                bestResult = curResult;
//                System.out.println(bestResult);
                return;
            }
            
            Edge curEdge = edges.get(index);
            ++curIteration;
            
            // Пробуем удалить ребро
            edgesWithoutToRemove.remove(curEdge);
            if (!graphIsConnected(edgesWithoutToRemove)) {
                int oldRes = curResults.get(curEdge.to);
                int w = curEdge.weight;
                int newRes = Math.max(weights.get(curEdge.to), oldRes - w);
                curResults.put(curEdge.to, newRes);
                find(edges, index + 1, iterations, curResults, curResult + newRes - oldRes , edgesToAdd, edgesWithoutToRemove);
                curResults.put(curEdge.to, oldRes);    
            }
            edgesWithoutToRemove.add(curEdge);
            
            // Пробуем оставить ребро
            edgesToAdd.add(curEdge);
            if (graphIsAcyclic(edgesToAdd)) {
                find(edges, index + 1, iterations, curResults, curResult, edgesToAdd, edgesWithoutToRemove);                
            }
            edgesToAdd.remove(curEdge);
        }
        
        public Map<Integer, Integer> initStartValues(List<Integer> component, Collection<Edge> edges) {
            Map<Integer, Integer> result = new HashMap<>();
            for (Integer index : component) {
                result.put(index, 0);
            }
            for (Edge e : edges) {
                result.put(e.to, result.get(e.to) + e.weight); 
            }
            return result;
        }
        
        public int find() {
            searchCompleted = false;
            
            bestResult = 0;
            for (Integer nodeIndex : component) {
                bestResult += weights.get(nodeIndex);
            }
            
            int iterations = totalIterations;
            List<Edge> edges = new ArrayList<>(allComponentEdges);
            
            int firstEdgeIndex = 0;
            List<Edge> edgesToAdd = new ArrayList<>();
            Set<Edge> edgesWithoutToRemove = new HashSet<>(edges);
            
            while (iterations > 0 && !searchCompleted && firstEdgeIndex < component.size()) {
                int localIterations = iterations * ratio / 100;
                if (localIterations == 0) break;
                
                Map<Integer, Integer> initPoints = initStartValues(component, edges);
                int sumValue = 0;
                for (Integer value : initPoints.values()) {
                    sumValue += value;
                }
                
                curIteration = 0;
                find(edges, firstEdgeIndex, localIterations, initPoints, sumValue, edgesToAdd, edgesWithoutToRemove);
                
                iterations -= localIterations;
                edgesToAdd.add(edges.get(firstEdgeIndex));
                ++firstEdgeIndex;
            }
            return bestResult;
        }
        
    }
    
    private int calculateComponent(ArrayList<Integer> component, int iterations) {
        // Строим список направленных ребер, отсортированный по невозрастанию веса 
        ArrayList<Edge> componentEdges = new ArrayList<>();
        for (Integer vertexIndex : component) {
            for (Edge e : graph.get(vertexIndex)) {
                if (e.direct) {
                    componentEdges.add(e);
                }
            }
        }
        
        Collections.sort(componentEdges, new Comparator<Edge>() {
            @Override
            public int compare(Edge e1, Edge e2) {
                if (e1.weight < e2.weight) return -1;
                if (e1.weight == e2.weight) return 0;
                return 1;
            }
        });
        
        BestTreeFinder finder = new BestTreeFinder(component, componentEdges, iterations);
        return finder.find();
    }
    
    public int calculate() {
        return calculate(defaultIterations);
    }
    
    public int calculate(int iterations) {
        checkGraph();
        
        int n = graph.size();
        ArrayList<Boolean> visited = new ArrayList<>(Collections.nCopies(n, false));
        
        int result = 0;
        for (int i = 0; i < n; ++i) {
            ArrayList<Integer> outComponent = new ArrayList<>();
            getComponent(i, visited, outComponent);
            if (outComponent.size() > 0) {
                result += calculateComponent(outComponent, iterations);
            }
        }
        return result;
    }
}
