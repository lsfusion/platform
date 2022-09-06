package lsfusion.server.logics.property.cases;

import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.server.base.version.Version;
import lsfusion.server.base.version.interfaces.NFList;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.language.action.LA;
import lsfusion.server.language.property.LP;
import lsfusion.server.language.property.oraction.LAP;
import lsfusion.server.logics.action.flow.CaseAction;
import lsfusion.server.logics.action.flow.ListCaseAction;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.classes.user.set.ResolveClassSet;
import lsfusion.server.logics.property.PropertyFact;
import lsfusion.server.logics.property.UnionProperty;
import lsfusion.server.logics.property.cases.graph.Comp;
import lsfusion.server.logics.property.cases.graph.CompProcessor;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.classes.infer.ClassType;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.logics.property.oraction.ActionOrPropertyInterfaceImplement;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.id.resolve.SignatureMatcher;

import java.util.*;
import java.util.function.Function;

public abstract class AbstractCase<P extends PropertyInterface, W extends PropertyInterfaceImplement<P>, M extends ActionOrPropertyInterfaceImplement> {
    
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


    private static <P extends PropertyInterface, W extends PropertyInterfaceImplement<P>,
            M extends ActionOrPropertyInterfaceImplement, A extends AbstractCase<P, W, M>> List<ResolveClassSet> getSignature(A aCase) {
        return aCase.signature;
    }

    private static <P extends PropertyInterface, W extends PropertyInterfaceImplement<P>,
            M extends ActionOrPropertyInterfaceImplement, A extends AbstractCase<P, W, M>> ClassWhere<P> getClasses(A aCase) {
        return ((PropertyMapImplement<?, P>) aCase.where).mapClassWhere(ClassType.casePolicy);
    }

    private static <P extends PropertyInterface, W extends PropertyInterfaceImplement<P>,
            M extends ActionOrPropertyInterfaceImplement, F extends Case<P, W, M>> ClassWhere<P> getClasses(F aCase) {
        return ((PropertyMapImplement<?, P>) aCase.where).mapClassWhere(ClassType.casePolicy);
    }
    
    private interface AbstractWrapper<P extends PropertyInterface, W extends PropertyInterfaceImplement<P>,
            M extends ActionOrPropertyInterfaceImplement, F extends Case<P, W, M>> {
        
        F proceedSet(ImSet<F> elements);

        F proceedList(ImList<F> elements);
    }
    
    public static <P extends PropertyInterface, W extends PropertyInterfaceImplement<P>, 
            M extends ActionOrPropertyInterfaceImplement, F extends Case<P, W, M>, A extends AbstractCase<P, W, M>> FinalizeResult<F> finalizeCases(NFList<A> cases, Function<A, F> translator, final AbstractWrapper<P, W, M, F> wrapper, final Function<M, Graph<F>> abstractReader, boolean areClassCases, boolean explicitExclusive) {
        ImList<A> list = cases.getList();
        if(!areClassCases || explicitExclusive) { // если не делать explicitExclusive вместо ошибки, начинает работать как если бы exclusive'а не было и платформа сама бы выбирала (впрочем обратная ветка уже работает стабильно)
            return new FinalizeResult<>(list.mapListValues(translator), explicitExclusive, null);
        }

        Comparator<A> priorityComparator = (o1, o2) -> {
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
                if(match21) {
                    assert match12;
                    return -Integer.compare(list.indexOf(o1), list.indexOf(o2));
                }
            }
            return Integer.compare(list.indexOf(o2), list.indexOf(o1));
        };

        ImOrderSet<A> orderSet = list.toOrderSet(); // может повторяться implementation
        ImSet<A> set = orderSet.getSet();

        // priorityComparator транзитивный (но на 0 не обязательно, то есть не consistent with equals)
        Graph<A> pregraph = Graph.create(set, priorityComparator); // построение графа из компаратора

        // 0. преобразовываем граф в final + если node'ы совпадают, то если есть между ними ребро, оставляем исходящую вершину, иначе кидаем ambiguous identical
        Result<Pair<A, A>> ambiguous = new Result<>();
        Graph<F> graph = pregraph.translate(set.mapValues(translator), ambiguous);
        if(graph == null)
            throw new RuntimeException("Ambiguous identical implementation " + ambiguous.result);
        
        // pre-3. собираем все abstract'ы, упорядочиваем по "возрастанию" использования, делаем это до очистки, чтобы их не потерять
        final ImMap<F, Graph<F>> abstractGraphs = graph.getNodes().mapValues((Function<F, Graph<F>>) value -> abstractReader.apply(value.implement)).removeNulls();
        ImOrderMap<F, Graph<F>> sortedAbstracts = abstractGraphs.sort((o1, o2) -> {
            if(abstractGraphs.get(o2).contains(o1))
                return 1;
            if(abstractGraphs.get(o1).contains(o2))
                return -1;
            return 0;
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
        graph = graph.cleanNodes(element1 -> getClasses(element1.first).andCompatible(getClasses(element1.second)).isFalse());
        
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

    private static <P extends PropertyInterface, W extends PropertyInterfaceImplement<P>,
            M extends ActionOrPropertyInterfaceImplement, F extends Case<P, W, M>> PropertyMapImplement<?, P> createUnionWhere(ImSet<P> interfaces, ImList<F> aCase, boolean isExclusive) {
        
        // собираем where и делаем их or
        return PropertyFact.createUnion(interfaces, aCase.mapListValues((F value) -> value.where), isExclusive);
    }
    
    private static <P extends PropertyInterface> ActionCase<P> createInnerActionCase(ImSet<P> interfaces, ImList<ActionCase<P>> cases, boolean isExclusive) {
        return new ActionCase<>(createUnionWhere(interfaces, cases, isExclusive), PropertyFact.createCaseAction(interfaces, isExclusive, cases));
    }

    private static <P extends PropertyInterface> CalcCase<P> createInnerCalcCase(ImSet<P> interfaces, ImList<CalcCase<P>> cases, boolean isExclusive) {
        return new CalcCase<>(createUnionWhere(interfaces, cases, isExclusive), PropertyFact.createUnion(interfaces, isExclusive, cases));
    }
    
    public static <P extends PropertyInterface> FinalizeResult<ActionCase<P>> finalizeActionCases(final ImSet<P> interfaces, NFList<AbstractActionCase<P>> cases, boolean areClassCases, boolean explicitExclusiveness) {
        return finalizeCases(cases, ActionCase::new, new AbstractWrapper<P, PropertyInterfaceImplement<P>, ActionMapImplement<?, P>, ActionCase<P>>() {
            public ActionCase<P> proceedSet(ImSet<ActionCase<P>> elements) {
                return createInnerActionCase(interfaces, elements.toList(), true);
            }

            public ActionCase<P> proceedList(ImList<ActionCase<P>> elements) {
                return createInnerActionCase(interfaces, elements, false);
            }
        }, ActionMapImplement::mapAbstractGraph, areClassCases, explicitExclusiveness);
    }

    public static <P extends PropertyInterface> FinalizeResult<CalcCase<P>> finalizeCalcCases(final ImSet<P> interfaces, NFList<AbstractCalcCase<P>> cases, boolean areClassCases, boolean explicitExclusiveness) {
        return finalizeCases(cases, CalcCase::new, new AbstractWrapper<P, PropertyInterfaceImplement<P>, PropertyInterfaceImplement<P>, CalcCase<P>>() {
            public CalcCase<P> proceedSet(ImSet<CalcCase<P>> elements) {
                return createInnerCalcCase(interfaces, elements.toList(), true);
            }

            public CalcCase<P> proceedList(ImList<CalcCase<P>> elements) {
                return createInnerCalcCase(interfaces, elements, false);
            }
        }, PropertyInterfaceImplement::mapAbstractGraph, areClassCases, explicitExclusiveness);
    }

    // оптимизация
    public static <P extends PropertyInterface, BP extends ActionOrProperty<P>, L extends LAP<P, BP>, AP extends BP> boolean preFillImplicitCases(L lp) {
        if(lp instanceof LP)
            return ((LP) lp).property instanceof CaseUnionProperty && ((CaseUnionProperty)((LP) lp).property).isAbstract() && ((CaseUnionProperty)((LP) lp).property).getAbstractType() == CaseUnionProperty.Type.MULTI;
        else
            return ((LA) lp).action instanceof CaseAction && ((CaseAction)((LA) lp).action).isAbstract() && ((CaseAction)((LA) lp).action).getAbstractType() == ListCaseAction.AbstractType.MULTI;
    }
    
    private static boolean match(List<ResolveClassSet> absSignature, List<ResolveClassSet> concSignature) {
        return SignatureMatcher.isCompatible(absSignature, concSignature, false, true);        
    }

//    public static int cntfnd = 0;
//    public static int cntfndsame = 0;
//    public static int cntdecl = 0;
//    public static int cntnotfnd = 0;
//    public static int cntnotfndsame = 0;

//    public static int cntexpl = 0;
//    public static int cntexplname = 0;

//    public static int cntsame = 0;
    
    //    public static <P extends PropertyInterface, BP extends Property<P>, L extends LAP<P, BP>, AP extends BP> 
    public static <I extends PropertyInterface> void fillImplicitCases(LAP absLP, LAP impLP, List<ResolveClassSet> absSignature, List<ResolveClassSet> impSignature, boolean sameNamespace, Version impVersion) {        
        assert preFillImplicitCases(absLP);

        if (absLP == impLP)
            return;
        
        if(!match(absSignature, impSignature))
            return;
        
        // слишком много функциональщины если делать по аналогии с finalize по сравнению с количеством кода        
        if(absLP instanceof LP) {
            LP<UnionProperty.Interface> absLCP = (LP) absLP;
            if (impLP instanceof LP) {
                LP<I> impLCP = (LP) impLP;
                
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
                
                PropertyMapImplement<I, UnionProperty.Interface> mapAbsImp = impLCP.getImplement(absLCP.listInterfaces.toArray(new UnionProperty.Interface[absLCP.listInterfaces.size()]));
                ((CaseUnionProperty) absLCP.property).addImplicitCase(mapAbsImp, impSignature, sameNamespace, impVersion);
            }
        } else {
            LA<PropertyInterface> absLA = (LA) absLP;
            if (impLP instanceof LA) {
                LA<I> impLA = (LA) impLP;
                ActionMapImplement<I, PropertyInterface> mapAbsImp = impLA.getImplement(absLA.listInterfaces.toArray(new PropertyInterface[absLA.listInterfaces.size()]));
                ((CaseAction) absLA.action).addImplicitCase(mapAbsImp, impSignature, sameNamespace, impVersion);
            }
        }
    }

//    private static <I extends PropertyInterface> boolean hasProp(LP<I> impLCP, CaseUnionProperty caseProp) {
//        ImList<ExplicitCalcCase<UnionProperty.Interface>> tstCases = caseProp.getTestCases();
//        if(tstCases != null) {
//            for (ExplicitCalcCase<UnionProperty.Interface> explCase : tstCases) {
//                if (explCase.implement instanceof PropertyMapImplement) {
//                    if (((PropertyMapImplement) explCase.implement).property.equals(impLCP.property))
//                        return true;
//
//                    if (((PropertyMapImplement) explCase.implement).property instanceof CaseUnionProperty && hasProp(impLCP, (CaseUnionProperty) ((PropertyMapImplement) explCase.implement).property))
//                        return true;
//                }
//            }
//        } else {
//            for (CalcCase<UnionProperty.Interface> explCase : caseProp.getCases()) {
//                if (explCase.implement instanceof PropertyMapImplement) {
//                    if (((PropertyMapImplement) explCase.implement).property.equals(impLCP.property))
//                        return true;
//
//                    if (((PropertyMapImplement) explCase.implement).property instanceof CaseUnionProperty && hasProp(impLCP, (CaseUnionProperty) ((PropertyMapImplement) explCase.implement).property))
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
