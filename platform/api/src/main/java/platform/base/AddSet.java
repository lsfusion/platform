package platform.base;

public abstract class AddSet<T,This extends AddSet<T,This>> {

    public final T[] wheres;

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

    public boolean isFalse() {
        return wheres.length == 0;
    }

    public boolean isTrue() {
        return false;
    }

    protected abstract This createThis(T[] wheres);
    protected abstract T[] newArray(int size);

    // если true - what выбрасываем
    protected abstract boolean containsAll(T who, T what);

    protected This add(This where) {
        if(isFalse() || where.isTrue()) return where;
        if(where.isFalse() || isTrue()) return (This) this;

        T[] ors = newArray(wheres.length+where.wheres.length); int ornum=0;
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
                ors[ornum++] = and;
        }
        T[] results = newArray(ornum+keepnum); System.arraycopy(ors,0, results,0,ornum);
        for(T keep : keeps)
            if(keep !=null)
                results[ornum++] = keep;
        return createThis(results);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof AddSet && BaseUtils.equalArraySets(wheres,(((AddSet)o).wheres)) && getClass()==o.getClass();
    }

    @Override
    public int hashCode() {
        return BaseUtils.hashArraySet(wheres) * 31;
    }

    @Override
    public String toString() {
        String result = "";
        for(T where : wheres)
            result = (result.length()==0?"":result+",") + where;
        return "{" + result + "}";
    }
}
