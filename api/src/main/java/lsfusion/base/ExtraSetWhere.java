package lsfusion.base;

import java.util.Iterator;

// расширенный AddSet который также подразумевает "слияние" элементов если это возможно
public abstract class ExtraSetWhere<T, This extends ExtraSetWhere<T,This>> extends TwinImmutableObject {

    public Iterable<T> it() {
        return new Iterable<T>() {
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
        };
    }

    public final T[] wheres;

    protected ExtraSetWhere() {
        wheres = newArray(0);
    }

    protected ExtraSetWhere(T[] wheres) {
        this.wheres = wheres;
    }

    protected ExtraSetWhere(T where) {
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

    public boolean calcTwins(TwinImmutableObject o) {
        return BaseUtils.equalArraySets(wheres,(((ExtraSetWhere)o).wheres));
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

    protected abstract This createThis(T[] wheres);
    protected abstract T[] newArray(int size);

    // если true - what выбрасываем
    protected abstract boolean containsAll(T who, T what);

    // может менять wheres
    protected abstract T add(T addWhere, T[] wheres, int numWheres, T[] proceeded, int numProceeded);

    // !!! важно учитывать что wheres - mutable то есть меняется
    // addCorrect - определяет достаточной ширины wheres или нет
    protected T[] add(T[] wheres, int numWheres, T[] adds, int numAdds, final boolean addCorrect) {

        T[] extradd = null; int numExtra = 0;
        T[] addProceeded = null; int numProceeded = 0;

        for(int i=0;i<numAdds;i++) {
            boolean contained = false;
            for(int j=0;j<numWheres;j++)
                if(wheres[j]!=null && containsAll(wheres[j], adds[i])) {
                    contained = true;
                    break;
                }
            if(!contained) { // не содержится
                for(int j=0;j<numWheres;j++)
                    if(wheres[j]!=null && containsAll(adds[i],wheres[j]))
                        wheres[j]=null;
                T added = add(adds[i], wheres, numWheres, addProceeded, numProceeded);
                if(added!=null) {
                    if(numExtra==0) extradd = newArray(adds.length);
                    extradd[numExtra++] = added;
                } else { // не слилось - вырезаем всех кого содержит этот элемен
                    if(addCorrect) {
                        if(numProceeded==0) addProceeded = newArray(adds.length);
                        addProceeded[numProceeded++] = adds[i];
                    } else
                        wheres[numWheres++] = adds[i];
                }
            }
        }

        if(numExtra>0) {
            if(addCorrect && numProceeded > 0) {
                System.arraycopy(addProceeded,0,wheres,numWheres,numProceeded);
                numWheres += numProceeded;
            }
            return add(wheres,numWheres,extradd,numExtra,false);
        } else {
            int actualWheres = 0;
            for(int i=0;i<numWheres;i++)
                if(wheres[i]!=null) actualWheres++;
            T[] result = newArray(actualWheres+numProceeded); int resnum=0;
            for(int i=0;i<numWheres;i++)
                if(wheres[i]!=null)
                    result[resnum++] = wheres[i];
            if(addCorrect && numProceeded > 0)
                System.arraycopy(addProceeded,0,result,resnum,numProceeded);
            return result;
        }
    }

    protected This add(This add) {
        if(isFalse() || add.isTrue()) return add;
        if(add.isFalse() || isTrue()) return (This) this;

        T[] addWheres = newArray(wheres.length+add.wheres.length);
        System.arraycopy(wheres,0,addWheres,0,wheres.length);
        return createThis(add(addWheres, wheres.length, add.wheres,add.wheres.length,true));        
    }
    
}
