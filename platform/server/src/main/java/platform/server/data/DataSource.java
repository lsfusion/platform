package platform.server.data;

import platform.server.data.query.ParsedJoin;
import platform.server.data.query.ExprTranslator;
import platform.server.data.query.Join;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.sql.SQLSyntax;

import java.util.Collection;
import java.util.Map;

public abstract class DataSource<K,V> extends Source<K,V> {

    protected DataSource(Collection<? extends K> iKeys) {
        super(iKeys);
    }
    DataSource() {
    }

    public abstract String getSource(SQLSyntax syntax, Map<ValueExpr, String> params);

    public abstract String getKeyName(K Key);
    public abstract String getPropertyName(V Property);

    // получает строку по которой можно определить входит ли ряд в запрос Select
    public String getInSourceName() {
        return (keys.size()>0?getKeyName(keys.iterator().next()):"subkey");
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

    public void parseJoin(Join<K, V> join, ExprTranslator translated, Collection<ParsedJoin> translatedJoins) {
        join.translate(translated, translatedJoins, this);
    }

    public abstract DataSource<K,V> translateValues(Map<ValueExpr, ValueExpr> values);
}
