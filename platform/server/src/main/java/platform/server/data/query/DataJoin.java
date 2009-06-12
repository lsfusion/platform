package platform.server.data.query;

import platform.base.BaseUtils;
import platform.base.Pairs;
import platform.server.data.DataSource;
import platform.server.data.MapSource;
import platform.server.data.classes.where.*;
import platform.server.data.query.exprs.*;
import platform.server.data.query.exprs.cases.CaseExpr;
import platform.server.data.query.translators.KeyTranslator;
import platform.server.data.query.translators.PackTranslator;
import platform.server.data.query.translators.QueryTranslator;
import platform.server.data.query.wheres.JoinWhere;
import platform.server.data.sql.SQLSyntax;
import platform.server.where.Where;

import java.util.*;

// построе на Lazy Execution и немедленную проверку классов
public class DataJoin<J,U> implements Join<U> {

    public DataSource<J,U> source;
    public Map<J, AndExpr> joins;
    public Map<U, JoinExpr<J,U>> exprs = new HashMap<U, JoinExpr<J,U>>(); // public'ом использоваться только трансляторами
    public JoinWhere inJoin;

    // теоретически только для таблиц может быть
    public boolean noAlias = false;
    public final static boolean debugWatch = false;

    public DataJoin(DataSource<J, U> iSource, Map<J,? extends AndExpr> iJoins) {
        source = iSource;
        joins = (Map<J, AndExpr>) iJoins;

        assert (joins.size()==source.getKeys().size());
    }

    public DataJoin<J,U> translate(KeyTranslator translator) {
        DataJoin<J, U> dataJoin = new DataJoin<J, U>(source.translateValues(translator.values),translator.translateAnd(joins));
        translator.retranslate(this, dataJoin);
        return dataJoin;
    }

    public Join<U> translate(QueryTranslator translator) {
        Join<U> join = source.translateValues(translator.values).join(translator.translate(joins));
        translator.retranslate(this,join);
        return join;
    }

    Where getJoinsWhere() {
        Where joinWhere = Where.TRUE;
        for(AndExpr joinExpr : joins.values())
            joinWhere = joinWhere.and(joinExpr.getWhere());
        return joinWhere;
    }

    public Map<U,SourceExpr> nullExprs = new HashMap<U, SourceExpr>();

    public SourceExpr getExpr(U property) {
        SourceExpr readExpr = exprs.get(property);
        if(readExpr!=null) return readExpr;

        readExpr = nullExprs.get(property);
        if(readExpr!=null) return readExpr;

        JoinExpr<J,U> readJoinExpr = new JoinExpr<J, U>(this, property);
        // высчитываем классы, чтобы сразу если что null кинуть
        ClassExprWhere exprWhere = ClassExprWhere.FALSE;
        // здесь уже AndExpr'ы
        for(AndClassWhere<Object> andWhere : source.getClassWhere(Collections.singleton(property)).wheres) {
            // сначала закинем условия на выражения
            ClassExprWhere joinWhere = new ClassExprWhere(readJoinExpr, andWhere.get(property));
            // затем закинем условия на join'ы
            for(Map.Entry<J, AndExpr> joinExpr : joins.entrySet())
                joinWhere = joinWhere.and(joinExpr.getValue().getClassWhere(andWhere.get(joinExpr.getKey())));
            exprWhere = exprWhere.or(joinWhere);
        }

        exprWhere = exprWhere.and(getJoinsWhere().getClassWhere());

        if(exprWhere.isFalse()) { // если null то возвращаем
            SourceExpr nullExpr = new CaseExpr();
            nullExprs.put(property,nullExpr);
            return nullExpr;
        } else {
            readJoinExpr.joinClassWhere = exprWhere;
            assert exprWhere.means(getWhere().getClassWhere());
            exprs.put(property,readJoinExpr);
            return readJoinExpr;
        }
    }

    public Map<U, SourceExpr> getExprs() {
        Map<U, SourceExpr> result = new HashMap<U,SourceExpr>();
        for(U property : source.getProperties())
            result.put(property,getExpr(property));
        return result;
    }

    boolean isNull = false;
    public Where getWhere() {
        if(inJoin!=null) return inJoin;
        if(isNull) return Where.FALSE;

        ClassExprWhere joinClassWhere = source.getKeyClassWhere().map(joins);
        if(joinClassWhere.isFalse()) {
            isNull = true;
            return Where.FALSE;
        } else {
            inJoin = new JoinWhere<J,U>(this,joinClassWhere);
            return inJoin;
        }
    }

    public String toString() {
        return source.toString();
    }

    // для кэша
    public <EJ,EU> boolean equals(DataJoin<EJ, EU> join, MapContext mapContext) {

        Map<ValueExpr,ValueExpr> contextValues = BaseUtils.filterKeys(mapContext.values,source.getValues());
        for(MapSource<J,U,EJ,EU> mapSource : source.map(join.source)) { // joins и values должны совпасть
            if(mapContext.equals(joins,BaseUtils.join(mapSource.mapKeys,join.joins)) && contextValues.equals(mapSource.mapValues)) {
                mapContext.put(inJoin, join.inJoin);
                for(Map.Entry<U,EU> mapProp : mapSource.mapProps.entrySet())
                    mapContext.put(exprs.get(mapProp.getKey()), join.exprs.get(mapProp.getValue()));
                return true;
            }
        }
        return false;
    }

    boolean hashed = false;
    int hash = 0;
    public int hash() {
        if(!hashed) {
            hash = 0;
            // нужен симметричный хэш относительно выражений
            for(SourceExpr join : joins.values())
                hash += join.hash();
            hash += source.hash()*31;
            hashed = true;
        }
        return hash;
    }

    String getFrom(String from, Map<QueryData, String> queryData, boolean inner, Collection<String> whereSelect, Map<KeyExpr, ValueExpr> exprValues, Map<ValueExpr,String> params, SQLSyntax syntax) {

        if(from.length()==0 && !inner)
            from = "dumb";

        // если GroupQuery проталкиваем внутрь ValueExpr'ы, и And в частности KeyExpr'ы внутрь
        DataSource<J,U> fromSource = source;
        Map<J, AndExpr> fromJoins;
        if(source instanceof GroupQuery) {
            fromJoins = new HashMap<J, AndExpr>();
            // заполняем статичные значения
            Map<J, ValueExpr> mergeKeys = new HashMap<J, ValueExpr>();
            for(Map.Entry<J, AndExpr> mapJoin : joins.entrySet()) {
                ValueExpr joinValue = null;
                if(mapJoin.getValue() instanceof ValueExpr)
                    joinValue = (ValueExpr) mapJoin.getValue();
                else
                if(mapJoin.getValue() instanceof KeyExpr){
                    ValueExpr keyValue = exprValues.get((KeyExpr)mapJoin.getValue());
                    if(keyValue!=null) joinValue = keyValue;
                }
                if(joinValue!=null)
                    mergeKeys.put(mapJoin.getKey(),joinValue);
                else
                    fromJoins.put(mapJoin.getKey(),mapJoin.getValue());
            }

            if(mergeKeys.size() > 0)
                fromSource = new GroupQuery<J,U>(((GroupQuery<J,U>) source),mergeKeys);
        } else
            fromJoins = joins;

        String joinString = "";
        String sourceString = fromSource.getSource(syntax, params);
        String alias;
        if(noAlias)
            alias = sourceString;
        else {
            alias = "t"+(queryData.size()+1);
            sourceString = sourceString + " " + alias;
        }

        for(Map.Entry<J,AndExpr> keyJoin : fromJoins.entrySet()) {
            String keySourceString = alias + "." + fromSource.getKeyName(keyJoin.getKey());
            // KeyExpr'а есди не было закинем
            String keyJoinString = keyJoin.getValue().getSource(queryData, syntax);
            if(debugWatch && keyJoinString==null && !inner) { // кинем висячий ключ на выход
                keyJoinString = keyJoin.getValue().toString();
                queryData.put((KeyExpr)keyJoin.getValue(),keyJoinString);
            }

            if(keyJoinString==null) {// значит KeyExpr которого еще не было
                if(!inner)
                    throw new RuntimeException("Не хватает ключей");
                queryData.put((KeyExpr)keyJoin.getValue(),keySourceString);
            } else {
                keySourceString = keySourceString + "=" + keyJoinString;
                if(from.length()==0)
                    whereSelect.add(keySourceString);
                else
                    joinString = (joinString.length()==0?"":joinString+" AND ") + keySourceString;
            }
        }

        // закинем все Expr'ы и Where
        for(Map.Entry<U,JoinExpr<J,U>> joinExpr : exprs.entrySet())
            queryData.put(joinExpr.getValue(),alias+"."+fromSource.getPropertyName(joinExpr.getKey()));
        if(inJoin!=null) queryData.put(inJoin,alias+"."+fromSource.getInSourceName()+" IS NOT NULL");

        if(from.length()==0)
            return sourceString;
        else
            return from + (inner ?"":" LEFT")+" JOIN " + sourceString + " ON "+(joinString.length()==0? Where.TRUE_STRING :joinString);
    }

    DataJoin<J,U> pack(Where where, PackTranslator translated) { // не по всем joins'ам есть условия
        DataJoin<J,U> packedJoin = new DataJoin<J,U>(source.packClassWhere(ClassWhere.get(joins,where.and(getWhere()))), translated.translateAnd(joins));
        translated.retranslate(this, packedJoin);
        return packedJoin;
    }

    <MJ,MU> DataJoin<J,Object> merge(DataJoin<MJ,MU> mergeJoin,Map<MU, Object> mergeProps) {

        for(Map<J,MJ> mapKeys : new Pairs<J,MJ>(source.getKeys(), mergeJoin.source.getKeys())) {
            if(!BaseUtils.mapEquals(joins,mergeJoin.joins,mapKeys)) // нужны только совпадающие ключи
                continue;

            // есть уже карта попробуем merge'уть
            DataSource<J, Object> merged = source.merge(mergeJoin.source, mapKeys, mergeProps);
            if(merged!=null) return new DataJoin<J,Object>(merged,joins);
        }
        return null;
    }

    public static <J,U,EU extends U> DataJoin<J,U> immutableCast(DataJoin<J,EU> join) {
        return (DataJoin<J,U>)join;        
    }

    public Context getContext() {
        Context result = new Context();
        result.add(this,false);
        return result;
    }
}
