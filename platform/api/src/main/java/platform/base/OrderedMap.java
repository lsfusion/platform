package platform.base;

import java.util.*;

public class OrderedMap<K,V> extends LinkedHashMap<K,V> {

    public OrderedMap() {
    }

    public OrderedMap(OrderedMap<K,V> orderedMap) {
        super(orderedMap);
    }

    public OrderedMap(Map<? extends K, ? extends V> map) {
        super(map);
    }

    public OrderedMap(List<K> list, V value) {
        for(K item : list)
            put(item, value);
    }

    public OrderedMap(K key, V value) {
        put(key, value);
    }

    private void reverse(Iterator<Map.Entry<K,V>> i) {
        if(i.hasNext()) {
            Map.Entry<K,V> entry = i.next();
            reverse(i);
            put(entry.getKey(),entry.getValue());
        }
    }

    public OrderedMap<K,V> reverse() {
        OrderedMap<K,V> result = new OrderedMap<K,V>();
        result.reverse(entrySet().iterator());
        return result;
    }


    public OrderedMap<K,V> moveStart(Collection<K> col) {
        OrderedMap<K,V> result = new OrderedMap<K,V>();
        for(Map.Entry<K,V> entry : entrySet())
            if(col.contains(entry.getKey()))
                result.put(entry.getKey(),entry.getValue());
        for(Map.Entry<K,V> entry : entrySet())
            if(!col.contains(entry.getKey()))
                result.put(entry.getKey(),entry.getValue());
        return result;
    }

    public <M> OrderedMap<M,V> map(Map<K,M> map) {
        OrderedMap<M,V> result = new OrderedMap<M,V>();
        for(Map.Entry<K,V> entry : entrySet())
            result.put(map.get(entry.getKey()),entry.getValue());
        return result;
    }

    public boolean starts(Collection<K> col) {
        return equals(moveStart(col));
    }

    public K singleKey() {
        return BaseUtils.single(keySet());
    }

    public V singleValue() {
        return BaseUtils.singleValue(this);
    }

    public Map.Entry<K,V> singleEntry() {
        return BaseUtils.singleEntry(this);
    }

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (Map.Entry<K, V> entry : entrySet())
            hashCode = 31 * hashCode + entry.hashCode();
        return hashCode;
    }

    // собсно ради этого метода класс и создавался
    @Override
    public boolean equals(Object o) {
        if(this==o) return true;
        if(!(o instanceof OrderedMap)) return false;

        OrderedMap<?,?> orderedMap = (OrderedMap)o;
        if(size()!=orderedMap.size()) return false;

        Iterator<Map.Entry<K,V>> i1 = entrySet().iterator();
        Iterator<? extends Map.Entry<?,?>> i2 = orderedMap.entrySet().iterator();
        while(i1.hasNext()) {
            Map.Entry<K, V> entry1 = i1.next();
            Map.Entry<?,?> entry2 = i2.next();
            if(!(entry1.getKey().equals(entry2.getKey()) && BaseUtils.nullEquals(entry1.getValue(),entry2.getValue())))
                return false;
        }

        return true;
    }

    public int indexOf(K key) {
        Iterator<K> i = keySet().iterator();
        int result = 0;
        while(i.hasNext()) {
            if(i.next().equals(key))
                return result;
            result++;
        }
        return -1;
    }

    public K getKey(int index) {
        Iterator<K> i = keySet().iterator();
        for(int j=0;j<index;j++)
            i.next();
        return i.next();
    }

    public List<K> keyList() {
        return new ArrayList<K>(keySet()); 
    }

    public V getValue(int index) {
        Iterator<V> i = values().iterator();
        for(int j=0;j<index;j++)
            i.next();
        return i.next();
    }

    public void removeAll(Collection<K> keys) {
        for(K key : keys)
            remove(key);
    }
}
