package lsfusion.server.logics.property.cases.graph;

import lsfusion.base.*;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.*;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.ImValueMap;

import java.util.Comparator;

// assert что граф всегда полный
public class Graph<T> {
    
    private final ImMap<T, ImSet<T>> edgesOut;
    private final ImMap<T, ImSet<T>> edgesIn;

    // также assert что граф полный (!) то есть все вершины до которых можно дойти (в том числе сами эти вершины) 
    public Graph(ImMap<T, ImSet<T>> edgesOut, ImMap<T, ImSet<T>> edgesIn) {
        this.edgesOut = edgesOut;
        this.edgesIn = edgesIn;
        
        assert checkGraph();
    }

    public static <T> ImMap<T, ImSet<T>> buildEdgesOut(ImMap<T, ImSet<T>> edgesIn) {
        MExclMap<T, MSet<T>> mResult = MapFact.mExclMap();

        for(T prop : edgesIn.keyIt())
            mResult.exclAdd(prop, SetFact.<T>mSet());

        for(int i=0,size=edgesIn.size();i<size;i++) {
            T element = edgesIn.getKey(i);
            for(T elementIn : edgesIn.getValue(i))
                mResult.get(elementIn).add(element);
        }

        return MapFact.immutableMap(mResult);
    }

    public Graph(ImMap<T, ImSet<T>> edgesIn) {
        this(buildEdgesOut(edgesIn), edgesIn);
    }

    private static <T> void recFindEdges(T element, boolean includeThis, ImMap<T, ImSet<T>> edges, ImSet<T> filteredElements, MSet<T> mResult, MAddSet<T> proceeded) {
        if(includeThis && filteredElements.contains(element))
            mResult.add(element);
        else {
            if(proceeded.add(element))
                return;
            for(T out : edges.get(element))
                recFindEdges(out, true, edges, filteredElements, mResult, proceeded);
        }
    }

    private static <T> ImMap<T, ImSet<T>> filter(final ImMap<T, ImSet<T>> edges, FunctionSet<T> filter) {
        final ImSet<T> filteredEdges = edges.keys().filterFn(filter);
        return filteredEdges.mapValues(new GetValue<ImSet<T>, T>() {
            public ImSet<T> getMapValue(T element) {
                MAddSet<T> proceeded = SetFact.mAddSet();
                MSet<T> mResult = SetFact.mSet();
                recFindEdges(element, false, edges, filteredEdges, mResult, proceeded);
                return mResult.immutable();
            }
        });
    }

    public Graph<T> filterGraph(FunctionSet<T> filter)  {
        return new Graph<>(filter(edgesOut, filter), filter(edgesIn, filter));
    }

    private boolean checkGraph() {
        if(!edgesIn.keys().equals(edgesOut.keys()))
            return false;
        
        for(int i=0,size=edgesIn.size();i<size;i++)
            if(!getNodes().containsAll(edgesIn.getValue(i)))
                return false;

        for(int i=0,size=edgesOut.size();i<size;i++)
            if(!getNodes().containsAll(edgesOut.getValue(i)))
                return false;

        return true;
    }

    // построем алгоритм на set \ map'ах не самый быстрый вариант, но 

    public Graph<T> extract(Graph<T> graph, final T element) { // если возникла рекурсия ничего не делать, assert что element'а нет в графе
        assert !getNodes().contains(element);
                
        final ImSet<T> graphNodes = graph.getNodes();

        GetValue<ImSet<T>, ImSet<T>> substitute = new GetValue<ImSet<T>, ImSet<T>>() {
            public ImSet<T> getMapValue(ImSet<T> value) {
                ImSet<T> removed = value.remove(graphNodes);
                if (removed.size() < value.size())
                    removed = removed.addExcl(element);
                return removed;
            }};
        ImMap<T, ImSet<T>> removedEdgesOut = edgesOut.remove(graphNodes).mapValues(substitute);
        if(edgesOut.size() - removedEdgesOut.size() != graphNodes.size()) { // есть дополнительные вершины
            return this;            
        }
        ImMap<T, ImSet<T>> removedEdgesIn = edgesIn.remove(graphNodes).mapValues(substitute);

        ImSet<T> collapsedOutEdges = SetFact.EMPTY();
        ImSet<T> collapsedInEdges = SetFact.EMPTY();

        for(T node : graphNodes) {
            ImSet<T> outEdges = edgesOut.get(node);
            ImSet<T> inEdges = edgesIn.get(node);
            assert outEdges != null && inEdges != null;

            ImSet<T> exOutEdges = graph.edgesOut.get(node);
            ImSet<T> exInEdges = graph.edgesIn.get(node);

            ImSet<T> externalOutEdges = outEdges.remove(exOutEdges);
            if(outEdges.size() - externalOutEdges.size() != exOutEdges.size()) { // есть дополнительные ребра
                return this;                
            }
            ImSet<T> externalInEdges = inEdges.remove(exInEdges);
            
            collapsedOutEdges = collapsedOutEdges.merge(externalOutEdges);
            collapsedInEdges = collapsedInEdges.merge(externalInEdges);
        }
        
        // нужно проверить что полученный граф не рекурсивный можно ли из out пройти в in
        if(collapsedInEdges.intersect(collapsedOutEdges))
            return this;

        return new Graph<>(removedEdgesOut.addExcl(element, collapsedOutEdges.addExcl(element)), // itself 
                removedEdgesIn.addExcl(element, collapsedInEdges.addExcl(element))); // itself
    }
    
    private ImCol<Graph<T>> split() {
        MCol<Graph<T>> mResult = ListFact.mColMax(edgesOut.size());
        MAddSet<T> proceededNodes = SetFact.mAddSet();
        ImSet<T> zeroInNodes = getZeroInNodes();
        for(T zeroInNode : zeroInNodes) {
            if(!proceededNodes.contains(zeroInNode)) {
                Graph<T> compGraph = getCompGraph(zeroInNode);
                mResult.add(compGraph);
                proceededNodes.addAll(compGraph.getNodes());
            }
        }
        return mResult.immutableCol();
    }

    public ImSet<T> getEdgesOut(T element) {
        return edgesOut.get(element);
    }

    private Graph<T> getCompGraph(T node) {
        ImSet<T> compNodes = getCompNodes(node);
        return new Graph<>(edgesOut.filterIncl(compNodes), edgesIn.filterIncl(compNodes));
    }

    private ImSet<T> getCompNodes(T node) {
        MSet<T> mCompNodes = SetFact.mSet(); // itself, иначе нужно еще и сами элементы включать
        for(T edge : edgesOut.get(node)) {
            mCompNodes.addAll(edgesIn.get(edge));
        }
        return mCompNodes.immutable();
    }

    private Graph<T> removeNodes(final ImSet<T> set) {
        return new Graph<>(edgesOut.remove(set).mapValues(new GetValue<ImSet<T>, ImSet<T>>() {
            public ImSet<T> getMapValue(ImSet<T> value) {
                return value.remove(set);
            }
        }), edgesIn.remove(set).mapValues(new GetValue<ImSet<T>, ImSet<T>>() {
            public ImSet<T> getMapValue(ImSet<T> value) {
                return value.remove(set);
            }
        }));
    }

    // находим вершины с нулевой степенью вырезаем, разбиваем на компоненты связности, (list'ы сливаем) для детерминированности бежим в исходном порядке ??? 
    public Comp<T> buildComps() {
        MExclSet<NodeListComp<T>> mResult = SetFact.mExclSet(); 
                
        ImCol<Graph<T>> graphComps = split();
        for(Graph<T> graphComp : graphComps) {
            ImSet<T> zeroNodes = graphComp.getZeroOutNodes();
            Comp<T> recGraphComp = graphComp.removeNodes(zeroNodes).buildComps();

            ImList<NodeSetComp<T>> graphResult = ListFact.add(zeroNodes.mapSetValues(new GetValue<NodeComp<T>, T>() {
                public NodeComp<T> getMapValue(T value) {
                    return new NodeComp<>(value);
                }
            }).toList(), recGraphComp.getList());
            mResult.exclAddAll(ListComp.create(graphResult).getSet());
        }
        
        return SetComp.create(mResult.immutable());
    }
    
    public Graph<T> inline(final T element, Graph<T> graph) { // assert что graph'а нет в графе
        final ImSet<T> graphNodes = graph.getNodes();

        assert !getNodes().intersect(graphNodes) && getNodes().contains(element);

        final ImSet<T> elementOut = edgesOut.get(element).removeIncl(element); // itself
        final ImSet<T> elementIn = edgesIn.get(element).removeIncl(element); // itself

        GetValue<ImSet<T>, ImSet<T>> substitute = new GetValue<ImSet<T>, ImSet<T>>() {
            public ImSet<T> getMapValue(ImSet<T> value) {
                ImSet<T> removed = value.remove(SetFact.singleton(element));
                if(removed.size() < value.size())
                    removed = removed.addExcl(graphNodes);
                return removed;
            }
        };
        return new Graph<>(edgesOut.removeIncl(element).mapValues(substitute).addExcl(graph.edgesOut.mapValues(new GetValue<ImSet<T>, ImSet<T>>() {
            public ImSet<T> getMapValue(ImSet<T> value) {
                return value.addExcl(elementOut);
            }
        })), edgesIn.removeIncl(element).mapValues(substitute).addExcl(graph.edgesIn.mapValues(new GetValue<ImSet<T>, ImSet<T>>() {
            public ImSet<T> getMapValue(ImSet<T> value) {
                return value.addExcl(elementIn);
            }
        })));
    }
    
    public Graph<T> cleanNodes(final FunctionSet<Pair<T, T>> set) {
        GetKeyValue<ImSet<T>, T, ImSet<T>> adjustSet = new GetKeyValue<ImSet<T>, T, ImSet<T>>() {
            public ImSet<T> getMapValue(final T key, ImSet<T> value) {
                return value.filterFn(new SFunctionSet<T>() {
                    public boolean contains(T element) {
                        return !set.contains(new Pair<>(key, element));
                    }
                });
            }};
        return new Graph<>(edgesOut.mapValues(adjustSet), edgesIn.mapValues(adjustSet));
    }

    public static <T> Graph<T> create(final ImSet<T> set, final Comparator<T> comparator) {
        return new Graph<>(set.mapValues(new GetValue<ImSet<T>, T>() {
            public ImSet<T> getMapValue(final T value) {
                return set.filterFn(new SFunctionSet<T>() {
                    public boolean contains(T element) {
                        return value == element || comparator.compare(value, element) > 0; // itself
                    }
                });
            }
        }), set.mapValues(new GetValue<ImSet<T>, T>() {
            public ImSet<T> getMapValue(final T value) {
                return set.filterFn(new SFunctionSet<T>() {
                    public boolean contains(T element) {
                        return value == element || comparator.compare(value, element) < 0; // itself
                    }
                });
            }
        }));
        
    }

    public ImSet<T> getZeroInNodes() {
        return edgesIn.filterFnValues(new SFunctionSet<ImSet<T>>() {
            public boolean contains(ImSet<T> element) {
                return element.size() == 1; // itself
            }}).keys();
    }
    public ImSet<T> getZeroOutNodes() {
        return edgesOut.filterFnValues(new SFunctionSet<ImSet<T>>() {
            public boolean contains(ImSet<T> element) {
                return element.size() == 1; // itself
            }}).keys();
    }
    public ImSet<T> getNodes() {
        return edgesOut.keys();
    }
    
    public boolean depends(T a, T b) {
        return edgesOut.get(a).contains(b);
    }
    
    public Graph<T> remove(T element) {
        return removeNodes(SetFact.singleton(element));
    }
    
    public boolean contains(T element) {
        return edgesOut.containsKey(element);
    }

    // в отличии от map'а не reversable, и предполагается что надо оставлять элемент из которого можно дойти до остальных иначе кидать ambigious
    // вообще говоря кривовато, но тут стечение многих факторов
    public <F> Graph<F> translate(ImMap<T, F> mapNodes, Result<Pair<T, T>> ambiguous) {
        ImMap<F, ImSet<T>> group = mapNodes.groupValues();
        ImSet<T> removeNodes = SetFact.EMPTY();
        ImValueMap<F, T> mapGroup = group.mapItValues();
        for(int i=0,size=group.size();i<size;i++) {
            ImSet<T> nodes = group.getValue(i);
            T singleNode = null;
            for(T node : nodes) {
                boolean found = true;
                for(T sibling : nodes) {
                    if(!depends(node, sibling)) {
                        if(!depends(sibling, node)) {
                            ambiguous.set(new Pair<>(node, sibling));
                            return null;
                        }
                        found = false;
                        break;
                    }
                }             
                if(found) {
                    singleNode = node;
                    break;
                }
            }
            assert singleNode != null; // так как иначе ambigious был бы
            mapGroup.mapValue(i, singleNode);
            removeNodes = removeNodes.addExcl(nodes.removeIncl(singleNode));
        }
        ImRevMap<F, T> map = mapGroup.immutableValue().toRevExclMap();

        return removeNodes(removeNodes).map(map.reverse().fnGetValue());
    }

    public <M> Graph<M> map(final GetValue<M, T> map) { // rev map
        GetValue<ImSet<M>, ImSet<T>> mapSets = new GetValue<ImSet<M>, ImSet<T>>() {
            public ImSet<M> getMapValue(ImSet<T> value) {
                return value.mapSetValues(map);
            }};
        return new Graph<>(edgesOut.mapKeyValues(map, mapSets), edgesIn.mapKeyValues(map, mapSets));
    }
}
