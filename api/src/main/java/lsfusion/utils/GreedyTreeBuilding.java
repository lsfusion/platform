package lsfusion.utils;

import java.util.*;

/**
 *  Реализует алгоритм построения бинарного дерева по набору вершин.
 *  <p>
 *  Входные данные: исходный граф G(V, E) - набор вершин и заданных ребер между ними. 
 *  Также необходима оценочная функция - реализация интерфейса CalculateCost, результат выполнения которой
 *  зависит от двух вершин изменяемого графа G'(N, E') и набора ребер исходного графа G.
 *  <p>
 *  Алгоритм метода compute(): 
 *  По исходному графу G, строим новый граф G'(N, E'), вершинами которого являются объекты класса Node, которые
 *  представляют собой подможножество вершин исходного графа G. Изначально каждой вершине исходного графа соответствует
 *  вершина графа G'. Граф G' всегда полный. 
 *  Затем находим стоимость каждого ребра в полном графе G' с помощью вызова оценочной функции, если в исходном графе не было
 *  ребер между вершинами, то в функцию передается пустой набор ребер.
 *  Затем на каждом шаге находим минимальное ребро, и объединяем вершины этого ребра в одну вершину. При этом две вершины
 *  графа G' удаляются и добавляется новая, для которой пересчитываются все ребра с остальными вершинами графа. Новой 
 *  вершине устанавливается стоимость, равная стоимости минимального ребра. 
 */

public class GreedyTreeBuilding<V, C extends Comparable<C>, E extends GreedyTreeBuilding.Edge<V>> {
    public interface Edge<V> {
        V getFrom();
        V getTo();
    }
    
    public interface CalculateCost<V, C extends Comparable<C>, E extends GreedyTreeBuilding.Edge<V>> {
        C calculate(Node<V, C> a, Node<V, C> b, Iterable<E> edges); 
    }
    
    
    /** Элемент дерева содержащий Node */
    static public final class TreeNode<V, C> {
        public Node<V, C> node;
        public TreeNode<V, C> parent, left, right;
        
        public TreeNode(Node<V, C> node) {
            this.node = node;
        }
    } 
    
    /** Вершина изменяющегося графа, является объединением некоторого подмножества вершин исходного графа */ 
    static public final class Node<V, C> {
        private final V initialVertex;
        private C cost;
        
        public Node(V vertex, C cost) {
            this.initialVertex = vertex;
            this.cost = cost;
        } 
        
        public boolean isInner() {
            return initialVertex == null;
        }
        
        public V getVertex() {
            return initialVertex;
        }
        
        public C getCost() {
            return cost;
        }
        
        public void setCost(C cost) { this.cost = cost; }
    }

    /** Ребро исходного графа (по умолчанию) */
    static class SimpleEdge<V> implements Edge<V> {
        private final V from, to;
        
        public SimpleEdge(V from, V to) {
            this.from = from;
            this.to = to;
        }
        
        @Override
        public V getFrom() {
            return from;
        }

        @Override
        public V getTo() {
            return to;
        }
    }
    
    /** Ребро изменяющегося графа, являющееся объединением ребер исходного графа */ 
    static class ComplexEdge<V, C extends Comparable<C>, E extends Edge<V>> implements Edge<Node<V, C>>, Comparable<ComplexEdge<V, C, E>> {
        private final C cost;
        private final Node<V, C> from;
        private final Node<V, C> to;
        private EdgeLinkedList<E> simpleEdges;
        
        public ComplexEdge(Node<V, C> from, Node<V, C> to, EdgeLinkedList<E> simpleEdges, C cost) {
            this.from = from;
            this.to = to;
            this.cost = cost;
            this.simpleEdges = simpleEdges;
        }
        
        @Override
        public int compareTo(ComplexEdge<V, C, E> o) {
            return cost.compareTo(o.getCost());
        }

        @Override
        public Node<V, C> getFrom() {
            return from;
        }

        @Override
        public Node<V, C> getTo() {
            return to;
        }

        public C getCost() {
            return cost;
        }
        
        public EdgeLinkedList<E> mergeSimpleEdges(ComplexEdge<V, C, E> otherEdge) {
            simpleEdges.addList(otherEdge.simpleEdges);
            return simpleEdges;
        }
    }
    
    private static class EdgeLinkedList<E extends Edge> implements Iterable<E> {
        @Override
        public Iterator<E> iterator() {
            return new EdgeLinkedListIterator();
        }

        private class EdgeLinkedListIterator implements Iterator<E> {
            private ListNode<E> node;
            
            public EdgeLinkedListIterator() {
                node = head;
            }

            @Override
            public boolean hasNext() {
                return node != null;
            }

            @Override
            public E next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                ListNode<E> tmp = node;
                node = node.next;
                return tmp.value;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        } 
        
        public static class ListNode<E extends Edge> {
            public ListNode(E value) { this.value = value; }
            public ListNode<E> next;
            public E value; 
        }
        
        private ListNode<E> head;
        private ListNode<E> tail;
        
        public ListNode<E> getHead() {
            return head;
        }
        
        public ListNode<E> getTail() {
            return tail;
        } 
        
        public void addEdge(E edge) {
            ListNode<E> newNode = new ListNode<>(edge);    
            if (head == null) {
                head = newNode;
            } 
            if (tail != null) {
                tail.next = newNode;
            }
            tail = newNode;
        }
        
        public void addList(EdgeLinkedList<E> secondList) {
            if (secondList.getHead() == null) {
                return;
            }
            if (head == null) {
                head = secondList.getHead();
                tail = secondList.getTail();
            } else {
                tail.next = secondList.getHead();
                tail = secondList.getTail();
            }                
        }
    }
    
    private List<Collection<E>> adjList = new ArrayList<>();
    private List<C> costs = new ArrayList<>(); 
    private List<V> vertices = new ArrayList<>();
    private Map<V, Integer> vertexIndex = new HashMap<>(); 
    
    public void addEdge(E e) {
        assert vertexIndex.containsKey(e.getFrom());
        assert vertexIndex.containsKey(e.getTo());
        
        int fromIndex = vertexIndex.get(e.getFrom());
        adjList.get(fromIndex).add(e);
    }
    
    public void addVertex(V vertex, C cost) {
        assert vertex != null;
        assert !vertexIndex.containsKey(vertex);
        
        vertices.add(vertex);
        vertexIndex.put(vertex, vertexIndex.size());
        adjList.add(new ArrayList<E>());
        costs.add(cost);
    }

    private PriorityQueue<ComplexEdge<V, C, E>> queue = new PriorityQueue<>();
    private final Map<Node<V, C>, Integer> nodeIndex = new HashMap<>();
    private final List<Node<V, C>> nodes = new ArrayList<>();
    private final List<TreeNode<V, C>> treeNodes = new ArrayList<>();
    private List<List<ComplexEdge<V, C, E>>> adjMatrix = new ArrayList<>(); 
    
    private TreeNode<V, C> addNode(V vertex, C cost) {
        Node<V, C> node = new Node<>(vertex, cost);
        TreeNode<V, C> treeNode = new TreeNode<>(node);
        nodes.add(node);
        treeNodes.add(treeNode);
        nodeIndex.put(node, nodes.size()-1);
        return treeNode;
    }
    
    private void initNodes() {
        nodeIndex.clear();
        nodes.clear();
        treeNodes.clear();
        
        for (int i = 0; i < vertices.size(); ++i) {
            addNode(vertices.get(i), costs.get(i));
        }
    }
    
    private void initEdges(CalculateCost<V, C, E>  functor) {
        queue.clear();
        adjMatrix.clear();

        final int vertexCnt = vertices.size();
        
        for (int i = 0; i < vertexCnt; ++i) {
            adjMatrix.add(new ArrayList<>(Collections.<ComplexEdge<V, C, E>>nCopies(vertexCnt, null)));
        }
        
        for (int i = 0; i < vertexCnt; ++i) {
            for (E edge : adjList.get(i)) {
                int fromIndex = vertexIndex.get(edge.getFrom());
                int toIndex = vertexIndex.get(edge.getTo());
                EdgeLinkedList<E> simpleEdges = new EdgeLinkedList<>();
                simpleEdges.addEdge(edge);
                C edgeCost = functor.calculate(nodes.get(fromIndex), nodes.get(toIndex), simpleEdges);
                ComplexEdge<V, C, E> newEdge = new ComplexEdge<>(nodes.get(fromIndex), nodes.get(toIndex), simpleEdges, edgeCost);
                
                adjMatrix.get(fromIndex).set(toIndex, newEdge);
                adjMatrix.get(toIndex).set(fromIndex, newEdge);
                queue.add(newEdge);
            }
        }

        for (int i = 0; i < vertexCnt; ++i) {
            for (int j = i+1; j < vertexCnt; ++j) {
                if (adjMatrix.get(i).get(j) == null) {
                    C edgeCost = functor.calculate(nodes.get(i), nodes.get(j), new LinkedList<E>());
                    ComplexEdge<V, C, E> newEdge = new ComplexEdge<>(nodes.get(i), nodes.get(j), new EdgeLinkedList<E>(), edgeCost);
                    adjMatrix.get(i).set(j, newEdge);
                    adjMatrix.get(j).set(i, newEdge);
                    queue.add(newEdge);
                }
            }
        }
    }

    private ComplexEdge<V, C, E> getMinimumEdge(Set<Node<V, C>> deleted) {
        while (!queue.isEmpty()) {
            ComplexEdge<V, C, E> nextEdge = queue.poll();
            if (!deleted.contains(nextEdge.getFrom()) && !deleted.contains(nextEdge.getTo())) {
                return nextEdge;
            }
        } 
        return null;
    }

    private TreeNode<V, C> getTreeNode(Node<V, C> node) {
        if (node == null) {
            return null;
        }
        return treeNodes.get(nodeIndex.get(node)); 
    }
    
    private void fillTreeNode(TreeNode<V, C> node, Node<V, C> left, Node<V, C> right) {
        node.left = getTreeNode(left);
        node.right = getTreeNode(right);
        node.right.parent = node;
        node.left.parent = node;
    }
    
    private void joinNodes(ComplexEdge<V, C, E> edge, Set<Node<V, C>> deleted, CalculateCost<V, C, E> functor) {
        int fromIndex = nodeIndex.get(edge.getFrom());
        int toIndex = nodeIndex.get(edge.getTo());
        
        TreeNode<V, C> newTreeNode = addNode(null, edge.getCost());
        fillTreeNode(newTreeNode, edge.getFrom(), edge.getTo());
        
        int newIndex = nodeIndex.get(newTreeNode.node);
        
        int nodeCnt = adjMatrix.size();
        adjMatrix.add(new ArrayList<>(Collections.<ComplexEdge<V, C, E>>nCopies(nodeCnt + 1, null)));
        for (int i = 0; i < nodeCnt; ++i) {
            if (i != fromIndex && i != toIndex && !deleted.contains(nodes.get(i))) {
                EdgeLinkedList<E> edges = adjMatrix.get(fromIndex).get(i).mergeSimpleEdges(adjMatrix.get(toIndex).get(i));
                C cost = functor.calculate(newTreeNode.node, nodes.get(i), edges);
                ComplexEdge<V, C, E> newEdge = new ComplexEdge<>(newTreeNode.node, nodes.get(i), edges, cost);
                adjMatrix.get(newIndex).set(i, newEdge);
                adjMatrix.get(i).add(newEdge);
                queue.add(newEdge);
            }
        }
        
        deleted.add(edge.getFrom());
        deleted.add(edge.getTo());
    } 
    
    public TreeNode<V, C> compute(CalculateCost<V, C, E> functor) {
        initNodes();        
        initEdges(functor);
        
        Set<Node<V, C>> deletedNodes = new HashSet<>();
        for (int i = 0; i + 1 < vertices.size(); ++i) {
            ComplexEdge<V, C, E> nextEdge = getMinimumEdge(deletedNodes);
            assert nextEdge != null;
           
            joinNodes(nextEdge, deletedNodes, functor);
        }
        return treeNodes.get(treeNodes.size() - 1); 
    }

    private int indexFromMask(int mask) {
        return (int)Math.round(Math.log(mask) / Math.log(2.0));        
    }
    
    private TreeNode<V, C> DP(int mask, ArrayList<TreeNode<V, C>> memo, CalculateCost<V, C, E> functor) {
        if (memo.get(mask) != null) {
            return memo.get(mask);
        }
        if ((mask & (mask-1)) == 0) { // если подмножество состоит из одного элемента
            int nodeIndex = indexFromMask(mask);
            memo.set(mask, treeNodes.get(nodeIndex));
            return treeNodes.get(nodeIndex);
        }
        
        C bestCost = null;
        int bestSubmask = -1;
        
        // перебор всех подмасок (http://e-maxx.ru/algo/all_submasks) суммарно за O(3^n)
        for (int submask = mask; submask != 0; submask = (submask - 1) & mask) {
            if (submask != mask) {
                int submask2 = mask ^ submask;
                if (submask < submask2) { // для исключения одинаковых разбиений (предполагаем, что функция оценки коммутативна)
                    TreeNode<V, C> firstNode = DP(submask, memo, functor);
                    TreeNode<V, C> secondNode = DP(submask2, memo, functor);
                    EdgeLinkedList<E> edgeList = new EdgeLinkedList<>();
                    for (int firstIndex = 0; firstIndex < nodes.size(); ++firstIndex) {
                        if ((submask & (1<<firstIndex)) != 0) {
                            for (E edge : adjList.get(firstIndex)) {
                                int secondIndex;
                                if (vertexIndex.get(edge.getTo()) == firstIndex) {
                                    secondIndex = vertexIndex.get(edge.getFrom());
                                } else {
                                    secondIndex = vertexIndex.get(edge.getTo());
                                }
                                if ((submask2 & (1<<secondIndex)) != 0) {
                                    edgeList.addEdge(edge);
                                }
                            }
                        }
                    }
                    C curCost = functor.calculate(firstNode.node, secondNode.node, edgeList);
                    if (bestCost == null || bestCost.compareTo(curCost) > 0) {
                        bestCost = curCost;
                        bestSubmask = submask;
                    }
                }
            }
        }
        
        TreeNode<V, C> resNode = addNode(null, bestCost);
        fillTreeNode(resNode, memo.get(bestSubmask).node, memo.get(mask ^ bestSubmask).node);
        memo.set(mask, resNode);
        return resNode;
    }
    
    public TreeNode<V, C> computeDP(CalculateCost<V, C, E> functor) {
        initNodes();
        assert nodes.size() <= 12;
        int subsetsCount = (1 << nodes.size());
        return DP(subsetsCount - 1, new ArrayList<>(Collections.<TreeNode<V, C>>nCopies(subsetsCount, null)), functor);
    }
    
    private void squeezeTree(TreeNode<V, C> parent, TreeNode<V, C> node, TreeNode<V, C> child) {
        assert parent != null;
        if (parent.left == node) {
            parent.left = child;
        } else {
            parent.right = child;
        }
        child.parent = parent;
    }
    
    private void expandTree(TreeNode<V, C> parent, TreeNode<V, C> node, TreeNode<V, C> child) {
        if (parent.left == child) {
            parent.left = node;
        } else {
            parent.right = node;
        }
        child.parent = node;
    }
    
    private void collectVertexIndices(TreeNode<V, C> node, Set<Integer> indices) {
        if (node.node.isInner()) {
            if (node.left != null) {
                collectVertexIndices(node.left, indices);
            }
            if (node.right != null) {
                collectVertexIndices(node.right, indices);
            }
        } else {
            indices.add(vertexIndex.get(node.node.initialVertex));
        }
    }
    
    private List<E> getEdges(Set<Integer> leftNodeIndices, Set<Integer> rightNodeIndices) {
        List<E> edges = new ArrayList<>(); 
        for (int leftIndex : leftNodeIndices) {
            for (E edge : adjList.get(leftIndex)) {
                int fromIndex = vertexIndex.get(edge.getFrom());
                int toIndex = vertexIndex.get(edge.getTo());
                if (leftIndex == toIndex && rightNodeIndices.contains(fromIndex) ||
                    leftIndex == fromIndex && rightNodeIndices.contains(toIndex)) {
                    edges.add(edge);            
                }
            }
        }
        return edges;
    }
    
    private C countTreeCost(TreeNode<V, C> siblingNode, CalculateCost<V, C, E> functor) {
        Set<Integer> newIndices = new HashSet<>();
        collectVertexIndices(siblingNode, newIndices);
        TreeNode<V, C> curNode = siblingNode;
        C newCost = siblingNode.node.getCost();
                
        while (curNode.parent != null) {
            TreeNode<V, C> oldNode = getSibling(curNode);
            Set<Integer> oldIndices = new HashSet<>();
            collectVertexIndices(oldNode, oldIndices);
            
            List<E> edges = getEdges(newIndices, oldIndices);
            
            C oldCost = curNode.node.getCost();
            curNode.node.setCost(newCost);
            newCost = functor.calculate(curNode.node, oldNode.node, edges);
            curNode.node.setCost(oldCost);
            
            newIndices.addAll(oldIndices);
            curNode = curNode.parent;
        }
        return newCost;
    }
    
    private int treeSize(TreeNode<V, C> node) {
        int res = 1;
        if (node.left != null) res += treeSize(node.left);
        if (node.right != null) res += treeSize(node.right);
        return res;    
    }

    private TreeNode<V, C> getSibling(TreeNode<V, C> node) {
        TreeNode<V, C> parentNode = node.parent;
        if (parentNode.left == node) {
            return parentNode.right;
        } else {
            return parentNode.left;
        }
    }
    
    private void traverseCut(TreeNode<V, C> curNode, TreeNode<V, C> startNode, ComputationResult<V, C> result, CalculateCost<V, C, E> functor) {
        boolean goDeeper = true;
        if (curNode != startNode) {
            TreeNode<V, C> parentNode = curNode.parent;
            assert parentNode.parent != null;
            TreeNode<V, C> siblingNode = getSibling(curNode);
            squeezeTree(parentNode.parent, parentNode, siblingNode);
            C treeCost = countTreeCost(siblingNode, functor);
            int cutTreeSize = treeSize(curNode);
            if (treeCost.compareTo(result.cost) < 0 || treeCost.compareTo(result.cost) == 0 && result.cutVertexCount < cutTreeSize) {
                result.cost = treeCost;
                result.cutNode = curNode;
                result.cutVertexCount = cutTreeSize;
                goDeeper = false;
            }
            expandTree(parentNode.parent, parentNode, siblingNode);
        }
        if (goDeeper) {
            if (curNode.left != null) {
                traverseCut(curNode.left, startNode, result, functor);
            }
            if (curNode.right != null) {
                traverseCut(curNode.right, startNode, result, functor);
            }
        }
    }
    
    private static class ComputationResult<V, C> {
        public C cost;
        public int cutVertexCount;
        public TreeNode<V, C> cutNode;
        
        public ComputationResult(C cost, int cutVertexCount, TreeNode<V, C> cutNode) {
            this.cost = cost;
            this.cutVertexCount = cutVertexCount;
            this.cutNode = cutNode;
        }
    }
    
    private TreeNode<V, C> createTreeWithCut(TreeNode<V, C> cutNode, CalculateCost<V, C, E> functor) {
        TreeNode<V, C> parentNode = cutNode.parent;
        TreeNode<V, C> siblingNode = getSibling(cutNode);
        squeezeTree(parentNode.parent, parentNode, siblingNode);

        TreeNode<V, C> curNode = siblingNode;
        
        Set<Integer> newIndices = new HashSet<>();
        collectVertexIndices(curNode, newIndices);
        
        while (curNode.parent != null) {
            TreeNode<V, C> oldNode = getSibling(curNode);
            Set<Integer> oldIndices = new HashSet<>();
            collectVertexIndices(oldNode, oldIndices);

            List<E> edges = getEdges(newIndices, oldIndices);

            C newCost = functor.calculate(curNode.node, oldNode.node, edges);
            
            newIndices.addAll(oldIndices);
            curNode = curNode.parent;
            curNode.node.setCost(newCost);
        }
        return curNode;
    }
    
    private TreeNode<V, C> computePartialGreedy(V vertex, CalculateCost<V, C, E> functor) {
        assert vertexIndex.containsKey(vertex);
        initNodes();
        initEdges(functor);

        ComplexEdge<V, C, E> nextEdge;
        Set<Node<V, C>> deletedNodes = new HashSet<>();
        do {
            nextEdge = getMinimumEdge(deletedNodes);
            assert nextEdge != null;
            joinNodes(nextEdge, deletedNodes, functor);
        } while (nextEdge.getFrom().initialVertex != vertex && nextEdge.getTo().initialVertex != vertex);

        return treeNodes.get(treeNodes.size() - 1);
    }
    
    public TreeNode<V, C> computeWithVertex(V vertex, CalculateCost<V, C, E> functor) {
        TreeNode<V, C> rootTreeNode = computePartialGreedy(vertex, functor);
        
        C bestCost = rootTreeNode.node.getCost();
        ComputationResult<V, C> result;
        do {
            TreeNode<V, C> startNode;
            if (rootTreeNode.left.node.getVertex() == vertex) {
                startNode = rootTreeNode.right;
            } else {
                startNode = rootTreeNode.left;
            }

            result = new ComputationResult<>(bestCost, 0, null);
            traverseCut(startNode, startNode, result, functor);
            if (result.cutNode != null) {
                createTreeWithCut(result.cutNode, functor);
            }
        } while (result.cutNode != null);
        return rootTreeNode;
    }
    
    public static void drawTree(TreeNode node) {
        System.out.print("(");
        if (node.left == null) {
            System.out.print(node.node.getVertex().toString());
        } else {
            drawTree(node.left);
            drawTree(node.right);    
        }
        System.out.print(")");
    }
    
    private static class WeightedEdge<T> extends SimpleEdge<T> {
        int w;
        
        public WeightedEdge(T from, T to, int weight) {
            super(from, to);
            w = weight;
        }
    }
    
    public static void test() {
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
        }; 
        
        TreeNode<Integer, Integer> res = algo.compute(func);
        TreeNode<Integer, Integer> res2 = algo.computeDP(func);
        TreeNode<Integer, Integer> res3 = algo.computeWithVertex(num[4], func);  
        
        System.out.println("!!!!!!! " + res.node.getCost());
        drawTree(res);
        System.out.println();
        System.out.println("!!!!!!! " + res2.node.getCost());
        drawTree(res2);
        System.out.println();
        System.out.println("!!!!!!! " + res3.node.getCost());
        drawTree(res3);
        System.out.println();
    }
}
