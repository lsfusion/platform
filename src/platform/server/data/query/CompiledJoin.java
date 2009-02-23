package platform.server.data.query;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;

import platform.base.Pairs;
import platform.base.BaseUtils;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.query.exprs.*;
import platform.server.data.DataSource;
import platform.server.data.Source;
import platform.server.where.Where;
import platform.Main;

public class CompiledJoin<J> extends Join<J,Object> {

    CompiledJoin(DataSource<J, Object> iSource, Map<J, ? extends SourceExpr> iJoins, boolean iNoAlias) {
        super(iSource, iJoins, iNoAlias);
    }

    DataSource<J,Object> getDataSource() {
        return (DataSource<J,Object>) source;
    }

    <MJ,MU> Map<MU, JoinExpr> merge(DataSource<MJ,MU> mergeSource,Map<MJ,? extends SourceExpr> mergeJoins) {

        // проверить что кол-во Keys в Source совпадает
        Map<MU,Object> mergeProps = new HashMap<MU,Object>();
        for(Map<J,MJ> mapKeys : new Pairs<J,MJ>(source.keys, mergeSource.keys)) {
            if(!BaseUtils.mapEquals(joins, mergeJoins,mapKeys)) // нужны только совпадающие ключи
                continue;

            // есть уже карта попробуем merge'уть
            Source<J, Object> merged = getDataSource().merge(mergeSource, mapKeys, mergeProps);
            if(merged!=null) { // нашли, изменим Source
                source = merged;
                Map<MU, JoinExpr> mergeExprs = new HashMap<MU, JoinExpr>();
                for(Map.Entry<MU,Object> mergeProp : mergeProps.entrySet()) { // докинем недостающие JoinExpr'ы
                    JoinExpr<J,Object> joinExpr = exprs.get(mergeProp.getValue());
                    if(joinExpr ==null) {
                        joinExpr = new JoinExpr<J,Object>(this,mergeProp.getValue());
                        exprs.put(mergeProp.getValue(), joinExpr);
                    }
                    mergeExprs.put(mergeProp.getKey(), joinExpr);
                }
                return mergeExprs;
            }
        }
        return null;
    }

    String getFrom(String from, Map<QueryData, String> queryData, boolean inner, Collection<String> whereSelect, Map<AndExpr, ValueExpr> exprValues, Map<ValueExpr,String> params, SQLSyntax syntax) {

        if(from.length()==0 && !inner)
            from = "dumb";

        // если GroupQuery проталкиваем внутрь ValueExpr'ы, и And в частности KeyExpr'ы внутрь
        DataSource<J,Object> fromSource = null;
        Map<J, SourceExpr> fromJoins;
        if(source instanceof GroupQuery) {
            fromJoins = new HashMap<J, SourceExpr>();
            // заполняем статичные значения
            Map<J, ValueExpr> mergeKeys = new HashMap<J, ValueExpr>();
            for(Map.Entry<J, SourceExpr> mapJoin : joins.entrySet()) {
                ValueExpr joinValue = null;
                if(mapJoin.getValue() instanceof ValueExpr)
                    joinValue = (ValueExpr) mapJoin.getValue();
                else {
                    ValueExpr keyValue = exprValues.get(mapJoin.getValue());
                    if(keyValue!=null) joinValue = keyValue;
                }
                if(joinValue!=null)
                    mergeKeys.put(mapJoin.getKey(),joinValue);
                else
                    fromJoins.put(mapJoin.getKey(),mapJoin.getValue());
            }

            if(mergeKeys.size() > 0)
                fromSource = (DataSource<J,Object>) ((GroupQuery) source).mergeKeyValue(mergeKeys,fromJoins.keySet());
        } else
            fromJoins = joins;

        if(fromSource==null) fromSource = getDataSource();

        String joinString = "";
        String sourceString = fromSource.getSource(syntax, params);
        String alias;
        if(noAlias)
            alias = sourceString;
        else {
            alias = "t"+(queryData.size()+1);
            sourceString = sourceString + " " + alias;
        }

        for(Map.Entry<J, SourceExpr> keyJoin : fromJoins.entrySet()) {
            String keySourceString = alias + "." + fromSource.getKeyName(keyJoin.getKey());
            // KeyExpr'а есди не было закинем
            String keyJoinString = keyJoin.getValue().getSource(queryData, syntax);
            if(Main.debugWatch && keyJoinString==null) // кинем висячий ключ на выход
                keyJoinString = keyJoin.getValue().toString();

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
        for(Map.Entry<Object, JoinExpr<J,Object>> joinExpr : exprs.entrySet())
            queryData.put(joinExpr.getValue(),alias+"."+fromSource.getPropertyName(joinExpr.getKey()));
        queryData.put(inJoin,alias+"."+fromSource.getInSourceName()+" IS NOT NULL");

        if(from.length()==0)
            return sourceString;
        else
            return from + (inner ?"":" LEFT")+" JOIN " + sourceString + " ON "+(joinString.length()==0? Where.TRUE_STRING :joinString);
    }

    CompiledJoin<J> translate(ExprTranslator translated,Map<ValueExpr, ValueExpr> mapValues) {

        Map<J, SourceExpr> transJoins = new HashMap<J, SourceExpr>();
        for(Map.Entry<J, SourceExpr> mapJoin : joins.entrySet())
            transJoins.put(mapJoin.getKey(),mapJoin.getValue().translate(translated));

        CompiledJoin<J> transJoin = new CompiledJoin<J>(getDataSource().translateValues(mapValues),transJoins,false);
        translated.put(inJoin,transJoin.inJoin);
        for(Map.Entry<Object, JoinExpr<J,Object>> expr : exprs.entrySet())
            translated.put(expr.getValue(),transJoin.exprs.get(expr.getKey()));

        return transJoin;
    }
}
