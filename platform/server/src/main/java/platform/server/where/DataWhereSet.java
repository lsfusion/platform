package platform.server.where;

// быстрый хэш set
public class DataWhereSet {

    int size;
    DataWhere[] table;
    int[] htable;
    int threshold;

    final float loadFactor;
    public DataWhereSet() {
        loadFactor = 0.3f;

        table = new DataWhere[4];
        htable = new int[table.length];

        threshold = (int)(table.length * loadFactor);
    }

    public DataWhereSet(DataWhereSet set) {
        size = set.size;
        loadFactor = set.loadFactor;

        table = set.table.clone();
        htable = set.htable.clone();
    }

    boolean contains(DataWhere where) {
        int hash = hash(where.hashCode());
        for(int i=hash & (table.length-1);table[i]!=null;i=(i==table.length-1?0:i+1))
            if(htable[i]==hash && table[i].equals(where))
                return true;
        return false;
    }

    boolean intersect(DataWhereSet set) {
        if(size>set.size) return set.intersect(this);

        for(int i=0;i<table.length;i++)
            if(table[i]!=null)
                for(int j=htable[i] & (set.table.length-1);set.table[j]!=null;j=(j==set.table.length-1?0:j+1))
                    if(htable[i]==set.htable[j] && table[i].equals(set.table[j]))
                        return true;
        return false;
    }

    void resize(int length) {
        DataWhere[] newTable = new DataWhere[length];
        int[] newHTable = new int[length];
        for(int i=0;i<table.length;i++)
            if(table[i]!=null) {
                int newHash = (htable[i] & (length-1));
                while(newTable[newHash]!=null) newHash = (newHash==length-1?0:newHash+1);
                newTable[newHash] = table[i];
                newHTable[newHash] = htable[i];
            }
        table = newTable;
        htable = newHTable;

        threshold = (int)(length * loadFactor);
    }

    public static int hash(int h) { // копися с hashSet'а
        h ^= (h >>> 20) ^ (h >>> 12);
        return (h ^ (h >>> 7) ^ (h >>> 4));
    }

    void add(DataWhere where) {
        add(where,hash(where.hashCode()));
    }

    void add(DataWhere where,int hash) {
        int i=hash & (table.length-1);
        while(table[i]!=null) {
            if(htable[i]==hash && (table[i]==where || table[i].equals(where)))
                return;
            i=(i==table.length-1?0:i+1);
        }
        table[i] = where; htable[i] = hash;
        if(size++>=threshold)
            resize(2*table.length);
    }

    // здесь можно еще сократить equals не проверяя друг с другом
    public void addAll(DataWhereSet set) {
        for(int i=0;i<set.table.length;i++)
            if(set.table[i]!=null)
                add(set.table[i],set.htable[i]);
    }
}

/*
// быстрый хэш set
class DataWhereSet {

    int size;
    DataWhere[] table;
    int[] htable;
    DataWhere[] wheres;
    int[] hwheres;

    float loadFactor;
    DataWhereSet() {
        loadFactor = 0.2f;

        table = new DataWhere[8];
        htable = new int[table.length];

        wheres = new DataWhere[(int)(table.length * loadFactor)];
        hwheres = new int[wheres.length];
    }

    DataWhereSet(DataWhereSet set) {
        size = set.size;
        loadFactor = set.loadFactor;

        wheres = set.wheres.clone();
        hwheres = set.hwheres.clone();

        table = set.table.clone();
        htable = set.htable.clone();
    }

    boolean contains(Where where) {
        int hash = hash(where.hashCode());
        for(int i=hash & (table.length-1);table[i]!=null;i=(i==table.length-1?0:i+1))
            if(htable[i]==hash && table[i].equals(where))
                return true;
        return false;
    }

    boolean intersect(DataWhereSet set) {
        if(size>set.size) return set.intersect(this);

        for(int i=0;i<size;i++)
            for(int j=hwheres[i] & (set.table.length-1);set.table[j]!=null;j=(j==set.table.length-1?0:j+1))
                if(hwheres[i]==set.htable[j] && wheres[i].equals(set.table[j]))
                    return true;
        return false;
    }

    void resize(int length) {
        table = new DataWhere[length];
        htable = new int[length];
        for(int i=0;i<size;i++) {
            int newHash = (hwheres[i] & (length-1));
            while(table[newHash]!=null) newHash = (newHash==length-1?0:newHash+1);
            table[newHash] = wheres[i];
            htable[newHash] = hwheres[i];
        }
        DataWhere[] newWheres = new DataWhere[(int)(length * loadFactor)];
        System.arraycopy(wheres,0,newWheres,0,size);
        int[] newHashes = new int[newWheres.length];
        System.arraycopy(hwheres,0,newHashes,0,size);
        wheres = newWheres;
        hwheres = newHashes;
    }

    int hash(int h) { // копися с hashSet'а
        h ^= (h >>> 20) ^ (h >>> 12);
        return (h ^ (h >>> 7) ^ (h >>> 4));
    }

    void add(DataWhere where) {
        add(where,hash(where.hashCode()));
    }

    void add(DataWhere where,int hash) {
        int i=hash & (table.length-1);
        while(table[i]!=null) {
            if(htable[i]==hash && (table[i]==where || table[i].equals(where)))
                return;
            i=(i==table.length-1?0:i+1);
        }
        table[i] = where; htable[i] = hash;
        wheres[size] = where; hwheres[size++] = hash;
        if(size>=wheres.length)
            resize(2*table.length);
    }

    // здесь можно еще сократить equals не проверяя друг с другом
    void addAll(DataWhereSet set) {
        for(int i=0;i<set.size;i++)
            add(set.wheres[i],set.hwheres[i]);
    }
}
  */

