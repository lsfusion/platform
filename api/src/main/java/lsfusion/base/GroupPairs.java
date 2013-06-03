package lsfusion.base;

import org.apache.log4j.Logger;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImRevMap;

import java.util.Iterator;

public abstract class GroupPairs<G,O,I> implements Iterable<I>  {

    private final static Logger logger = Logger.getLogger(GroupPairs.class);


    protected abstract I createI(ImRevMap<O, O> map);

    private final ImMap<G, ImSet<O>> map1;
    private final ImMap<G, ImSet<O>> map2;

    private final int maxIterations;

    public GroupPairs(ImMap<O,G> group1, ImMap<O,G> group2, boolean mapConstruct, int maxIterations) {
        this.map1 = group1.groupValues();
        this.map2 = group2.groupValues();

        this.maxIterations = maxIterations;
    }

    public GroupPairs(ImMap<G, ? extends ImSet<O>> map1, ImMap<G, ? extends ImSet<O>> map2, int maxIterations) {
        this.map1 = (ImMap<G, ImSet<O>>) map1;
        this.map2 = (ImMap<G, ImSet<O>>) map2;

        this.maxIterations = maxIterations;
    }

    private class GroupIterator implements Iterator<I> {

        final ImSet<O>[] group1;
        final ImSet<O>[] group2;

        private GroupIterator(ImSet<O>[] group1, ImSet<O>[] group2) {
            this.group1 = group1;
            this.group2 = group2;

            iterators = new Iterator[group1.length];
            iterations = new ImRevMap[group1.length];
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


            for(Iterator<ImRevMap<O, O>> iterator : iterators)
                if(iterator.hasNext())
                    return true;
            return false;
        }

        Iterator<ImRevMap<O,O>>[] iterators;
        ImRevMap<O,O>[] iterations;

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

            return createI(MapFact.mergeMaps(iterations));
        }

        public void remove() {
            throw new RuntimeException("not supported");
        }

    }

    public Iterator<I> iterator() {
        if(map1.size()!=map2.size()) // чтобы в classSet только в одну сторону проверять
            return new EmptyIterator<I>();

        ImSet<O>[] group1 = new ImSet[map1.size()]; int groups = 0;
        ImSet<O>[] group2 = new ImSet[group1.length];
        for(int i=0,size=map1.size();i<size;i++) {
            ImSet<O> classSet2 = map2.get(map1.getKey(i));
            ImSet<O> classSet1 = map1.getValue(i);
            if(classSet2==null || classSet1.size()!=classSet2.size())
                return new EmptyIterator<I>();
            group1[groups] = classSet1;
            group2[groups++] = classSet2;
        }

        return new GroupIterator(group1,group2);
    }

}
