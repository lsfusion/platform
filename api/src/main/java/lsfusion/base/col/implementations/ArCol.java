package lsfusion.base.col.implementations;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.implementations.abs.ACol;
import lsfusion.base.col.interfaces.immutable.ImCol;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.add.MAddCol;

public class ArCol<K> extends ACol<K> implements MCol<K>, MAddCol<K> {

    public int size;
    public Object[] array;

    public ArCol() {
        this.array = new Object[4];
    }

    public ArCol(int size) {
        this.array = new Object[size];
    }

    public ArCol(int size, Object[] array) {
        this.size = size;
        this.array = array;
    }

    public ArCol(ArCol<K> col) {
        this.size = col.size();
        this.array = col.array.clone();
    }

    public int size() {
        return size;
    }

    public K get(int i) {
        return (K) array[i];
    }

    public void set(int i, K key) {
        array[i] = key;
    }

    public void add(K key) {
        if(size>=array.length) resize(2 * array.length + 1);
        array[size++] = key;
    }

    public void remove(int i) {
        System.arraycopy(array, i + 1, array, i, size-1-i);
        array[(size--) - 1] = null;
    }

    private void resize(int length) {
        Object[] newArray = new Object[length];
        System.arraycopy(array, 0, newArray, 0, size);
        array = newArray;
    }

    public void addAll(ImCol<? extends K> col) {
        for(int i=0,size=col.size();i<size;i++)
            add(col.get(i));
    }

    public ImCol<K> immutableCol() {
        if(size==0)
            return SetFact.EMPTY();
        if(size==1)
            return SetFact.singleton(single());

        if(array.length > size * SetFact.factorNotResize) {
            Object[] newArray = new Object[size];
            System.arraycopy(array, 0, newArray, 0, size);
            array = newArray;
        }
        return this;
    }

    public Iterable<K> it() {
        return this;
    }
    
    public void removeAll() {
        size = 0;
        array = new Object[4];
    }
}
