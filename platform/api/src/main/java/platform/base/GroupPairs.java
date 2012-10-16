package platform.base;

import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class GroupPairs<G,O,I> implements Iterable<I>  {

    private final static Logger logger = Logger.getLogger(GroupPairs.class);


    protected abstract I createI(Map<O,O> map);

    private final Map<G, Set<O>> map1;
    private final Map<G, Set<O>> map2;

    private final int maxIterations;

    public GroupPairs(QuickMap<O,G> group1, QuickMap<O,G> group2, boolean mapConstruct, int maxIterations) {
        this.map1 = BaseUtils.groupSet(group1);
        this.map2 = BaseUtils.groupSet(group2);

        this.maxIterations = maxIterations;
    }

    public GroupPairs(BaseUtils.Group<G, O> getter, Set<O> set1, Set<O> set2, int maxIterations) {
        this.map1 = BaseUtils.groupSet(getter, set1);
        this.map2 = BaseUtils.groupSet(getter, set2);

        this.maxIterations = maxIterations;
    }

    public GroupPairs(Map<G, Set<O>> map1, Map<G, Set<O>> map2, int maxIterations) {
        this.map1 = map1;
        this.map2 = map2;

        this.maxIterations = maxIterations;
    }

    private class GroupIterator implements Iterator<I> {

        final Set<O>[] group1;
        final Set<O>[] group2;

        private GroupIterator(Set<O>[] group1, Set<O>[] group2) {
            this.group1 = group1;
            this.group2 = group2;

            iterators = new Iterator[group1.length];
            iterations = new Map[group1.length];
        }

        boolean first = true;

        int groupNext = 0;

        public boolean hasNext() {
            if(first)
                return true;

            if(maxIterations > 0 && groupNext >= maxIterations) {
                try {
                    String stackTrace = "";
                    for(StackTraceElement stackLine : Thread.currentThread().getStackTrace())
                        stackTrace += stackLine.toString() + '\n';
                    if (logger.isDebugEnabled())
                        logger.debug("MAP INNER HASH : " + map1 + '\n' + map2 + '\n' + stackTrace);
                } catch(Exception e) {
                }
                return false;
            }


            for(Iterator<Map<O, O>> iterator : iterators)
                if(iterator.hasNext())
                    return true;
            return false;
        }

        Iterator<Map<O,O>>[] iterators;
        Map<O,O>[] iterations;

        public I next() {
            groupNext++;

            for(int i=0;i<group1.length;i++) {
                if(!first && iterators[i].hasNext()) {
                    iterations[i] = iterators[i].next();
                    break;
                } else {
                    iterators[i] = SymmetricPairs.create(group1[i],group2[i]).iterator();
                    iterations[i] = iterators[i].next();
                }
            }
            first = false;

            return createI(BaseUtils.mergeMaps(iterations));
        }

        public void remove() {
            throw new RuntimeException("not supported");
        }

    }

    public Iterator<I> iterator() {
        if(map1.size()!=map2.size()) // чтобы в classSet только в одну сторону проверять
            return new EmptyIterator<I>();

        Set<O>[] group1 = new Set[map1.size()]; int groups = 0;
        Set<O>[] group2 = new Set[group1.length];
        for(Map.Entry<G,Set<O>> classSet1 : map1.entrySet()) {
            Set<O> classSet2 = map2.get(classSet1.getKey());
            if(classSet2==null || classSet1.getValue().size()!=classSet2.size())
                return new EmptyIterator<I>();
            group1[groups] = classSet1.getValue();
            group2[groups++] = classSet2;
        }

        return new GroupIterator(group1,group2);
    }

}
