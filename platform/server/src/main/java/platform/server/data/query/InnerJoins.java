package platform.server.data.query;

import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.BaseExpr;
import platform.server.data.expr.where.EqualsWhere;
import platform.server.data.where.Where;
import platform.server.data.where.DNFWheres;
import platform.base.BaseUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class InnerJoins extends DNFWheres<InnerWhere, InnerJoins> {

    protected boolean privateWhere() {
        return false;
    }
    protected InnerJoins createThis() {
        return new InnerJoins();
    }

    public static class Entry {
        public InnerWhere mean;
        public Where where;

        public Entry(InnerWhere iMean, Where iWhere) {
            mean = iMean;
            where = iWhere;
        }
    }
    
    // компилирует запрос на выполнение группируя means'ы, отдельно means по-любому нельзя так как JoinSelect спрячется за not,
    // который потом может уйти в followFalse, а JoinSelect так и останется в Where торчать и не хватит ключа
    // а так у нас есть гарантия что ключей хватит
    public Collection<Entry> compileMeans() {
//        return this;
        // все образуют граф means'ов
        // находим "верхние" вершины - разбивая на компоненты, и соответственно or'им (все Ji.FF(вершина) and Wi) (все можно не FF все равно явно нету)
        Collection<Entry> result = new ArrayList<Entry>();
        for(int i=0;i<size;i++) {
            Where where = getValue(i);
            InnerWhere mean = getKey(i);//where.getInnerJoins().singleKey(); // чтобы не было избыточных join'ов objects

            boolean found = false;
            // ищем кого-нибудь кого он means
            for(Entry resultJoin : result)
                if(mean.means(resultJoin.mean)) {
                    resultJoin.where = resultJoin.where.or(where); //.and(whereJoin.mean.followFalse(resultJoin.mean.not())
                    found = true;
                }
            if(!found) {
                // ищем все кто его means и удаляем
                for(Iterator<Entry> it = result.iterator();it.hasNext();) {
                    Entry resultJoin = it.next();
                    if(resultJoin.mean.means(mean)) {
                        where = where.or(resultJoin.where);
                        it.remove();
                    }
                }
                result.add(new Entry(mean,where));
            }
        }

        // упакуем entry
        for(Iterator<Entry> it = result.iterator();it.hasNext();) {
            Entry resultJoin = it.next();
            resultJoin.where = resultJoin.where.pack();
            if(resultJoin.where.isFalse())
                it.remove();
        }
        
        return result;
    }

    public InnerJoins() {
    }

    private InnerJoins(InnerWhere inner, Where where) {
        super(inner, where);
    }

    public InnerJoins(Where where) {
        this(new InnerWhere(),where);
    }

    public InnerJoins(InnerJoin join,Where where) {
        this(new InnerWhere(join),where);
    }

    public InnerJoins(KeyExpr key, BaseExpr expr) {
        this(new InnerWhere(key,expr),new EqualsWhere(key,expr));
        assert !expr.hasKey(key);
    }
}
