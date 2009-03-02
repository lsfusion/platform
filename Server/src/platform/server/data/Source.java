package platform.server.data;

import platform.server.data.query.CompiledJoin;
import platform.server.data.query.ExprTranslator;
import platform.server.data.query.Join;
import platform.server.data.query.JoinQuery;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.types.Type;

import java.util.*;

// абстрактный класс источников
public abstract class Source<K,V> {

    public Collection<K> keys;
    public static boolean debugWatch = false;

    protected Source(Collection<? extends K> iKeys) {
        keys =(Collection<K>)iKeys;
    }
    protected Source() {
        keys =new ArrayList();}

    public abstract Collection<V> getProperties();
    public abstract Type getType(V Property);

    // вспомогательные методы
    public static String stringExpr(LinkedHashMap<String,String> KeySelect,LinkedHashMap<String,String> PropertySelect) {
        String ExpressionString = "";
        for(Map.Entry<String,String> Key : KeySelect.entrySet())
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Key.getValue() + " AS " + Key.getKey();
        if(KeySelect.size()==0)
            ExpressionString = "1 AS subkey";
        for(Map.Entry<String,String> Property : PropertySelect.entrySet())
            ExpressionString = (ExpressionString.length()==0?"":ExpressionString+",") + Property.getValue() + " AS " + Property.getKey();
        return ExpressionString;
    }

    public static <T> LinkedHashMap<String,String> mapNames(Map<T,String> Exprs,Map<T,String> Names, List<T> Order) {
        LinkedHashMap<String,String> Result = new LinkedHashMap<String, String>();
        for(Map.Entry<T,String> Name : Names.entrySet()) {
            Result.put(Name.getValue(),Exprs.get(Name.getKey()));
            Order.add(Name.getKey());
        }
        return Result;
    }

    public static String stringWhere(Collection<String> WhereSelect) {
        String WhereString = "";
        for(String Where : WhereSelect)
            WhereString = (WhereString.length()==0?"":WhereString+" AND ") + Where;
        return WhereString;
    }

    public abstract Map<ValueExpr,ValueExpr> getValues();

    // записывается в Join'ы
    public abstract void compileJoin(Join<K, V> join, ExprTranslator translated, Collection<CompiledJoin> translatedJoins);

    public <EK, EV> boolean equals(Source<EK, EV> source, Map<K, EK> mapKeys, Map<V, EV> mapProperties, Map<ValueExpr, ValueExpr> mapValues) {
        if(this== source) {
            for(Map.Entry<K,EK> MapKey : mapKeys.entrySet())
                if(!MapKey.getKey().equals(MapKey.getValue()))
                    return false;

            for(V Field : getProperties())
                mapProperties.put(Field, (EV) Field);

            return true;
        }
        return false;
    }

    boolean Hashed = false;
    int Hash;
    public int hash() {
        if(!Hashed) {
            Hash = getHash();
            Hashed = true;
        }
        return Hash;
    }
    protected int getHash() {
        return hashCode();
    }

    public abstract int hashProperty(V Property);

    int getComplexity() {
        Set<JoinQuery> Queries = new HashSet<JoinQuery>();
        fillJoinQueries(Queries);
        return Queries.size();
    }
    public abstract void fillJoinQueries(Set<JoinQuery> Queries);
}
