package platform.server.caches;

import platform.base.EmptyIterator;
import platform.base.Pairs;
import platform.server.classes.ConcreteClass;
import platform.server.data.expr.ValueExpr;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.translator.MapValuesTranslator;

import java.util.*;

public class ValuePairs implements Iterable<MapValuesTranslate> {
    private final Set<ValueExpr> values1;
    private final Set<ValueExpr> values2;

    public ValuePairs(Set<ValueExpr> values1, Set<ValueExpr> values2) {
        this.values1 = values1;
        this.values2 = values2;
    }

    private static Map<ConcreteClass, Set<ValueExpr>> groupClasses(Set<ValueExpr> exprs) {
        Map<ConcreteClass, Set<ValueExpr>> result = new HashMap<ConcreteClass, Set<ValueExpr>>();
        for(ValueExpr expr : exprs) {
            Set<ValueExpr> classSet = result.get(expr.objectClass);
            if(classSet==null) {
                classSet = new HashSet<ValueExpr>();
                result.put(expr.objectClass, classSet);
            }
            classSet.add(expr);
        }
        return result;
    }

    private class ClassIterator implements Iterator<MapValuesTranslate> {

        final Set<ValueExpr>[] group1;
        final Set<ValueExpr>[] group2;

        private ClassIterator(Set<ValueExpr>[] group1, Set<ValueExpr>[] group2) {
            this.group1 = group1;
            this.group2 = group2;

            iterators = new Iterator[group1.length];
            iterations = new Map[group1.length];
        }

        boolean first = true;

        public boolean hasNext() {
            if(first)
                return true;

            for(Iterator<Map<ValueExpr, ValueExpr>> iterator : iterators)
                if(iterator.hasNext())
                    return true;
            return false;
        }

        Iterator<Map<ValueExpr,ValueExpr>>[] iterators;
        Map<ValueExpr,ValueExpr>[] iterations;

        public MapValuesTranslate next() {
            for(int i=0;i<group1.length;i++) {
                if(!first && iterators[i].hasNext()) {
                    iterations[i] = iterators[i].next();
                    break;
                } else {
                    iterators[i] = new Pairs<ValueExpr,ValueExpr>(group1[i],group2[i]).iterator();
                    iterations[i] = iterators[i].next();
                }
            }
            first = false;

            return new MapValuesTranslator(iterations);
        }

        public void remove() {
            throw new RuntimeException("not supported");
        }

    }

    public Iterator<MapValuesTranslate> iterator() {
        Map<ConcreteClass, Set<ValueExpr>> map1 = groupClasses(values1);
        Map<ConcreteClass, Set<ValueExpr>> map2 = groupClasses(values2);

        if(map1.size()!=map2.size()) // чтобы в classSet только в одну сторону проверять
            return new EmptyIterator<MapValuesTranslate>();

        Set<ValueExpr>[] group1 = new Set[map1.size()]; int groups = 0;
        Set<ValueExpr>[] group2 = new Set[group1.length];
        for(Map.Entry<ConcreteClass,Set<ValueExpr>> classSet1 : map1.entrySet()) {
            Set<ValueExpr> classSet2 = map2.get(classSet1.getKey());
            if(classSet2==null || classSet1.getValue().size()!=classSet2.size())
                return new EmptyIterator<MapValuesTranslate>();
            group1[groups] = classSet1.getValue();
            group2[groups++] = classSet2;
        }

        return new ClassIterator(group1,group2);
    }
}
