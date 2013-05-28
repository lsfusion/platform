package platform.base;

import java.util.Iterator;

public abstract class AddSet<T,This extends AddSet<T,This>> extends TwinImmutableObject implements Iterable<T> {

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int i=0;
            public boolean hasNext() {
                return i<wheres.length;
            }
            public T next() {
                return wheres[i++];
            }
            public void remove() {
                throw new RuntimeException("not supported");
            }
        };
    }

    protected final T[] wheres;

    protected AddSet() {
        wheres = newArray(0);
    }

    protected AddSet(T[] wheres) {
        this.wheres = wheres;
    }

    protected AddSet(T where) {
        wheres = toArray(where);
    }

    protected T[] toArray(T where) {
        T[] newArray = newArray(1);
        newArray[0] = where;
        return newArray;
    }

    protected boolean isFalse() {
        return wheres.length == 0;
    }

    protected boolean isTrue() {
        return false;
    }

    protected abstract This createThis(T[] wheres);
    protected abstract T[] newArray(int size);

    // если true - what выбрасываем
    protected abstract boolean containsAll(T who, T what);

    protected This add(This where) {
        if(isFalse() || where.isTrue()) return where;
        if(where.isFalse() || isTrue()) return (This) this;

        T[] added = newArray(wheres.length+where.wheres.length); int numAdd=0;
        T[] keeps = wheres.clone(); int keepnum = wheres.length;
        for(T and : where.wheres) {
            boolean contained = false;
            for(int i=0;i<keeps.length;i++)
                if(keeps[i]!=null) {
                    if(containsAll(keeps[i], and)) {
                        contained = true;
                        break;
                    }
                    if(containsAll(and, keeps[i])) {
                        keeps[i] = null; keepnum--;
                        for(int j=i+1;j< keeps.length;j++)
                            if(keeps[j]!=null && containsAll(and, keeps[j])) {
                                keeps[j] = null; keepnum--; }
                        break;
                    }
                }
            if(!contained)
                added[numAdd++] = and;
        }
        T[] results = newArray(numAdd+keepnum); System.arraycopy(added,0, results,0,numAdd);
        for(T keep : keeps)
            if(keep !=null)
                results[numAdd++] = keep;
        return createThis(results);
    }

    public boolean twins(TwinImmutableObject o) {
        return BaseUtils.equalArraySets(wheres,(((AddSet) o).wheres));
    }

    public int immutableHashCode() {
        return BaseUtils.hashSet(wheres) * 31;
    }

    @Override
    public String toString() {
        String result = "";
        for(T where : wheres)
            result = (result.length()==0?"":result+",") + where;
        return "{" + result + "}";
    }
}
