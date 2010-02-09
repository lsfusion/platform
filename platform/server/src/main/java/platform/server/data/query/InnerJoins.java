package platform.server.data.query;

import platform.base.BaseUtils;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.where.Where;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class InnerJoins extends ArrayList<InnerJoins.Entry> {

    static final boolean MEANS = true;

    public void or(InnerJoins joins) {
        for(Entry whereJoin : joins)
            or(whereJoin.mean,whereJoin.where);
    }

    private void or(InnerWhere join, Where where) {
        // ищем одинаковое
        for(Entry whereJoin : this)
            if(BaseUtils.hashEquals(whereJoin.mean,join)) {
                whereJoin.where = whereJoin.where.or(where);
                return;
            }
        add(new Entry(join,where));
    }

    // компилирует запрос на выполнение группируя means'ы, отдельно means по-любому нельзя так как JoinSelect спрячется за not,
    // который потом может уйти в followFalse, а JoinSelect так и останется в Where торчать и не хватит ключа
    // а так у нас есть гарантия что ключей хватит
    public Collection<Entry> compileMeans() {
//        return this;
        // все образуют граф means'ов
        // находим "верхние" вершины - разбивая на компоненты, и соответственно or'им (все Ji.FF(вершина) and Wi) (все можно не FF все равно явно нету)
        Collection<Entry> result = new ArrayList<Entry>();
        for(Entry whereJoin : this) {
            boolean found = false;
            // ищем кого-нибудь кого он means
            for(Entry resultJoin : result)
                if(whereJoin.mean.means(resultJoin.mean)) {
                    resultJoin.where = resultJoin.where.or(whereJoin.where); //.and(whereJoin.mean.followFalse(resultJoin.mean.not())
                    found = true;
                }
            if(!found) {
                // ищем все кто его means и удаляем
                Where where = whereJoin.where;
                for(Iterator<Entry> i = result.iterator();i.hasNext();) {
                    Entry resultJoin = i.next();
                    if(resultJoin.mean.means(whereJoin.mean)) {
                        where = where.or(resultJoin.where);
                        i.remove();
                    }
                }
                result.add(new Entry(whereJoin.mean,where));
            }
        }
        return result;
    }

    public InnerJoins() {
    }

    private InnerJoins(InnerWhere inner, Where where) {
        add(new Entry(inner,where));
    }

    public InnerJoins(Where where) {
        this(new InnerWhere(),where);
    }
    
    public InnerJoins(InnerJoin join,Where where) {
        this(new InnerWhere(join),where);
    }

    public InnerJoins(KeyExpr key, BaseExpr expr) {
        this(new InnerWhere(key,expr),new EqualsWhere(key,expr));
        assert expr.isValue();
    }

    public InnerJoins and(InnerJoins joins) {
        InnerJoins result = new InnerJoins();
        // берем все пары joins'ов
        for(Entry whereJoin1 : this)
            for(Entry whereJoin2 : joins) {
                InnerWhere andJoin = whereJoin1.mean.and(whereJoin2.mean); Where andWhere;
                if(!(andJoin==null || (andWhere=whereJoin1.where.and(whereJoin2.where)).isFalse())) // тут isFalse'а достаточно так как AB=>!(IaIb) <=> ABIaIb==FALSE, A=>Ia,B=>Ib <=> AB==FALSE
                    result.or(andJoin, andWhere);
            }
        return result;
    }

    public static class Entry {
        public InnerWhere mean;
        public Where where;

        public Entry(InnerWhere iMean, Where iWhere) {
            mean = iMean;
            where = iWhere;
        }
    }
}
