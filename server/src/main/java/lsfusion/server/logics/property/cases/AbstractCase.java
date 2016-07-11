package lsfusion.server.logics.property.cases;

import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.SFunctionSet;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.sets.ResolveClassSet;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.linear.LAP;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.linear.LP;
import lsfusion.server.logics.mutables.Version;
import lsfusion.server.logics.mutables.interfaces.NFList;
import lsfusion.server.logics.property.*;
import lsfusion.server.logics.property.actions.flow.CaseActionProperty;
import lsfusion.server.logics.property.actions.flow.ListCaseActionProperty;
import lsfusion.server.logics.property.cases.graph.Comp;
import lsfusion.server.logics.property.cases.graph.CompProcessor;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.util.*;

public abstract class AbstractCase<P extends PropertyInterface, W extends CalcPropertyInterfaceImplement<P>, M extends PropertyInterfaceImplement<P>> {
    
    public final W where;    
    public final M implement;
    
    public final List<ResolveClassSet> signature;
    
    public AbstractCase(W where, M implement, List<ResolveClassSet> signature) {
        this.where = where;
        this.implement = implement;
        this.signature = signature;        
    }
    
    protected boolean isImplicit() {
        return false;
    }
    
    protected boolean getSameNamespace() {
        throw new UnsupportedOperationException();
    }


    private static <P extends PropertyInterface, W extends CalcPropertyInterfaceImplement<P>,
            M extends PropertyInterfaceImplement<P>, A extends AbstractCase<P, W, M>> List<ResolveClassSet> getSignature(A aCase) {
        return aCase.signature;
    }

    private static <P extends PropertyInterface, W extends CalcPropertyInterfaceImplement<P>,
            M extends PropertyInterfaceImplement<P>, A extends AbstractCase<P, W, M>> ClassWhere<P> getClasses(A aCase) {
        return ((CalcPropertyMapImplement<?, P>) aCase.where).mapClassWhere(ClassType.casePolicy);
    }

    private static <P extends PropertyInterface, W extends CalcPropertyInterfaceImplement<P>,
            M extends PropertyInterfaceImplement<P>, F extends Case<P, W, M>> ClassWhere<P> getClasses(F aCase) {
        return ((CalcPropertyMapImplement<?, P>) aCase.where).mapClassWhere(ClassType.casePolicy);
    }
    
    private interface AbstractWrapper<P extends PropertyInterface, W extends CalcPropertyInterfaceImplement<P>,
            M extends PropertyInterfaceImplement<P>, F extends Case<P, W, M>> {
        
        F proceedSet(ImSet<F> elements);

        F proceedList(ImList<F> elements);
    }
    
    public static <P extends PropertyInterface, W extends CalcPropertyInterfaceImplement<P>, 
            M extends PropertyInterfaceImplement<P>, F extends Case<P, W, M>, A extends AbstractCase<P, W, M>> FinalizeResult<F> finalizeCases(NFList<A> cases, GetValue<F, A> translator, final AbstractWrapper<P, W, M, F> wrapper, final GetValue<Graph<F>, M> abstractReader, boolean hasImplicit, boolean explicitExclusive) {
        ImList<A> list = cases.getList();
        if(!hasImplicit) {
            return new FinalizeResult<>(list.mapListValues(translator), explicitExclusive, null);
        }

        Comparator<A> priorityComparator = new Comparator<A>() {
            public int compare(A o1, A o2) {
                if(!o1.isImplicit() && o2.isImplicit())
                    return 1;
                if(!o2.isImplicit() && o1.isImplicit())
                    return -1;
                
                if(o1.isImplicit()) {
                    assert o2.isImplicit();
                    if(o1.getSameNamespace() && !o2.getSameNamespace())
                        return 1;
                    if(!o1.getSameNamespace() && o2.getSameNamespace())
                        return -1;
                }

                List<ResolveClassSet> signature1 = getSignature(o1);
                List<ResolveClassSet> signature2 = getSignature(o2);
                if(signature1 != null && signature2 != null) {
                    boolean match21 = match(signature2, signature1);
                    boolean match12 = match(signature1, signature2);
                    if (match21 && !match12)
                        return 1;
                    if (match12 && !match21)
                        return -1;
                }
                return 0;
            }
        };

        ImOrderSet<A> orderSet = list.toOrderExclSet();
        ImSet<A> set = orderSet.getSet();

        // priorityComparator транзитивный (но на 0 не обязательно, то есть не consistent with equals)
        Graph<A> pregraph = Graph.create(set, priorityComparator); // построение графа из компаратора

        // 0. преобразовываем граф в final + если node'ы совпадают, то если есть между ними ребро, оставляем исходящую вершину, иначе кидаем ambiguous identical
        Result<Pair<A, A>> ambiguous = new Result<>();
        Graph<F> graph = pregraph.translate(set.mapValues(translator), ambiguous);
        if(graph == null)
            throw new RuntimeException("Ambiguous identical implementation");
        
        // pre-3. собираем все abstract'ы, упорядочиваем по "возрастанию" использования, делаем это до очистки, чтобы их не потерять
        final ImMap<F, Graph<F>> abstractGraphs = graph.getNodes().mapValues(new GetValue<Graph<F>, F>() {
            public Graph<F> getMapValue(F value) {
                return abstractReader.getMapValue(value.implement);
            }
        }).removeNulls();
        ImOrderMap<F, Graph<F>> sortedAbstracts = abstractGraphs.sort(new Comparator<F>() {
            public int compare(F o1, F o2) {
                if(abstractGraphs.get(o2).contains(o1))
                    return 1;
                if(abstractGraphs.get(o1).contains(o2))
                    return -1;
                return 0;
            }
        });

        // 1. "очистка" графа - удаление избыточных вершин, or'я классы всех вершин которые следуют если полностью следует - убираем !!!
        Queue<F> queue = new ArrayDeque<>();
        Set<F> wasInQueue = new HashSet<>();
        Map<F, ClassWhere<P>> upClasses = new HashMap<>();

        for(F upNode : graph.getZeroInNodes()) {
            queue.add(upNode);
            upClasses.put(upNode, ClassWhere.<P>FALSE());
        }

        F element;
        while((element = queue.poll()) != null) {
            if(wasInQueue.add(element)) { // если не было
                ClassWhere<P> classesElement = getClasses(element);

                ClassWhere<P> upClassesElement = upClasses.get(element);

                boolean remove = false;
                if(classesElement.meansCompatible(upClassesElement)) { // если полностью покрывается верхними классами - выкидываем
                    remove = true;
                } 
                
                // запускаем поиск в ширину
                upClassesElement = upClassesElement.or(classesElement);
                for(F outElement : graph.getEdgesOut(element)) {
                    ClassWhere<P> outClasses = upClasses.get(outElement);
                    if(outClasses != null)
                        upClassesElement = upClassesElement.or(outClasses);
                    upClasses.put(outElement, upClassesElement);

                    queue.add(outElement);
                }

                if(remove)
                    graph = graph.remove(element);
            }
        }

        // pre-3. непосредственно inline (можно делать и до "очистки")
        for(int i=0,size=sortedAbstracts.size();i<size;i++) {
            F absNode = sortedAbstracts.getKey(i);
            Graph<F> abstractGraph = sortedAbstracts.getValue(i);

            if(graph.contains(absNode)) {
                for (F impNode : abstractGraph.getNodes()) {
                    if (graph.contains(impNode)) {
                        boolean impPrior = graph.depends(impNode, absNode); // если реализация приоритетнее, удаляем вершину из включаемого графа за ненадобностью
                        if (impPrior) { // если реализация уже есть и приоритетнее, удаляем вершину из включаемого графа за ненадобностью
                            abstractGraph = abstractGraph.remove(impNode); // можно сразу удалить 
                        } else {
                            if(!graph.depends(absNode, impNode))
                                absNode = absNode;
//                            assert graph.depends(absNode, impNode); // неверно если отключить ambiguos // из определения priorityComparator'а и abstract 
                            graph = graph.remove(impNode); // так как он сейчас будет inline'ся, assert что есть ??? - не факт из-за анонимных реализаций
                        }
                    }
                }

                graph = graph.inline(absNode, abstractGraph); // assert что не пересекается по node'ам
            }
        }

        // "очистка" ребер (важно после inline так как могут появится избыточные ребра)
        graph = graph.cleanNodes(new SFunctionSet<Pair<F, F>>() {
            public boolean contains(Pair<F, F> element) {
                return getClasses(element.first).andCompatible(getClasses(element.second)).isFalse();
            }});
        
        // 2. "проверка ambigious" - берем 2 реализации B и C, compare(B, C) == 0, берем все x такие что compare(X, B) > 0, и compare(X, C) > 0 объединяем их классы и смотрим что он включает B AND C
        List<F> nodes = new ArrayList<>(graph.getNodes().toJavaSet());
        for(int i = 0, size = nodes.size(); i < size; i++) {
            F a = nodes.get(i);
            for (int j = i + 1; j < size; j++) {
                F b = nodes.get(j);
                if(!graph.depends(a, b) && !graph.depends(b, a)) { // не связаны
                    ClassWhere<P> classesAB = getClasses(a).andCompatible(getClasses(b));
                    if(!classesAB.isFalse()) { // оптимизация
                        ClassWhere<P> priorityWhere = ClassWhere.FALSE();
                        for (int k = 0; k < size; k++) // можно edgesIn пересечь и по ним бежать
                            if (k != i && k != j) {
                                F c = nodes.get(k);
                                if (graph.depends(c, a) && graph.depends(c, b)) { // c приоритетнее и a и b
                                    priorityWhere = priorityWhere.or(getClasses(c));
                                }
                            }
                        if(!classesAB.meansCompatible(priorityWhere))
                            System.out.println("Ambiguous implementation"); //throw new RuntimeException("Ambiguous implementation");
                    }
                }
            }
        }
        
        // 3. "extract abstract" - поиск подграфа + сворачивание в вершину + проверка нерекурсивности
        for(int i=sortedAbstracts.size()-1;i>=0;i--) {
            F absNode = sortedAbstracts.getKey(i);
            Graph<F> abstractGraph = sortedAbstracts.getValue(i);
            
            graph = graph.extract(abstractGraph, absNode); // assert что absNode нет            
        }
        
        // 4. построение Set<List<Set...>>  - находим вершины с нулевой степенью вырезаем, разбиваем на компоненты связности, для детерминированности бежим в порядке list
        Comp<F> comp = graph.buildComps();
        Pair<ImList<F>, Boolean> compResult = comp.proceed(new CompProcessor<F, F, Pair<ImList<F>, Boolean>>() {
            public F proceedInnerNode(F element) {
                return element;
            }

            public F proceedInnerSet(ImSet<F> elements) {
                return wrapper.proceedSet(elements);
            }

            public F proceedInnerList(ImList<F> elements) {
                return wrapper.proceedList(elements);
            }

            public Pair<ImList<F>, Boolean> proceedNode(F element) {
                return new Pair<>(ListFact.singleton(element), true);
            }

            public Pair<ImList<F>, Boolean> proceedSet(ImSet<F> elements) {
                return new Pair<>(elements.toList(), true);
            }

            public Pair<ImList<F>, Boolean> proceedList(ImList<F> elements) {
                return new Pair<>(elements, false);
            }
        });
        
        return new FinalizeResult<>(compResult.first, compResult.second, graph);
    }

    private static <P extends PropertyInterface> int compare(ClassWhere<P> where1, ClassWhere<P> where2) {
        boolean means1 = where1.meansCompatible(where2);
        boolean means2 = where2.meansCompatible(where1);
        if(means1 && !means2)
            return 1;
        if(means2 && !means1)
            return -1;

        return 0;
    }

    private static <P extends PropertyInterface, W extends CalcPropertyInterfaceImplement<P>,
            M extends PropertyInterfaceImplement<P>, F extends Case<P, W, M>> CalcPropertyMapImplement<?, P> createUnionWhere(ImSet<P> interfaces, ImList<F> aCase, boolean isExclusive) {
        
        // собираем where и делаем их or
        return DerivedProperty.createUnion(interfaces, aCase.mapListValues(new GetValue<W, F>() {
            public W getMapValue(F value) {
                return value.where;
            }
        }), isExclusive);
    }
    
    private static <P extends PropertyInterface> ActionCase<P> createInnerActionCase(ImSet<P> interfaces, ImList<ActionCase<P>> cases, boolean isExclusive) {
        return new ActionCase<>(createUnionWhere(interfaces, cases, isExclusive), DerivedProperty.createCaseAction(interfaces, isExclusive, cases));
    }

    private static <P extends PropertyInterface> CalcCase<P> createInnerCalcCase(ImSet<P> interfaces, ImList<CalcCase<P>> cases, boolean isExclusive) {
        return new CalcCase<>(createUnionWhere(interfaces, cases, isExclusive), DerivedProperty.createUnion(interfaces, isExclusive, cases));
    }
    
    public static <P extends PropertyInterface> FinalizeResult<ActionCase<P>> finalizeActionCases(final ImSet<P> interfaces, NFList<AbstractActionCase<P>> cases, boolean hasImplicit, boolean explicitExclusiveness) {
        return finalizeCases(cases, new GetValue<ActionCase<P>, AbstractActionCase<P>>() {
            public ActionCase<P> getMapValue(AbstractActionCase<P> value) {
                return new ActionCase<>(value);
            }
        }, new AbstractWrapper<P, CalcPropertyMapImplement<?, P>, ActionPropertyMapImplement<?, P>, ActionCase<P>>() {
            public ActionCase<P> proceedSet(ImSet<ActionCase<P>> elements) {
                return createInnerActionCase(interfaces, elements.toList(), true);
            }

            public ActionCase<P> proceedList(ImList<ActionCase<P>> elements) {
                return createInnerActionCase(interfaces, elements, false);
            }
        }, new GetValue<Graph<ActionCase<P>>, ActionPropertyMapImplement<?, P>>() {
            public Graph<ActionCase<P>> getMapValue(ActionPropertyMapImplement<?, P> value) {
                return value.mapAbstractGraph();
            }
        }, hasImplicit, explicitExclusiveness);
    }

    public static <P extends PropertyInterface> FinalizeResult<CalcCase<P>> finalizeCalcCases(final ImSet<P> interfaces, NFList<AbstractCalcCase<P>> cases, boolean hasImplicit, boolean explicitExclusiveness) {
        return finalizeCases(cases, new GetValue<CalcCase<P>, AbstractCalcCase<P>>() {
            public CalcCase<P> getMapValue(AbstractCalcCase<P> value) {
                return new CalcCase<>(value);
            }
        }, new AbstractWrapper<P, CalcPropertyInterfaceImplement<P>, CalcPropertyInterfaceImplement<P>, CalcCase<P>>() {
            public CalcCase<P> proceedSet(ImSet<CalcCase<P>> elements) {
                return createInnerCalcCase(interfaces, elements.toList(), true);
            }

            public CalcCase<P> proceedList(ImList<CalcCase<P>> elements) {
                return createInnerCalcCase(interfaces, elements, false);
            }
        }, new GetValue<Graph<CalcCase<P>>, CalcPropertyInterfaceImplement<P>>() {
            public Graph<CalcCase<P>> getMapValue(CalcPropertyInterfaceImplement<P> value) {
                return value.mapAbstractGraph();
            }
        }, hasImplicit, explicitExclusiveness);
    }

    // оптимизация
    public static <P extends PropertyInterface, BP extends Property<P>, L extends LP<P, BP>, AP extends BP> boolean preFillImplicitCases(L lp) {
        if(lp instanceof LCP)
            return ((LCP) lp).property instanceof CaseUnionProperty && ((CaseUnionProperty)((LCP) lp).property).isAbstract() && ((CaseUnionProperty)((LCP) lp).property).getAbstractType() == CaseUnionProperty.Type.MULTI;
        else
            return ((LAP) lp).property instanceof CaseActionProperty && ((CaseActionProperty)((LAP) lp).property).isAbstract() && ((CaseActionProperty)((LAP) lp).property).getAbstractType() == ListCaseActionProperty.AbstractType.MULTI;
    }
    
    private static boolean match(List<ResolveClassSet> absSignature, List<ResolveClassSet> concSignature) {
        return LogicsModule.match(absSignature, concSignature, false, true);        
    }

//    public static int cntfnd = 0;
//    public static int cntfndsame = 0;
//    public static int cntdecl = 0;
//    public static int cntnotfnd = 0;
//    public static int cntnotfndsame = 0;

//    public static int cntexpl = 0;
//    public static int cntexplname = 0;

//    public static int cntsame = 0;
    
    //    public static <P extends PropertyInterface, BP extends Property<P>, L extends LP<P, BP>, AP extends BP> 
    public static <I extends PropertyInterface> void fillImplicitCases(LP absLP, LP impLP, List<ResolveClassSet> absSignature, List<ResolveClassSet> impSignature, boolean sameNamespace, Version impVersion) {        
        assert preFillImplicitCases(absLP);

        if (absLP == impLP)
            return;
        
        if(!match(absSignature, impSignature))
            return;
        
        // слишком много функциональщины если делать по аналогии с finalize по сравнению с количеством кода        
        if(absLP instanceof LCP) {
            LCP<UnionProperty.Interface> absLCP = (LCP) absLP;
            if (impLP instanceof LCP) {
                LCP<I> impLCP = (LCP) impLP;
                
//                CaseUnionProperty caseProp = (CaseUnionProperty) absLCP.property;
//                boolean found = hasProp(impLCP, caseProp);
//                
//                if(!sameNamespace) {
//                    if (found)
//                        cntfnd = cntfnd + 1;
//                    else if (absLCP.property.toString().contains("DataSkuLedger") || absLCP.property.toString().contains("description"))
//                        cntdecl = cntdecl + 1;
//                    else {
//                        System.out.println("NOTSAME : " + absLCP.property + " - " + impLCP.property);
//                        cntnotfnd = cntnotfnd + 1;
//                    }
//                } else {
//                    if (found) {
//                        cntfndsame = cntfndsame + 1;
//                    } else {
//                        System.out.println("SAME : " + absLCP.property + " - " + impLCP.property);
//                        cntnotfndsame = cntnotfndsame + 1;
//                    }
//                }
                
                CalcPropertyMapImplement<I, UnionProperty.Interface> mapAbsImp = impLCP.getImplement(absLCP.listInterfaces.toArray(new UnionProperty.Interface[absLCP.listInterfaces.size()]));
                ((CaseUnionProperty) absLCP.property).addImplicitCase(mapAbsImp, impSignature, sameNamespace, impVersion);
            }
        } else {
            LAP<PropertyInterface> absLAP = (LAP) absLP;
            if (impLP instanceof LAP) {
                LAP<I> impLAP = (LAP) impLP;
                ActionPropertyMapImplement<I, PropertyInterface> mapAbsImp = impLAP.getImplement(absLAP.listInterfaces.toArray(new PropertyInterface[absLAP.listInterfaces.size()]));
                ((CaseActionProperty) absLAP.property).addImplicitCase(mapAbsImp, impSignature, sameNamespace, impVersion);
            }
        }
    }

//    private static <I extends PropertyInterface> boolean hasProp(LCP<I> impLCP, CaseUnionProperty caseProp) {
//        ImList<ExplicitCalcCase<UnionProperty.Interface>> tstCases = caseProp.getTestCases();
//        if(tstCases != null) {
//            for (ExplicitCalcCase<UnionProperty.Interface> explCase : tstCases) {
//                if (explCase.implement instanceof CalcPropertyMapImplement) {
//                    if (((CalcPropertyMapImplement) explCase.implement).property.equals(impLCP.property))
//                        return true;
//
//                    if (((CalcPropertyMapImplement) explCase.implement).property instanceof CaseUnionProperty && hasProp(impLCP, (CaseUnionProperty) ((CalcPropertyMapImplement) explCase.implement).property))
//                        return true;
//                }
//            }
//        } else {
//            for (CalcCase<UnionProperty.Interface> explCase : caseProp.getCases()) {
//                if (explCase.implement instanceof CalcPropertyMapImplement) {
//                    if (((CalcPropertyMapImplement) explCase.implement).property.equals(impLCP.property))
//                        return true;
//
//                    if (((CalcPropertyMapImplement) explCase.implement).property instanceof CaseUnionProperty && hasProp(impLCP, (CaseUnionProperty) ((CalcPropertyMapImplement) explCase.implement).property))
//                        return true;
//                }
//            }
//        }
//        
//        return false;
//    }


    public String toString() {
        return where + " -> " + implement;
    }
}
