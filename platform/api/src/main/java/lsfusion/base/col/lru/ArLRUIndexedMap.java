package lsfusion.base.col.lru;

import lsfusion.base.col.implementations.*;

public class ArLRUIndexedMap<K, V> implements MCacheMap<K, V> {

    public int size;
    public Object[] keys;
    public Object[] values;
    public int[] used;
    
    private static ThreadLocal<ThreadBucket> pendingBucket = new ThreadLocal<ThreadBucket>();

    public ArLRUIndexedMap(byte strategy) {
        this.keys = new Object[1];
        this.values = new Object[1];
        this.used = new int[1];

        ThreadBucket pending = pendingBucket.get();
        if(pending == null) {
            pending = new ThreadBucket();
            pendingBucket.set(pending);
        }

        pending.add(this, strategy);
    }

    private int findIndex(Object key) {
        return ArIndexedMap.findIndex(key, size, keys);
    }

    public synchronized V get(K key) {
        int index = findIndex(key);
        if(index >= 0) {
            used[index] = (int) (System.nanoTime() >> 32);
            return (V)values[index];
        }
        return null;
    }

    public synchronized boolean containsKey(K key) {
        int index = findIndex(key);
        if(index >= 0) {
            used[index] = (int) (System.nanoTime() >> 32);
            return true;
        }
        return false;
    }

    private void resize(int length) {
        Object[] newKeys = new Object[length];
        System.arraycopy(keys, 0, newKeys, 0, size);
        keys = newKeys;
        Object[] newValues = new Object[length];
        System.arraycopy(values, 0, newValues, 0, size);
        values = newValues;
        int[] newUsed = new int[length];
        System.arraycopy(used, 0, newUsed, 0, size);
        used = newUsed;
    }

    public synchronized void removeExpired(long expired) {
        for(int i=size-1;i>=0;i--) {
            if(used[i] < expired) { // устарела
                System.arraycopy(keys, i + 1, keys, i, size - i - 1);
                keys[size-1] = null;
                System.arraycopy(values, i + 1, values, i, size - i - 1);
                values[size-1] = null;
                System.arraycopy(used, i + 1, used, i, size - i - 1);
                used[size-1] = 0;
                size--;
            }
        }

        while(true) {
            int resize = keys.length >> 1;
            if(resize > 0 && resize >= size)
                resize(resize);
            else
                break;
        }
    }

    public synchronized void exclAdd(K key, V value) {
        if (size >= keys.length) resize(keys.length << 1);

        int index = findIndex(key);
        assert index < 0;
        int insert = (- index - 1);
        System.arraycopy(keys, insert, keys, insert + 1, size - insert);
        System.arraycopy(values, insert , values, insert + 1, size - insert);
        System.arraycopy(used, insert , used, insert + 1, size - insert);
        keys[insert] = key;
        values[insert] = value;
        used[insert] = (int) (System.nanoTime() >> 32);
        size++;
    }
}
