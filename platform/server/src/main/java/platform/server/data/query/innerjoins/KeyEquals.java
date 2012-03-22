package platform.server.data.query.innerjoins;

import platform.base.BaseUtils;
import platform.base.Pair;
import platform.base.QuickMap;
import platform.base.QuickSet;
import platform.interop.Compare;
import platform.server.Settings;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.translator.PartialQueryTranslator;
import platform.server.data.where.Where;

import java.util.*;

public class KeyEquals extends QuickMap<KeyEqual, Where> {

    public KeyEquals() {
    }

    public KeyEquals(Where where) {
        super(new KeyEqual(), where);
        assert !where.isFalse();
    }

    public KeyEquals(KeyExpr key, BaseExpr expr) {
        super(new KeyEqual(key, expr), expr.getWhere());
    }

    protected Where addValue(KeyEqual key, Where prevValue, Where newValue) {
        return prevValue.or(newValue);
    }

    protected KeyEquals createThis() {
        return new KeyEquals();
    }

    public KeyEquals and(KeyEquals joins) {
        KeyEquals result = new KeyEquals();
        // берем все пары joins'ов
        for(int i1=0;i1<size;i1++)
            for(int i2=0;i2<joins.size;i2++) {
                KeyEqual eq1 = getKey(i1);
                KeyEqual eq2 = joins.getKey(i2);

                Where where1 = getValue(i1);
                Where where2 = joins.getValue(i2);

                // сначала определяем общие ключи
                Map<KeyExpr, BaseExpr> diffEq1 = new HashMap<KeyExpr, BaseExpr>(); Map<KeyExpr, BaseExpr> diffEq2 = new HashMap<KeyExpr, BaseExpr>();
                Map<KeyExpr, BaseExpr> sameEq1 = BaseUtils.splitKeys(eq1.keyExprs, eq2.keyExprs.keySet(), diffEq1);
                Map<KeyExpr, BaseExpr> sameEq2 = BaseUtils.splitKeys(eq2.keyExprs, eq1.keyExprs.keySet(), diffEq2);

                // транслируем правые левыми
                Map<KeyExpr, Expr> cleanEq1 = new HashMap<KeyExpr, Expr>();// складываются "очищенные" equals'ы
                Map<KeyExpr, Expr> transEq1 = new PartialQueryTranslator(diffEq2).translate(diffEq1);
                // погнали топологическую сортировку отсеивая Expr'ы
                while(!transEq1.isEmpty()) {
                    boolean found = false;
                    Iterator<Map.Entry<KeyExpr,Expr>> it = transEq1.entrySet().iterator();
                    while(it.hasNext()) {
                        Map.Entry<KeyExpr,Expr> keyEq = it.next();
                        QuickSet<KeyExpr> enumKeys = keyEq.getValue().getOuterKeys();
                        if(enumKeys.disjoint(transEq1.keySet())) {// если не зависит от остальных
                            cleanEq1.put(keyEq.getKey(),keyEq.getValue().translateQuery(new PartialQueryTranslator(cleanEq1))); // транслэйтим clean'ами
                            it.remove();
                            found = true; 
                            break;
                        }
                    }

                    if(!found) { // значит остались циклы, берем любую и перекидываем в where
                        Map.Entry<KeyExpr, Expr> cycle = transEq1.entrySet().iterator().next();
                        where1 = where1.and(cycle.getKey().compare(cycle.getValue(), Compare.EQUALS));
                    }
                }

                // второй просто транслируем первым
                PartialQueryTranslator cleanTranslator1 = new PartialQueryTranslator(cleanEq1);
                Map<KeyExpr, Expr> cleanEq2 = cleanTranslator1.translate(diffEq2);
                PartialQueryTranslator cleanTranslator2 = new PartialQueryTranslator(cleanEq2);
                Where andWhere = where2.translateQuery(cleanTranslator1).and(where1.translateQuery(cleanTranslator2));

                // сливаем same'ы, их также надо translateOuter'ить так как могут быть несвободными от противоположных ключей
                Where extraWhere = Where.TRUE;
                Map<KeyExpr,Expr> mergeSame = new HashMap<KeyExpr,Expr>(cleanTranslator2.translate(sameEq1));
                for(Map.Entry<KeyExpr,Expr> andKeyExpr : cleanTranslator1.translate(sameEq2).entrySet()) {
                    Expr expr = mergeSame.get(andKeyExpr.getKey());
                    if(!expr.isValue()) // предпочитаем статичные значение
                        mergeSame.put(andKeyExpr.getKey(),andKeyExpr.getValue());
                    if(!BaseUtils.hashEquals(expr, andKeyExpr.getValue())) // закидываем compare
                        extraWhere = extraWhere.and(expr.compare(andKeyExpr.getValue(), Compare.EQUALS));
                }

                Map<KeyExpr,BaseExpr> andEq = new HashMap<KeyExpr, BaseExpr>();
                for(Map.Entry<KeyExpr,Expr> mergeEntry : BaseUtils.merge(mergeSame,BaseUtils.merge(cleanEq1, cleanEq2)).entrySet()) // assertion что не пересекаются
                    if(mergeEntry.getValue() instanceof BaseExpr)
                        andEq.put(mergeEntry.getKey(), (BaseExpr) mergeEntry.getValue());
                    else // выкидываем Expr
                        extraWhere = extraWhere.and(mergeEntry.getKey().compare(mergeEntry.getValue(), Compare.EQUALS));

                andWhere = andWhere.and(extraWhere);
                if (cleanEq1.isEmpty() && cleanEq2.isEmpty() && extraWhere.isTrue()) { // чтобы не уходило в бесконечный цикл
                    if(!andWhere.isFalse())
                        result.add(new KeyEqual(andEq), andWhere);
                } else {
                    KeyEquals recEquals = andWhere.getKeyEquals();
                    for(int i=0;i<recEquals.size;i++) {
                        KeyEqual recEqual = recEquals.getKey(i);
                        result.add(new KeyEqual((Map<KeyExpr, BaseExpr>) (Object) BaseUtils.merge(recEqual.getTranslator().translate(andEq), recEqual.keyExprs)), recEquals.getValue(i));
                    }
                }
            }
        return result;
    }
    

    protected boolean containsAll(Where who, Where what) {
        throw new RuntimeException("not supported yet");
    }

    // получает Where с учетом трансляций
    private Where getWhere() {
        Where where = Where.FALSE;
        for(int i=0;i<size;i++)
           where = where.or(getValue(i).and(getKey(i).getWhere()));
        return where;
    }

    public <K extends BaseExpr> Collection<GroupJoinsWhere> getWhereJoins(QuickSet<K> keepStat, List<Expr> orderTop) {
        Collection<GroupJoinsWhere> result = new ArrayList<GroupJoinsWhere>();
        for(int i=0;i<size;i++) {
            KeyEqual keyEqual = getKey(i); // keyEqual закидывается в статистику так как keepStat не всегда translate'ся
            Where where = getValue(i);
            where.groupJoinsWheres(keepStat, keyEqual.getKeyStat(where), orderTop).compileMeans().fillList(keyEqual, result);
        }
        return result;
    }

    private static <T extends GroupWhere> long getComplexity(List<T> statJoins) {
        long prev = 0;
        long result = 0;
        for(T statJoin : statJoins) {
            prev += statJoin.where.getComplexity(true);
            result += prev;
        }
        return result;
    }

    //по аналогии с GroupStatType, сливает одинаковые
    private Collection<GroupJoinsWhere> merge(Collection<GroupJoinsWhere> whereJoins, GroupJoinsWhere join) {
        Collection<GroupJoinsWhere> result = new ArrayList<GroupJoinsWhere>(whereJoins);
        for(Iterator<GroupJoinsWhere> i=result.iterator();i.hasNext();) {
            GroupJoinsWhere where = i.next();
            if(where.keyEqual.equals(join.keyEqual) && where.joins.equals(join.joins)) {
                i.remove();
                join = new GroupJoinsWhere(join.keyEqual, join.joins, join.joins.orUpWheres(join.upWheres, where.upWheres), join.where.or(where.where));
                break;
            }
        }
        result.add(join);
        return result;
    }

    public <K extends BaseExpr> Pair<Collection<GroupJoinsWhere>, Boolean> getWhereJoins(boolean tryExclusive, QuickSet<K> keepStat, List<Expr> orderTop) {
        Collection<GroupJoinsWhere> whereJoins = getWhereJoins(keepStat, orderTop);
        if(!tryExclusive || whereJoins.size()<=1 || whereJoins.size() > Settings.instance.getLimitExclusiveCount())
            return new Pair<Collection<GroupJoinsWhere>, Boolean>(whereJoins, false);
        List<GroupJoinsWhere> sortedWhereJoins = GroupWhere.sort(whereJoins);
        long sortedComplexity = getComplexity(sortedWhereJoins);
        if(sortedComplexity > Settings.instance.getLimitExclusiveComplexity())
            return new Pair<Collection<GroupJoinsWhere>, Boolean>(whereJoins, false);

        // если сложность превышает порог - просто andNot'им верхние
        if(sortedWhereJoins.size() > Settings.instance.getLimitExclusiveSimpleCount() || sortedComplexity > Settings.instance.getLimitExclusiveSimpleComplexity()) {
            Collection<GroupJoinsWhere> exclJoins = new ArrayList<GroupJoinsWhere>();
            Where prevWhere = Where.FALSE;
            for(GroupJoinsWhere whereJoin : sortedWhereJoins) {
                exclJoins.add(new GroupJoinsWhere(whereJoin.keyEqual, whereJoin.joins, whereJoin.upWheres, whereJoin.where.and(prevWhere.not())));
                prevWhere.or(whereJoin.getFullWhere());
            }
            return new Pair<Collection<GroupJoinsWhere>, Boolean>(exclJoins, true);
        } else { // иначе запускаем рекурсию
            GroupJoinsWhere firstJoin = sortedWhereJoins.iterator().next();
            Pair<Collection<GroupJoinsWhere>, Boolean> recWhereJoins = getWhere().and(firstJoin.getFullWhere().not()).getWhereJoins(true, keepStat, orderTop);
            return new Pair<Collection<GroupJoinsWhere>, Boolean>(merge(recWhereJoins.first, firstJoin), recWhereJoins.second); // assert что keyEquals.getWhere тоже самое что this только упрощенное транслятором
        }
    }

    public <K extends BaseExpr> Collection<GroupStatWhere<K>> getStatJoins(QuickSet<K> keepStat) {
        Collection<GroupStatWhere<K>> statJoins = new ArrayList<GroupStatWhere<K>>();
        for(GroupJoinsWhere whereJoin : getWhereJoins(keepStat, new ArrayList<Expr>()))
            statJoins.add(new GroupStatWhere<K>(whereJoin.keyEqual, whereJoin.getStatKeys(keepStat), whereJoin.where));
        return statJoins;
    }

    public <K extends BaseExpr> Collection<GroupStatWhere<K>> getStatJoins(boolean exclusive, QuickSet<K> keepStat, GroupStatType type, boolean noWhere) {
        assert !(exclusive && noWhere); // если noWhere то не exclusive
        // получаем GroupJoinsWhere, конвертим в GroupStatWhere, группируем по type'у (через MapWhere), упорядочиваем по complexity
        Collection<GroupStatWhere<K>> statJoins = type.group(getStatJoins(keepStat));
        if(!exclusive || statJoins.size()<=1) // если не нужно notExclusive || один элемент
            return statJoins;

        List<GroupStatWhere<K>> sortedStatJoins = GroupWhere.sort(statJoins);
        if(sortedStatJoins.size() > Settings.instance.getLimitExclusiveSimpleCount() || getComplexity(sortedStatJoins) > Settings.instance.getLimitExclusiveSimpleComplexity()) { // если сложность превышает порог - просто andNot'им верхние
            Collection<GroupStatWhere<K>> exclJoins = new ArrayList<GroupStatWhere<K>>();
            Where prevWhere = Where.FALSE;
            for(GroupStatWhere<K> statJoin : sortedStatJoins) {
                exclJoins.add(new GroupStatWhere<K>(statJoin.keyEqual, statJoin.stats, statJoin.where.and(prevWhere.not())));
                prevWhere.or(statJoin.getFullWhere());
            }
            return exclJoins;
        } else { // иначе запускаем рекурсию
            GroupStatWhere<K> firstJoin = sortedStatJoins.iterator().next();
            return type.merge(getWhere().and(firstJoin.getFullWhere().not()).getStatJoins(keepStat, exclusive, type, noWhere), firstJoin); // assert что keyEquals.getWhere тоже самое что this только упрощенное транслятором
        }
    }
}
