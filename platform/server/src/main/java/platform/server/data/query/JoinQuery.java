package platform.server.data.query;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.data.MapSource;
import platform.server.data.classes.BaseClass;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.DataClass;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.session.SQLSession;
import platform.server.where.Where;
import platform.server.caches.Lazy;

import java.sql.SQLException;
import java.util.*;

import net.jcip.annotations.Immutable;

// запрос Join
@Immutable
public class JoinQuery<K,V> implements MapKeysInterface<K> {

    public final Map<K,KeyExpr> mapKeys;
    public Map<V, SourceExpr> properties;
    protected Where<?> where = Where.TRUE;

    public JoinQuery(Map<K,KeyExpr> iMapKeys) {
        mapKeys = iMapKeys;
        properties = new HashMap<V, SourceExpr>();
    }

    public JoinQuery(MapKeysInterface<K> mapInterface) {
        this(mapInterface.getMapKeys());
    }

    public Map<K, KeyExpr> getMapKeys() {
        return mapKeys;
    }

    public Type getKeyType(K key) {
        return mapKeys.get(key).getType(where);
    }

    protected Context context = null;

    protected static <K,V> Context getContext(Map<K,KeyExpr> mapKeys,Map<V,SourceExpr> properties,Where where)  {
        Context context = new Context();
        context.fill(mapKeys.values(),false);
        context.fill(properties.values(),false);
        where.fillContext(context, false);
        return context;
    }

    // скомпилированные св-ва
    public Context getContext() {
        if(context ==null)
            context = getContext(mapKeys,properties,where);
        return context;
    }

    public Join<V> join(Map<K, ? extends SourceExpr> joinImplement) {
        return parse().join(joinImplement);
    }

    static <K> String stringOrder(List<K> sources, int offset, LinkedHashMap<K,Boolean> orders) {
        String orderString = "";
        for(Map.Entry<K,Boolean> order : orders.entrySet())
            orderString = (orderString.length()==0?"":orderString+",") + (sources.indexOf(order.getKey())+offset+1) + " " + (order.getValue()?"DESC":"ASC");
        return orderString;
    }

    public void and(Where addWhere) {
        where = where.and(addWhere);
    }

    public void putKeyWhere(Map<K, DataObject> keyValues) {
        for(Map.Entry<K,DataObject> mapKey : keyValues.entrySet())
            and(new CompareWhere(mapKeys.get(mapKey.getKey()), mapKey.getValue().getExpr(), Compare.EQUALS));
    }

    public <CK,CV> MapSource<CK,CV,K,V> map(JoinQuery<CK,CV> query) {
        Map<CV,V> mapProps;
        for(MapContext mapContext : query.getContext().map(getContext()))
            if(query.where.equals(where,mapContext) && (mapProps=mapContext.equalProps(query.properties,properties))!=null)
                return new MapSource<CK,CV,K,V>(BaseUtils.crossValues(query.mapKeys,mapKeys,mapContext.keys),mapProps,mapContext.values);
        return null;
    }

    public ParsedQuery<K,V> parse() { // именно ParsedQuery потому как aspect'ами корректируется
        return new ParsedJoinQuery<K,V>(this);
    }

    @Lazy
    public <B> ClassWhere<B> getClassWhere(Collection<? extends V> properties) {
        return parse().getClassWhere(properties);
    }

    public CompiledQuery<K,V> compile(SQLSyntax syntax) {
        return compile(syntax, new LinkedHashMap<V, Boolean>(), 0);
    }
    CompiledQuery<K,V> compile(SQLSyntax syntax,LinkedHashMap<V,Boolean> orders,int selectTop) {
        return parse().compileSelect(syntax,orders,selectTop);
    }

    public static <V> LinkedHashMap<V,Boolean> reverseOrder(LinkedHashMap<V,Boolean> orders) {
        LinkedHashMap<V,Boolean> result = new LinkedHashMap<V, Boolean>();
        for(Map.Entry<V,Boolean> order : orders.entrySet())
            result.put(order.getKey(),!order.getValue());
        return result;
    }

    public LinkedHashMap<Map<K, Object>, Map<V, Object>> executeSelect(SQLSession session) throws SQLException {
        return executeSelect(session,new LinkedHashMap<V, Boolean>(),0);
    }
    public LinkedHashMap<Map<K, Object>, Map<V, Object>> executeSelect(SQLSession session,LinkedHashMap<V,Boolean> orders,int selectTop) throws SQLException {
        return compile(session.syntax,orders,selectTop).executeSelect(session,false);
    }

    public LinkedHashMap<Map<K, DataObject>, Map<V, ObjectValue>> executeSelectClasses(SQLSession session, BaseClass baseClass) throws SQLException {
        return executeSelectClasses(session, new LinkedHashMap<V, Boolean>(), 0, baseClass);
    }

    static class ReadClasses<T> {
        Map<T,DataClass> mapDataClasses = new HashMap<T, DataClass>();
        Map<T,Object> mapObjectClasses = new HashMap<T, Object>();

        BaseClass baseClass;

        ReadClasses(Map<T,? extends SourceExpr> map,JoinQuery<?,Object> query,BaseClass iBaseClass) {
            baseClass = iBaseClass;
            for(Map.Entry<T,? extends SourceExpr> expr : map.entrySet()) {
                Type type = expr.getValue().getType(query.where);
                if(type instanceof DataClass)
                    mapDataClasses.put(expr.getKey(),(DataClass)type);
                else {
                    Object propertyClass = new Object();
                    mapObjectClasses.put(expr.getKey(),propertyClass);
                    query.properties.put(propertyClass,expr.getValue().getClassExpr(baseClass));
                }
            }
        }

        ObjectValue read(T key,Object value,Map<Object,Object> classes) {
            ConcreteClass propertyClass = mapDataClasses.get(key);
            if(propertyClass==null) propertyClass = baseClass.findConcreteClassID((Integer) classes.get(mapObjectClasses.get(key)));
            return ObjectValue.getValue(value,propertyClass);
        }
    }

    public LinkedHashMap<Map<K, DataObject>, Map<V, ObjectValue>> executeSelectClasses(SQLSession session,LinkedHashMap<V,Boolean> orders,int selectTop, BaseClass baseClass) throws SQLException {
        LinkedHashMap<Map<K, DataObject>, Map<V, ObjectValue>> result = new LinkedHashMap<Map<K, DataObject>, Map<V, ObjectValue>>();

        if(where.isFalse()) return result; // иначе типы ключей не узнаем

        // создаем запрос с IsClassExpr'ами
        JoinQuery<K,Object> classQuery = new JoinQuery<K,Object>((JoinQuery<K,Object>) this,false);

        ReadClasses<K> keyClasses = new ReadClasses<K>(mapKeys,classQuery,baseClass);
        ReadClasses<V> propClasses = new ReadClasses<V>(properties,classQuery,baseClass);

        LinkedHashMap<Map<K, Object>, Map<Object, Object>> rows = classQuery.executeSelect(session, (LinkedHashMap<Object,Boolean>) orders,selectTop);

        // перемаппим
        for(Map.Entry<Map<K,Object>,Map<Object,Object>> row : rows.entrySet()) {
            Map<K,DataObject> keyResult = new HashMap<K, DataObject>();
            for(Map.Entry<K,Object> keyRow : row.getKey().entrySet())
                keyResult.put(keyRow.getKey(), (DataObject) keyClasses.read(keyRow.getKey(),keyRow.getValue(),row.getValue()));
            Map<V,ObjectValue> propResult = new HashMap<V, ObjectValue>();
            for(V property : properties.keySet())
                propResult.put(property,propClasses.read(property,row.getValue().get(property),row.getValue()));
            result.put(keyResult,propResult);
        }
        return result;
    }

    public void outSelect(SQLSession session) throws SQLException {
        compile(session.syntax).outSelect(session);
    }
    public void outSelect(SQLSession session,LinkedHashMap<V,Boolean> orders,int selectTop) throws SQLException {
        compile(session.syntax,orders,selectTop).outSelect(session);
    }

    public String toString() {
        return "JQ";
    }

    protected int getHash() {
        // должны совпасть hash'и properties и hash'и wheres
        int hash = 0;
        for(SourceExpr property : properties.values())
            hash += property.hash();
        return where.hash()*31+hash;
    }

    // конструктор копирования
    public JoinQuery(JoinQuery<K, V> query, boolean noProps) {
        mapKeys = query.mapKeys;
        if(noProps)
            properties = new HashMap<V, SourceExpr>();
        else
            properties = new HashMap<V, SourceExpr>(query.properties);

        where = query.where;
    }

    public <GK extends V, GV extends V> ParsedQuery<GK,GV> groupBy(Collection<GK> keys,GV property, boolean max) {
        // вытаскиваем Case'ы
        Collection<GV> maxProps = new ArrayList<GV>();
        Collection<GV> sumProps = new ArrayList<GV>();
        if(max)
            maxProps.add(property);
        else
            sumProps.add(property);

        return parse().groupBy(keys, maxProps, sumProps);
    }

    boolean hashed = false;
    int hash;
    public int hash() {
        if(!hashed) {
            hash = getHash();
            hashed = true;
        }
        return hash;
    }
}

