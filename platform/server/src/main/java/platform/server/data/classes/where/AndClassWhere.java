package platform.server.data.classes.where;

import platform.server.data.classes.ConcreteClass;
import platform.server.data.query.exprs.AndExpr;
import platform.server.where.Where;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AndClassWhere<K> extends AbstractAndClassWhere<K,AndClassWhere<K>> {

    public AndClassWhere() {
    }

    public AndClassWhere(AndClassWhere<? extends K> set) {
        super((AndClassWhere<K>) set);
    }

    public AndClassWhere(K key, ClassSet classes) {
        super(key, classes);
    }

    protected ClassSet[] newValues(int size) {
        return new ClassSet[size];
    }

    protected AndClassWhere<K> getThis() {
        return this;
    }

    protected AndClassWhere<K> copy() {
        return new AndClassWhere<K>(this);
    }

    // чисто для getCommonParent
    public Map<K,ClassSet> toMap() {
        Map<K,ClassSet> map = new HashMap<K, ClassSet>();
        for(int i=0;i<size;i++)
            map.put((K) table[indexes[i]], vtable[indexes[i]]);
        return map;        
    }

    public AndClassWhere(Map<K, ConcreteClass> map) {
        for(Map.Entry<K,ConcreteClass> entry : map.entrySet())
            add(entry.getKey(),entry.getValue());
    }

    public <V> AndClassWhere<V> mapKeys(Map<K,V> map) {
        AndClassWhere<V> result = new AndClassWhere<V>();
        for(int i=0;i<size;i++)
            result.add(map.get(table[indexes[i]]),vtable[indexes[i]]);
        return result;
    }

    public void fillKeys(Set<K> keys) {
        for(int i=0;i<size;i++)
            keys.add((K) table[indexes[i]]);
    }
}
