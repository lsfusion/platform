package platform.server.data.query;

import platform.base.BaseUtils;
import platform.base.Pairs;
import platform.base.MapIterable;
import platform.interop.Compare;
import platform.server.data.DataSource;
import platform.server.data.MapSource;
import platform.server.data.classes.IntegralClass;
import platform.server.data.classes.where.AndClassWhere;
import platform.server.data.classes.where.ClassWhere;
import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.query.exprs.AndExpr;
import platform.server.data.query.exprs.KeyExpr;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.translators.MergeTranslator;
import platform.server.data.query.translators.PackTranslator;
import platform.server.data.query.wheres.CompareWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.Where;
import platform.server.session.SQLSession;
import platform.server.caches.Lazy;

import java.util.*;

import net.jcip.annotations.Immutable;

@Immutable
public class GroupQuery<K,V> extends DataSource<K,V> {

    public static final boolean inner = false;

    final Map<V,AndExpr> max;
    final Map<V,SourceExpr> sum;
    final Map<K,AndExpr> groupKeys;

    public Collection<K> getKeys() {
        return groupKeys.keySet();
    }

    // key какие нужны, values какие в контексте
    final Map<ValueExpr,ValueExpr> values;
    final Where<?> where;

    static <K> Where getKeysWhere(Map<K,AndExpr> groupKeys) {
        Where where = Where.TRUE;
        for(AndExpr expr : groupKeys.values())
            where = where.and(expr.getWhere());
        return where;
    }

    static <K> Where getGroupWhere(Where where,Map<K,AndExpr> groupKeys) {
        return where.and(getKeysWhere(groupKeys));
    }

    @Lazy
    Where getGroupWhere() {
        return where.and(getKeysWhere(groupKeys));
    }

    static <V> Where getPropertyWhere(Map<V,AndExpr> max,Map<V,SourceExpr> sum) {
        Where propertyWhere = Where.FALSE;
        for(AndExpr property : max.values())
            propertyWhere = propertyWhere.or(property.getWhere());
        for(SourceExpr property : sum.values())
            propertyWhere = propertyWhere.or(property.getWhere());
        return propertyWhere;
    }

    @Lazy
    Where getPropertyWhere() {
        return getPropertyWhere(max, sum);
    }
    
    // получает Where когда одно из св-в не null
    static <K,V> Where getFullWhere(Where where,Map<K,AndExpr> groupKeys,Map<V,AndExpr> max,Map<V,SourceExpr> sum) {
        return getGroupWhere(where,groupKeys).and(getPropertyWhere(max, sum));
    }

    @Lazy
    Where getFullWhere() {
        return getGroupWhere().and(getPropertyWhere());
    }

    static <K,V> Collection<InnerJoins.Entry> getInnerJoins(Where where,Map<K,AndExpr> groupKeys,Map<V,AndExpr> max,Map<V,SourceExpr> sum) {
        return getFullWhere(where,groupKeys,max,sum).getInnerJoins().compileMeans();
    }

    Collection<InnerJoins.Entry> getInnerJoins() {
        return getFullWhere().getInnerJoins().compileMeans();
    }

    Where getJoinWhere() {
        assert getInnerJoins().size()==1;
        return getInnerJoins().iterator().next().mean;
    }

    Type getExprType(SourceExpr expr) {
        return expr.getType(expr.getWhere().and(getGroupWhere()));
    }

    private static <K,V> Context getContext(Map<V, AndExpr> max, Map<V, SourceExpr> sum, Map<K, AndExpr> groupKeys, Where where) {
        // перечитаем joins, потому как из-за merge'а могут поуходить join'ы
        Context context = new Context();
        context.fill(groupKeys.values(), false);
        context.fill(max.values(), false);
        context.fill(sum.values(), false);
        where.fillContext(context, false);
        return context;
    }

    public GroupQuery(Map<V, AndExpr> iMax, Map<V, SourceExpr> iSum, Map<K, AndExpr> iGroupKeys, Where iWhere) {

        groupKeys = iGroupKeys;
        max = iMax;
        sum = iSum;
        where = iWhere.followFalse(getKeysWhere(groupKeys).not());
        
        // перечитаем joins, потому как из-за merge'а могут поуходить join'ы
        context = getContext(max, sum, groupKeys, where);
        values = BaseUtils.toMap(context.values);

        int keyCount = 0;
        keyNames = new HashMap<K, String>();
        for(K key : groupKeys.keySet())
            keyNames.put(key,"dkey"+(keyCount++));
        propertyNames = generateNames();

        assert checkQuery();
    }

    boolean checkQuery() {

        MergeTranslator merge = new MergeTranslator(context);
        assert merge.context.size()==context.size();
        for(int i=0;i<context.size();i++)
            assert context.get(i).size()==merge.context.get(i).size();

        for(Map.Entry<V,SourceExpr> sumExpr : sum.entrySet())
            assert getExprType(sumExpr.getValue()) instanceof IntegralClass;

        assert !(inner && getJoinWhere()==null); //assert'им что joinWhere 1

        // узнаем все join'ы
        Context checkContext = getContext(max, sum, groupKeys, where);
        assert !(values.containsValue(null));
        assert checkContext.forCheckEqual().equals(context.forCheckEqual());
        assert new HashSet<ValueExpr>(values.values()).equals(context.values);

        return true;
    }

    private Map<V,String> generateNames() {
        int propertyCount = 0;
        Map<V,String> genNames = new HashMap<V, String>();
        for(V property : getProperties())
            genNames.put(property,"dprop"+(propertyCount++));
        return genNames;
    }

    public String getSource(SQLSyntax syntax, Map<ValueExpr, String> params) {

        // сделаем запрос который поставит фильтр на ключи и на properties на или ???
        // ключи не колышат
        Map<Object,String> fromPropertySelect = new HashMap<Object, String>();
        Collection<String> whereSelect = new ArrayList<String>(); // проверить crossJoin
        String fromSelect;

        if(inner)
            fromSelect = CompiledQuery.fillAndSelect(new HashMap<KeyExpr,KeyExpr>(),getJoinWhere(),getFullWhere(),
                BaseUtils.<Object,V,K,SourceExpr>merge(BaseUtils.merge(max,sum),groupKeys),new HashMap<KeyExpr,String>(),
                fromPropertySelect,whereSelect,BaseUtils.crossJoin(values,params),syntax);
        else
            fromSelect = new CompiledQuery<KeyExpr,Object>(new ParsedJoinQuery<KeyExpr,Object>(context,BaseUtils.toMap(context.keys),BaseUtils.<Object,V,K,SourceExpr>merge(BaseUtils.merge(max,sum),groupKeys), getFullWhere())
                .compileSelect(syntax),values).fillSelect(new HashMap<KeyExpr, String>(),fromPropertySelect, whereSelect, params);

        String groupBy = "";
        Map<K,String> keySelect = new HashMap<K, String>();
        Map<V,String> propertySelect = new HashMap<V, String>();
        for(K key : groupKeys.keySet()) {
            String keyExpr = fromPropertySelect.get(key);
            keySelect.put(key, keyExpr);
            groupBy = (groupBy.length()==0?"":groupBy+",") + keyExpr;
        }
        for(V property : max.keySet())
            propertySelect.put(property,"MAX("+ fromPropertySelect.get(property) +")");
        for(V property : sum.keySet())
            propertySelect.put(property,"SUM("+ fromPropertySelect.get(property) +")");

        return "(" + syntax.getSelect(fromSelect, SQLSession.stringExpr(
                SQLSession.mapNames(keySelect, keyNames,new ArrayList<K>()),
                SQLSession.mapNames(propertySelect, propertyNames,new ArrayList<V>())),
                SQLSession.stringWhere(whereSelect),"",groupBy,"") + ")";
    }

    private final Map<K,String> keyNames;
    private final Map<V,String> propertyNames;

    public String getKeyName(K Key) {
        return keyNames.get(Key);
    }

    String string = null;
    public String toString() {
        return "GQ";
    }

    public String getPropertyName(V property) {
        return propertyNames.get(property);
    }

    // конструктор трансляции значений - key есть, value какие нужны
    private GroupQuery(Map<ValueExpr,ValueExpr> mapValues, GroupQuery<K,V> query) {
        context = query.context;

        max = query.max;
        sum = query.sum;

        values = BaseUtils.crossJoin(mapValues,query.values);

        keyNames = query.keyNames;
        propertyNames = query.propertyNames;

        groupKeys = query.groupKeys;
        where = query.where;

        assert checkQuery();
    }
    
    public DataSource<K, V> translateValues(Map<ValueExpr, ValueExpr> mapValues) {
        return new GroupQuery<K, V>(BaseUtils.filterKeys(mapValues,getValues()), this);
    }
    
    public <MK, MV> DataSource<K, Object> merge(DataSource<MK, MV> merge, Map<K, MK> mergeKeys, Map<MV, Object> mergeProps) {
        // если Merge'ся From'ы
        DataSource<K, Object> superMerge = super.merge(merge, mergeKeys, mergeProps);
        if(superMerge!=null) return superMerge;
        if(!(merge instanceof GroupQuery)) return null;

        GroupQuery<MK, MV> mergeGroup = (GroupQuery<MK, MV>) merge;

        if(groupKeys.size()!=mergeGroup.groupKeys.size()) return null;

        // вроде проверено достаточно эффективно работает - ключи должны совпадать
        if(getHash(groupKeys)!=getHash(mergeGroup.groupKeys)) return null;
        if(where.hash()!= mergeGroup.where.hash()) return null;

        // попробуем смерджить со всеми мапами пока не получим нужный набор ключей
        for(Map<KeyExpr,KeyExpr> mapKeys : new Pairs<KeyExpr,KeyExpr>(mergeGroup.context.keys,context.keys)) {
            DataSource<K, Object> mergedSource = proceedGroupMerge(mergeGroup, mergeKeys, mergeProps, mapKeys);
            if(mergedSource!=null) return mergedSource;
        }

        return null;
    }

    final Context context;

    // конструктор трансляции ключей, mapValues - key какие нужны, values какие нужны в контексте
    private GroupQuery(GroupQuery<K,V> query,Map<KeyExpr,KeyExpr> mapKeys,Map<ValueExpr,ValueExpr> mapValues) {
        KeyTranslator translate = new KeyTranslator(query.context,mapKeys,BaseUtils.crossJoin(query.values,mapValues));
        context = translate.context;

        groupKeys = translate.translateAnd(query.groupKeys);
        sum = translate.translate(query.sum);
        max = translate.translateAnd(query.max);

        values = mapValues;
        where = query.where.translate(translate);

        keyNames = query.keyNames;
        propertyNames = query.propertyNames;

        assert checkQuery();
    }

    // уже со всеми готовыми элементами конструктор
    private GroupQuery(Context iContext,Map<K,String> iKeyNames,Map<V, AndExpr> iMax, Map<V, SourceExpr> iSum, Map<K, AndExpr> iGroupKeys, Map<ValueExpr, ValueExpr> iValues, Where iWhere) {
        context = iContext;

        groupKeys = iGroupKeys;
        max = iMax;
        sum = iSum;

        values = iValues;
        where = iWhere;

        keyNames = iKeyNames;
        propertyNames = generateNames();

        assert checkQuery();
    }

    private <MK, MV> DataSource<K, Object> proceedGroupMerge(GroupQuery<MK, MV> mergeGroup, Map<K, MK> mergeKeys, Map<MV, Object> mergeProps, Map<KeyExpr, KeyExpr> mapKeys) {

        // сливаем значения
        Map<ValueExpr,ValueExpr> mergedValues = new HashMap<ValueExpr, ValueExpr>(values);
        Map<ValueExpr,ValueExpr> transValues = new HashMap<ValueExpr, ValueExpr>(); // просто получается фильтр в mergedValues только из mergeGroup.values
        for(Map.Entry<ValueExpr,ValueExpr> mergeValue : mergeGroup.values.entrySet())
            transValues.put(mergeValue.getKey(), BaseUtils.addValue(mergedValues, mergeValue.getKey()));

        // перетранслируем mergeGroup на эти ключи
        GroupQuery<MK,MV> transGroup = new GroupQuery<MK,MV>(mergeGroup,mapKeys,transValues);
        
        MergeTranslator merge = new MergeTranslator(new Context(context,transGroup.context));

        // проверяем что совпадают Where
        Where mergedWhere = where.translate(merge);
        if(!mergedWhere.equals(transGroup.where.translate(merge))) return null;

        // должны совпасть все ключи
        Map<K,AndExpr> mergedKeys = merge.translateAnd(groupKeys);
        if(!mergedKeys.equals(BaseUtils.join(mergeKeys, merge.translateAnd(transGroup.groupKeys)))) return null;

        Map<Object, AndExpr> mergedMax = BaseUtils.mergeMaps(merge.translateAnd(max), merge.translateAnd(transGroup.max), mergeProps);
        Map<Object, SourceExpr> mergedSum = BaseUtils.mergeMaps(merge.translate(sum), merge.translate(transGroup.sum), mergeProps);

        // проверим assertion  чтобы не нарушить
        if(inner && getInnerJoins(mergedWhere,mergedKeys,mergedMax,mergedSum).size()!=1) return null;

        // "слилось", теперь "сливаем" max'ы и sum'ы
        return new GroupQuery<K, Object>(merge.context,keyNames,mergedMax,mergedSum,
                mergedKeys, mergedValues, mergedWhere);
    }

    public Collection<V> getProperties() {
        return BaseUtils.mergeSet(max.keySet(),sum.keySet());
    }

    @Lazy
    public Type getType(V property) {
        AndExpr maxExpr = max.get(property);
        if(maxExpr==null)
            return getExprType(sum.get(property));
        else
            return getExprType(maxExpr);
    }

    // конструктор проталкивания значений в GroupQuery
    public GroupQuery(GroupQuery<K,V> query,Map<K,ValueExpr> mergeKeys) {
        max = query.max;
        sum = query.sum;

        propertyNames = query.propertyNames;

        groupKeys = new HashMap<K, AndExpr>();

        Where mergedWhere = query.where;
        values = new HashMap<ValueExpr, ValueExpr>(query.values);
        // раскидываем по dumb'ам или на выход
        for(Map.Entry<K,AndExpr> groupKey : query.groupKeys.entrySet()) {
            ValueExpr groupValue = mergeKeys.get(groupKey.getKey());
            if(groupValue==null)
                groupKeys.put(groupKey.getKey(), groupKey.getValue());
            else
                mergedWhere = mergedWhere.and(new CompareWhere(groupKey.getValue(), BaseUtils.addValue(values, groupValue), Compare.EQUALS));
        }

        context = new Context(query.context,values.values()); // нужно докинуть значения из контекста

        keyNames = BaseUtils.filterKeys(query.keyNames,groupKeys.keySet());

        where = mergedWhere;

        assert checkQuery();
    }

    public Collection<ValueExpr> getValues() {
        return values.keySet();
    }

    public <EK,EV> Iterable<MapSource<K,V,EK,EV>> map(final DataSource<EK,EV> source) {
        if(!(source instanceof GroupQuery)) return new ArrayList<MapSource<K,V,EK,EV>>();

        final GroupQuery<EK,EV> query =  (GroupQuery<EK,EV>)source;
        
        return new MapIterable<MapContext, MapSource<K,V,EK,EV>>() {
            protected MapSource<K, V, EK, EV> map(MapContext mapContext) {
                if(!where.equals(query.where,mapContext)) return null;

                Map<K, EK> equalKeys;
                if((equalKeys=mapContext.equalProps(groupKeys, query.groupKeys))==null) return null;
                Map<V,EV> equalMax;
                if((equalMax= mapContext.equalProps(max, query.max))==null) return null;
                Map<V,EV> equalSum;
                if((equalSum= mapContext.equalProps(sum, query.sum))==null) return null;

                return new MapSource<K, V, EK, EV>(equalKeys,BaseUtils.merge(equalMax,equalSum),BaseUtils.crossJoin(values,query.values,mapContext.values));
            }

            protected Iterator<MapContext> mapIterator() {
                return context.map(query.context).iterator();
            }
        };
    }

    static int getHash(Map<?,? extends SourceExpr> exprs) {
        int hash = 0;
        for(SourceExpr expr : exprs.values())
            hash = hash + expr.hash();
        return hash;
    }

    protected int getHash() {
        return context.hash() + getHash(sum) *31 + getHash(max) *31*31+ getHash(groupKeys) *31*31*31+where.hash()*31*31*31*31;
    }

    public int hashProperty(V property) {
        AndExpr maxExpr = max.get(property);
        if(maxExpr==null)
            return sum.get(property).hash()+31;
        else
            return maxExpr.hash();
    }

    private boolean checkPack(GroupQuery<K,V> query, Where trueWhere) {
        for(AndExpr expr : BaseUtils.merge(query.groupKeys.values(),query.max.values()))
            assert !expr.getWhere().means(trueWhere.not());
        return true;
    }

    private Where getMeanWhere(ClassExprWhere groupClassWhere) {
        return groupClassWhere.toMeanWhere(groupKeys.values());        
    }

    // конструктор упаковки
    public GroupQuery(GroupQuery<K,V> query,ClassWhere<K> keyClasses) {
        Where keyWhere = query.getMeanWhere(keyClasses.map(query.groupKeys));

        Where queryWhere = query.getFullWhere();
        Where<?> packedWhere = queryWhere.followFalse(query.getMeanWhere(queryWhere.getClassWhere()).and(keyWhere.not())); // не интересует когда давал заявленный классы но не верхние
//        Where packedWhere = Where.FALSE; чтобы потом joins'ами проFollowFalse'ать
//        for(InnerJoins.Entry innerJoin : query.getInnerJoins()) // joinWhere не трогаем !!! с другой стороны
//            packedWhere = packedWhere.or(innerJoin.where.followFalse(query.getMeanWhere(innerJoin.where.getClassWhere()).and(keyWhere.not())).and(innerJoin.mean));
        Where<?> fullWhere = queryWhere.and(keyWhere);

        assert checkPack(query, fullWhere);

        Map<V, SourceExpr> packedSum = fullWhere.followTrue(query.sum);
        PackTranslator pack = new PackTranslator(getContext(query.max,packedSum,query.groupKeys,packedWhere),fullWhere); //

        // groupKeys и max не надо followFalse'ить
        groupKeys = pack.translateAnd(query.groupKeys);
        max = pack.translateAnd(query.max);
        sum = pack.translate(packedSum);

        where = packedWhere.translate(pack);

        // перечитаем joins, потому как из-за merge'а могут поуходить join'ы
        context = getContext(max, sum, groupKeys, where);

        values = query.values;

        keyNames = query.keyNames;
        propertyNames = query.propertyNames;

        assert checkQuery();
    }

    public DataSource<K, V> packClassWhere(ClassWhere<K> keyClasses) {
        return new GroupQuery<K,V>(this,keyClasses);
    }

    public ClassWhere<K> getKeyClassWhere() {
        return ClassWhere.get(groupKeys,getFullWhere());
    }
    
    public ClassWhere<Object> getClassWhere(Collection<V> notNull) {
        Where propWhere = Where.TRUE;

        AndClassWhere<Object> sumClasses = new AndClassWhere<Object>();
        for(Map.Entry<V,SourceExpr> sumProp : BaseUtils.filterKeys(sum, notNull).entrySet()) {
            sumClasses.add(sumProp.getKey(),(IntegralClass)getExprType(sumProp.getValue()));
            propWhere = propWhere.and(sumProp.getValue().getWhere());
        }

        Map<V,AndExpr> notNullMax = BaseUtils.filterKeys(max, notNull);
        for(AndExpr maxProp : notNullMax.values())
            propWhere = propWhere.and(maxProp.getWhere());

        return ClassWhere.get(BaseUtils.merge(notNullMax,groupKeys),getGroupWhere().and(propWhere)).and(new ClassWhere<Object>(sumClasses));
    }
}
