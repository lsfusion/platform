package platform.base;

public abstract class DNFWhere<T,This extends DNFWhere<T,This>> {

    public final T[] wheres;

    protected DNFWhere() {
        wheres = newArray(0);
    }

    protected DNFWhere(T[] iWheres) {
        wheres = iWheres;
    }

    protected DNFWhere(T where) {
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

    protected abstract This getThis();

    public boolean isTrue() {
        return false;
    }

    protected abstract This createThis(T[] iWheres);
    protected abstract T[] newArray(int size);

    public abstract boolean means(T from,T to);
    public abstract T[] and(T where1,T where2);

    public This or(This where) {
        if(isFalse() || where.isTrue()) return where;
        if(where.isFalse() || isTrue()) return getThis();

        T[] ors = newArray(wheres.length+where.wheres.length); int ornum=0;
        T[] keeps = wheres.clone(); int keepnum = wheres.length;
        for(T and : where.wheres) {
            boolean means = false;
            for(int i=0;i<keeps.length;i++)
                if(keeps[i]!=null) {
                    if(means(and,keeps[i])) {
                        means = true;
                        break;
                    }
                    if(means(keeps[i],and)) {
                        keeps[i] = null; keepnum--;
                        for(int j=i+1;j< keeps.length;j++)
                            if(keeps[j]!=null && means(keeps[j],and)) {
                                keeps[j] = null; keepnum--; }
                        break;
                    }
                }
            if(!means)
                ors[ornum++] = and;
        }
        T[] results = newArray(ornum+keepnum); System.arraycopy(ors,0, results,0,ornum);
        for(T keep : keeps)
            if(keep !=null)
                results[ornum++] = keep;
        return createThis(results);
    }
    public This and(This where) {
        if(isTrue() || where.isFalse()) return where;
        if(isFalse() || where.isTrue()) return getThis();

        This result = createThis(newArray(0));
        for(T andOp : where.wheres)
            for(T and : wheres)
                result = result.or(createThis(and(andOp,and)));
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof DNFWhere && BaseUtils.equalArraySets(wheres,(((DNFWhere)o).wheres)) && getClass()==o.getClass();
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
