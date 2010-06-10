package platform.server.caches;

import platform.server.data.expr.ValueExpr;
import platform.server.classes.ConcreteClass;
import platform.base.MapIterable;
import platform.base.Pairs;
import platform.base.EmptyIterator;

import java.util.*;

public class ValuePairs implements Iterable<Map<ValueExpr,ValueExpr>> {
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

    private class ClassIterator implements Iterator<Map<ValueExpr,ValueExpr>> {

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

        public Map<ValueExpr, ValueExpr> next() {
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

            Map<ValueExpr, ValueExpr> result = new HashMap<ValueExpr, ValueExpr>();
            for(int i=0;i<group1.length;i++)
                result.putAll(iterations[i]);
            return result;
        }

        public void remove() {
            throw new RuntimeException("not supported");
        }

    }

    public Iterator<Map<ValueExpr, ValueExpr>> iterator() {
        Map<ConcreteClass, Set<ValueExpr>> map1 = groupClasses(values1);
        Map<ConcreteClass, Set<ValueExpr>> map2 = groupClasses(values2);

        if(map1.size()!=map2.size()) // чтобы в classSet только в одну сторону проверять
            return new EmptyIterator<Map<ValueExpr, ValueExpr>>();

        Set<ValueExpr>[] group1 = new Set[map1.size()]; int groups = 0;
        Set<ValueExpr>[] group2 = new Set[group1.length];
        for(Map.Entry<ConcreteClass,Set<ValueExpr>> classSet1 : map1.entrySet()) {
            Set<ValueExpr> classSet2 = map2.get(classSet1.getKey());
            if(classSet2==null || classSet1.getValue().size()!=classSet2.size())
                return new EmptyIterator<Map<ValueExpr, ValueExpr>>();
            group1[groups] = classSet1.getValue();
            group2[groups++] = classSet2;
        }

        return new ClassIterator(group1,group2);
    }
}
