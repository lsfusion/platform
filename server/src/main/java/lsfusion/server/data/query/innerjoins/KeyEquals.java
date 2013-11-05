package lsfusion.server.data.query.innerjoins;

import lsfusion.base.BaseUtils;
import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.ListFact;
import lsfusion.base.col.MapFact;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.WrapMap;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MCol;
import lsfusion.base.col.interfaces.mutable.MExclMap;
import lsfusion.base.col.interfaces.mutable.MMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetKeyValue;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.interop.Compare;
import lsfusion.server.Settings;
import lsfusion.server.caches.ParamExpr;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.translator.MapTranslate;
import lsfusion.server.data.translator.PartialQueryTranslator;
import lsfusion.server.data.where.AbstractWhere;
import lsfusion.server.data.where.Where;

import java.util.Collection;
import java.util.Iterator;

public class KeyEquals extends WrapMap<KeyEqual, Where> {

    public final boolean isSimple; // сделано через поле, а не проверкой на один пустой KeyEquals, потому как из-за перетрансляций в and'е может упростить условие, а keyEquals останется один
    public KeyEquals(ImMap<KeyEqual, Where> map, boolean isSimple) {
        super(map);

        this.isSimple = isSimple;
    }

    public final static KeyEquals EMPTY = new KeyEquals(MapFact.<KeyEqual, Where>EMPTY(), false);

    public KeyEquals(Where where, boolean isSimple) {
        super(KeyEqual.EMPTY, where);
        assert !where.isFalse();

        this.isSimple = isSimple;
    }

    public KeyEquals(ParamExpr key, BaseExpr expr) {
        super(new KeyEqual(key, expr), expr.getWhere());

        isSimple = false;
    }

    public KeyEquals and(KeyEquals joins) {
        MMap<KeyEqual, Where> result = MapFact.mMap(AbstractWhere.<KeyEqual>addOr());
        // берем все пары joins'ов
        for(int i1=0,size1=size();i1<size1;i1++)
            for(int i2=0,size2=joins.size();i2<size2;i2++) {
                KeyEqual eq1 = getKey(i1);
                KeyEqual eq2 = joins.getKey(i2);

                Where where1 = getValue(i1);
                Where where2 = joins.getValue(i2);

                // сначала определяем общие ключи
                Result<ImMap<ParamExpr, BaseExpr>> diffEq1 = new Result<ImMap<ParamExpr, BaseExpr>>();
                Result<ImMap<ParamExpr, BaseExpr>> diffEq2 = new Result<ImMap<ParamExpr, BaseExpr>>();
                ImMap<ParamExpr, BaseExpr> sameEq1 = eq1.keyExprs.splitKeys(eq2.keyExprs.keys(), diffEq1);
                ImMap<ParamExpr, BaseExpr> sameEq2 = eq2.keyExprs.splitKeys(eq1.keyExprs.keys(), diffEq2);

                // транслируем правые левыми, погнали топологическую сортировку отсеивая Expr'ы
                java.util.Map<ParamExpr, Expr> mTransEq1 = MapFact.mAddRemoveMap(); // remove есть
                MapFact.addJavaAll(mTransEq1, new PartialQueryTranslator(diffEq2.result).translate(diffEq1.result));

                MExclMap<ParamExpr, Expr> mCleanEq1 = MapFact.mExclMap();// складываются "очищенные" equals'ы

                while(!mTransEq1.isEmpty()) {
                    boolean found = false;
                    Iterator<java.util.Map.Entry<ParamExpr,Expr>> it = mTransEq1.entrySet().iterator();
                    while(it.hasNext()) {
                        java.util.Map.Entry<ParamExpr,Expr> keyEq = it.next();
                        ImSet<ParamExpr> enumKeys = keyEq.getValue().getOuterKeys();
                        if(MapFact.disjointJava(enumKeys, mTransEq1.keySet())) {// если не зависит от остальных
                            mCleanEq1.exclAdd(keyEq.getKey(), keyEq.getValue().translateQuery(new PartialQueryTranslator(mCleanEq1.immutableCopy()))); // транслэйтим clean'ами
                            it.remove();
                            found = true; 
                            break;
                        }
                    }

                    if(!found) { // значит остались циклы, берем любую и перекидываем в where
                        java.util.Map.Entry<ParamExpr, Expr> cycle = mTransEq1.entrySet().iterator().next();
                        where1 = where1.and(cycle.getKey().compare(cycle.getValue(), Compare.EQUALS));
                    }
                }
                ImMap<ParamExpr, Expr> cleanEq1 = mCleanEq1.immutable();

                // второй просто транслируем первым
                PartialQueryTranslator cleanTranslator1 = new PartialQueryTranslator(cleanEq1);
                ImMap<ParamExpr, Expr> cleanEq2 = cleanTranslator1.translate(diffEq2.result);
                PartialQueryTranslator cleanTranslator2 = new PartialQueryTranslator(cleanEq2);
                Where andWhere = where2.translateQuery(cleanTranslator1).and(where1.translateQuery(cleanTranslator2));

                // сливаем same'ы, их также надо translateOuter'ить так как могут быть несвободными от противоположных ключей
                ImMap<ParamExpr, Expr> transSameEq1 = cleanTranslator2.translate(sameEq1);
                ImMap<ParamExpr, Expr> transSameEq2 = cleanTranslator1.translate(sameEq2);

                assert BaseUtils.hashEquals(sameEq1.keys(), sameEq2.keys());
                Where extraWhere = Where.TRUE;
                for(int i=0,size=transSameEq2.size();i<size;i++) {
                    ParamExpr key = transSameEq2.getKey(i);
                    Expr value2 = transSameEq2.getValue(i);
                    Expr value1 = transSameEq1.get(key);
                    if(!BaseUtils.hashEquals(value1, value2)) // закидываем compare
                        extraWhere = extraWhere.and(value1.compare(value2, Compare.EQUALS));
                }
                ImMap<ParamExpr, Expr> mergeSame = transSameEq1.merge(transSameEq2, KeyEqual.keepValue()); // предпочитаем статичные значение

                ImMap<ParamExpr, Expr> mergeKeys = mergeSame.addExcl(cleanEq1.addExcl(cleanEq2));

                Result<ImMap<ParamExpr, Expr>> notBaseExprs = new Result<ImMap<ParamExpr, Expr>>();
                ImMap<ParamExpr, BaseExpr> andEq = BaseUtils.immutableCast(mergeKeys.splitKeys(new GetKeyValue<Boolean, ParamExpr, Expr>() {
                    public Boolean getMapValue(ParamExpr key, Expr value) {
                        return value instanceof BaseExpr;
                    }}, notBaseExprs));
                for(int i=0,size=notBaseExprs.result.size();i<size;i++)
                    extraWhere = extraWhere.and(notBaseExprs.result.getKey(i).compare(notBaseExprs.result.getValue(i), Compare.EQUALS));

                andWhere = andWhere.and(extraWhere);
                if (cleanEq1.isEmpty() && cleanEq2.isEmpty() && extraWhere.isTrue()) { // чтобы не уходило в бесконечный цикл
                    if(!andWhere.isFalse())
                        result.add(new KeyEqual(andEq), andWhere);
                } else {
                    KeyEquals recEquals = andWhere.getKeyEquals();
                    for(int i=0,recSize=recEquals.size();i<recSize;i++) {
                        KeyEqual recEqual = recEquals.getKey(i);
                        result.add(new KeyEqual((ImMap) (Object) recEqual.getTranslator().translate(andEq).addExcl(recEqual.keyExprs)), recEquals.getValue(i));
                    }
                }
            }
        return new KeyEquals(result.immutable(), false); // потому что вызывается из calculateGroupKeyEquals, а там уже проверили что все не isSimple
    }

    // получает Where с учетом трансляций
    private Where getWhere() {
        Where where = Where.FALSE;
        for(int i=0,size=size();i<size;i++)
           where = where.or(getValue(i).and(getKey(i).getWhere()));
        return where;
    }

    public <K extends BaseExpr> ImCol<GroupJoinsWhere> getWhereJoins(ImSet<K> keepStat, ImOrderSet<Expr> orderTop, GroupJoinsWheres.Type type) {
        MCol<GroupJoinsWhere> result = ListFact.mCol();
        for(int i=0,size=size();i<size;i++) {
            KeyEqual keyEqual = getKey(i); // keyEqual закидывается в статистику так как keepStat не всегда translate'ся
            Where where = getValue(i);
            where.groupJoinsWheres(keepStat, keyEqual.getKeyStat(where), orderTop, type).compileMeans().fillList(keyEqual, result);
        }
        return result.immutableCol();
    }

    private static <T extends GroupWhere> long getComplexity(ImList<T> statJoins) {
        long prev = 0;
        long result = 0;
        for(T statJoin : statJoins) {
            prev += statJoin.where.getComplexity(true);
            result += prev;
        }
        return result;
    }

    //по аналогии с GroupStatType, сливает одинаковые
    public static ImCol<GroupJoinsWhere> merge(ImCol<GroupJoinsWhere> whereJoins, GroupJoinsWhere join) {
        return merge(whereJoins, SetFact.singleton(join));
    }

    public static ImCol<GroupJoinsWhere> merge(ImCol<GroupJoinsWhere> whereJoins, ImCol<GroupJoinsWhere> joins) {
        Collection<GroupJoinsWhere> result = ListFact.mAddRemoveCol();
        ListFact.addJavaAll(whereJoins, result);

        for(GroupJoinsWhere join : joins) {
            for(Iterator<GroupJoinsWhere> i=result.iterator();i.hasNext();) {
                GroupJoinsWhere where = i.next();
                if(where.keyEqual.equals(join.keyEqual) && where.joins.equals(join.joins)) {
                    i.remove();
                    join = new GroupJoinsWhere(join.keyEqual, join.joins, join.joins.orUpWheres(join.upWheres, where.upWheres), join.where.or(where.where));
                    break;
                }
            }
            result.add(join);
        }
        return ListFact.fromJavaCol(result);
    }

    public <K extends BaseExpr> Pair<ImCol<GroupJoinsWhere>, Boolean> getWhereJoins(boolean tryExclusive, ImSet<K> keepStat, ImOrderSet<Expr> orderTop) {
        ImCol<GroupJoinsWhere> whereJoins = getWhereJoins(keepStat, orderTop, GroupJoinsWheres.Type.WHEREJOINS);
        if(!tryExclusive || whereJoins.size()<=1 || whereJoins.size() > Settings.get().getLimitExclusiveCount())
            return new Pair<ImCol<GroupJoinsWhere>, Boolean>(whereJoins, whereJoins.size()<=1);
        ImList<GroupJoinsWhere> sortedWhereJoins = GroupWhere.sort(whereJoins);
        long sortedComplexity = getComplexity(sortedWhereJoins);
        if(sortedComplexity > Settings.get().getLimitExclusiveComplexity())
            return new Pair<ImCol<GroupJoinsWhere>, Boolean>(whereJoins, false);

        // если сложность превышает порог - просто andNot'им верхние
        if(sortedWhereJoins.size() > Settings.get().getLimitExclusiveSimpleCount() || sortedComplexity > Settings.get().getLimitExclusiveSimpleComplexity()) {
            MCol<GroupJoinsWhere> exclJoins = ListFact.mCol(sortedWhereJoins.size()); // есть последействие
            Where prevWhere = Where.FALSE;
            for(GroupJoinsWhere whereJoin : sortedWhereJoins) {
                exclJoins.add(new GroupJoinsWhere(whereJoin.keyEqual, whereJoin.joins, whereJoin.upWheres, whereJoin.where.and(prevWhere.not())));
                prevWhere.or(whereJoin.getFullWhere());
            }
            return new Pair<ImCol<GroupJoinsWhere>, Boolean>(exclJoins.immutableCol(), true);
        } else { // иначе запускаем рекурсию
            GroupJoinsWhere firstJoin = sortedWhereJoins.iterator().next();
            Pair<ImCol<GroupJoinsWhere>, Boolean> recWhereJoins = getWhere().and(firstJoin.getFullWhere().not()).getWhereJoins(true, keepStat, orderTop);
            return new Pair<ImCol<GroupJoinsWhere>, Boolean>(merge(recWhereJoins.first, firstJoin), recWhereJoins.second); // assert что keyEquals.getWhere тоже самое что this только упрощенное транслятором
        }
    }

    public <K extends BaseExpr> ImCol<GroupStatWhere<K>> getStatJoins(final ImSet<K> keepStat, boolean noWhere) {
        return getWhereJoins(keepStat, SetFact.<Expr>EMPTYORDER(), noWhere ? GroupJoinsWheres.Type.STAT_ONLY : GroupJoinsWheres.Type.STAT_WITH_WHERE).mapColValues(new GetValue<GroupStatWhere<K>, GroupJoinsWhere>() {
            public GroupStatWhere<K> getMapValue(GroupJoinsWhere whereJoin) {
                return new GroupStatWhere<K>(whereJoin.keyEqual, whereJoin.getStatKeys(keepStat), whereJoin.where);
            }
        });
    }

    public <K extends BaseExpr> ImCol<GroupStatWhere<K>> getStatJoins(boolean exclusive, ImSet<K> keepStat, GroupStatType type, boolean noWhere) {
        assert !(exclusive && noWhere); // если noWhere то не exclusive
        // получаем GroupJoinsWhere, конвертим в GroupStatWhere, группируем по type'у (через MapWhere), упорядочиваем по complexity
        ImCol<GroupStatWhere<K>> statJoins = type.group(getStatJoins(keepStat, noWhere || type.equals(GroupStatType.ALL)), noWhere, this);
        if(!exclusive || statJoins.size()<=1) // если не нужно notExclusive || один элемент
            return statJoins;

        ImList<GroupStatWhere<K>> sortedStatJoins = GroupWhere.sort(statJoins);
        if(sortedStatJoins.size() > Settings.get().getLimitExclusiveSimpleCount() || getComplexity(sortedStatJoins) > Settings.get().getLimitExclusiveSimpleComplexity()) { // если сложность превышает порог - просто andNot'им верхние
            MCol<GroupStatWhere<K>> exclJoins = ListFact.mCol(sortedStatJoins.size());
            Where prevWhere = Where.FALSE;
            for(GroupStatWhere<K> statJoin : sortedStatJoins) {
                exclJoins.add(new GroupStatWhere<K>(statJoin.keyEqual, statJoin.stats, statJoin.where.and(prevWhere.translateQuery(statJoin.keyEqual.getTranslator()).not())));
                prevWhere = prevWhere.or(statJoin.getFullWhere());
            }
            return exclJoins.immutableCol();
        } else { // иначе запускаем рекурсию
            GroupStatWhere<K> firstJoin = sortedStatJoins.get(0);
            return type.merge(getWhere().and(firstJoin.getFullWhere().not()).getStatJoins(keepStat, exclusive, type, noWhere), firstJoin); // assert что keyEquals.getWhere тоже самое что this только упрощенное транслятором
        }
    }
    
    public KeyEquals translateOuter(MapTranslate translator) {
        return new KeyEquals(translator.translateMap(map), isSimple);
    }
}
