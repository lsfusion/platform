package platform.server.data;

import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.*;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.exprs.cases.MapCase;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.sql.PostgreDataAdapter;
import platform.server.data.types.Type;

import java.util.Collection;
import java.util.Map;
import java.util.HashMap;

public abstract class DataSource<K,V> {

    public DataSource() {
    }

    public abstract Collection<K> getKeys();

    public static final SQLSyntax debugSyntax = new PostgreDataAdapter();

    public abstract Collection<V> getProperties();
    public abstract Type getType(V property);

    public abstract int hashProperty(V Property);

    public abstract String getSource(SQLSyntax syntax, Map<ValueExpr, String> params);

    public abstract String getKeyName(K Key);
    public abstract String getPropertyName(V Property);

    // получает строку по которой можно определить входит ли ряд в запрос Select
    public String getInSourceName() {
        return (getKeys().size()>0?getKeyName(getKeys().iterator().next()):"subkey");
    }

    //    abstract <MK,MV> DataSource<K, Object> merge(DataSource<MK,MV> Merge, Map<K,MK> MergeKeys, Map<MV, Object> MergeProps);
    public <MK, MV> DataSource<K, Object> merge(DataSource<MK, MV> merge, Map<K, MK> mergeKeys, Map<MV, Object> mergeProps) {
        if(this== merge) {
            for(Map.Entry<K,MK> MapKey : mergeKeys.entrySet())
                if(!MapKey.getKey().equals(MapKey.getValue()))
                    return null;

            for(MV Field : merge.getProperties())
                mergeProps.put(Field,Field);

            return (DataSource<K, Object>)((DataSource<K,?>)this);
        }
        return null;
    }

    public Join<V> join(Map<K, ? extends SourceExpr> joinImplement) {
        CaseJoin<V> result = new CaseJoin<V>(getProperties());
        for(MapCase<K> caseJoin : CaseExpr.pullCases(joinImplement))
            result.add(new JoinCase<V>(caseJoin.where,joinAnd(caseJoin.data)));
        return result;
    }

    public DataJoin<K, V> joinAnd(Map<K, ? extends AndExpr> joinImplement) {
        return new DataJoin<K,V>(this,joinImplement);
    }

    public abstract Collection<ValueExpr> getValues();

    // заменяет параметры на другие - key есть, value какие нужны
    public abstract DataSource<K,V> translateValues(Map<ValueExpr, ValueExpr> values);

    abstract public DataSource<K,V> packClassWhere(ClassWhere<K> keyClasses);
    abstract public ClassWhere<Object> getClassWhere(Collection<V> notNull);
    abstract public ClassWhere<K> getKeyClassWhere();

    public abstract <EK, EV> Iterable<MapSource<K, V, EK, EV>> map(DataSource<EK, EV> source);

    boolean hashed = false;
    int hash;
    public int hash() {
        if(!hashed) {
            hash = getHash();
            hashed = true;
        }
        return hash;
    }
    protected int getHash() {
        return hashCode();
    }
    
}
