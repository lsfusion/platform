package platform.server.data.query;

import platform.base.Pairs;
import platform.server.data.DataSource;
import platform.server.data.Source;
import platform.server.data.query.exprs.*;
import platform.server.data.query.wheres.JoinWhere;

import java.util.*;

public class Join<J,U>  {
    public Source<J,U> source;
    public Map<J, SourceExpr> joins;
    public Map<U, JoinExpr<J,U>> exprs = new HashMap<U, JoinExpr<J,U>>();
    public JoinWhere inJoin;

    // теоретически только для таблиц может быть
    public boolean noAlias = false;

    public Join(Source<J,U> iSource) {
        this(iSource,new HashMap<J, SourceExpr>());
    }

    public Join(Source<J, U> iSource, Map<J,? extends SourceExpr> iJoins) {
        this(iSource,iJoins,false);
    }

    Join(Source<J, U> iSource, Map<J,? extends SourceExpr> iJoins, boolean iNoAlias) {
        source = iSource;
        joins = (Map<J,SourceExpr>) iJoins;
        noAlias = iNoAlias;

        inJoin = new JoinWhere(this);
        for(U property : source.getProperties())
            exprs.put(property,new JoinExpr<J,U>(this,property));
    }

    // конструктор когда надо просто ключи протранслировать
    public <K> Join(Source<J,U> iSource,Map<J,K> iJoins, JoinQuery<K,?> mapSource) {
        this(iSource);

        for(J implement : source.keys)
            joins.put(implement,mapSource.mapKeys.get(iJoins.get(implement)));
    }

    public <K> Join(Source<J,U> iSource,JoinQuery<K,?> mapSource,Map<K,J> iJoins) {
         this(iSource);

         for(K Implement : mapSource.keys)
             joins.put(iJoins.get(Implement),mapSource.mapKeys.get(Implement));
    }

    public <V> Join(Source<J,U> iSource,JoinQuery<J,V> mapSource) {
        this(iSource);

        for(J Key : source.keys)
            joins.put(Key,mapSource.mapKeys.get(Key));
    }

    void addJoin(List<Join> fillJoins) {
        fillJoins.add(this);
    }

    public void fillJoins(List<? extends Join> fillJoins, Set<ValueExpr> values) {
        if(fillJoins.contains(this)) return;

        for(SourceExpr Join : joins.values())
            Join.fillJoins(fillJoins, values);
        addJoin((List<Join>) fillJoins);
    }

    public void translate(ExprTranslator translated, Collection<CompiledJoin> translatedJoins, DataSource<J,U> joinSource) {
        MapCaseList<J> caseList = CaseExpr.translateCase(joins, translated, true, false);

        // перетранслируем InJoin'ы в OR (And Where And NotWhere And InJoin)
        CaseJoins<J,U> caseJoins = new CaseJoins<J,U>(translatedJoins, joinSource, noAlias);
        translated.put(inJoin,caseList.getWhere(caseJoins));
        // перетранслируем все выражения в CaseWhen'ы
        for(Map.Entry<U, JoinExpr<J,U>> mapJoin : exprs.entrySet()) {
            ExprCaseList translatedExpr = new ExprCaseList();
            for(MapCase<J> mapCase : caseList) // здесь напрямую потому как MapCaseList уже все проверил
                translatedExpr.add(new ExprCase(mapCase.where,caseJoins.get(mapCase).get(mapJoin.getKey())));
            translated.put(mapJoin.getValue(),translatedExpr.getExpr(mapJoin.getValue().getType()));
        }
    }

    public String toString() {
        return source.toString();
    }

    // для кэша
    public <EJ,EU> boolean equals(Join<EJ, EU> join, Map<ValueExpr, ValueExpr> mapValues, Map<ObjectExpr, ObjectExpr> mapExprs, Map<JoinWhere, JoinWhere> mapWheres) {

        // проверить что кол-во Keys в Source совпадает
        for(Map<J,EJ> mapKeys : new Pairs<J,EJ>(source.keys, join.source.keys)) {
            boolean equal = true;
            for(Map.Entry<J,EJ> mapKey : mapKeys.entrySet()) {
                if(!joins.get(mapKey.getKey()).equals(join.joins.get(mapKey.getValue()), mapExprs, mapWheres)) {
                    equal = false;
                    break;
                }
            }
            if(!equal) continue;

            Map<U,EU> mapProperties = new HashMap<U, EU>();
            if(source.equals(join.source,mapKeys,mapProperties, mapValues)) {
                for(Map.Entry<U,EU> mapProp : mapProperties.entrySet())
                    mapExprs.put(exprs.get(mapProp.getKey()), join.exprs.get(mapProp.getValue()));
                mapWheres.put(inJoin, join.inJoin);
                return true;
            }
        }

        return false;

    }

    boolean hashed = false;
    int hash = 0;
    public int hash() {
        if(!hashed) {
            // нужен симметричный хэш относительно выражений
            for(SourceExpr join : joins.values())
                hash += join.hash();
            hash += source.hash()*31;
            hashed = true;
        }
        return hash;
    }
}

