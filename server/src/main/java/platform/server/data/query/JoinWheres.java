package platform.server.data.query;

import platform.server.where.Where;

import java.util.ArrayList;
import java.util.Iterator;

// нужен для "расщепления" Where на типы Join'ов,равенств ключей и остальных Where
public class JoinWheres extends ArrayList<JoinWhereEntry> {

    static boolean MEANS = true;

    public void or(JoinWheres joins) {
        for(JoinWhereEntry whereJoin : joins)
            or(whereJoin.join,whereJoin.where);
    }

    void or(Where join,Where where) {
        // ищем одинаковое
        for(JoinWhereEntry whereJoin : this)
            if(whereJoin.join.hashEquals(join)) {
                whereJoin.where = whereJoin.where.or(where);
                return;
            }
        add(new JoinWhereEntry(join,where));
    }

    // компилирует запрос на выполнение группируя means'ы, отдельно means по-любому нельзя так как Join спрячется за not,
    // который потом может уйти в followFalse, а Join так и останется в Where торчать и не хватит ключа
    // а так у нас есть гарантия что ключей хватит
    JoinWheres compileMeans() {
//        return this;
        // все образуют граф means'ов
        // находим "верхние" вершины - разбивая на компоненты, и соответственно or'им (все Ji.FF(вершина) and Wi) (все можно не FF все равно явно нету)
        JoinWheres result = new JoinWheres();
        for(JoinWhereEntry whereJoin : this) {
            boolean found = false;
            // ищем кого-нибудь кого он means
            for(JoinWhereEntry resultJoin : result)
                if(whereJoin.join.means(resultJoin.join)) {
                    resultJoin.where = resultJoin.where.or(whereJoin.where.and(whereJoin.join.followFalse(resultJoin.join.not())));
                    found = true;
                }
            if(!found) {
                // ищем все кто его means и удаляем
                Where where = whereJoin.where;
                for(Iterator<JoinWhereEntry> i = result.iterator();i.hasNext();) {
                    JoinWhereEntry resultJoin = i.next();
                    if(resultJoin.join.means(whereJoin.join)) {
                        where = where.or(resultJoin.where.and(resultJoin.join.followFalse(whereJoin.join.not())));
                        i.remove();
                    }
                }
                result.add(new JoinWhereEntry(whereJoin.join,where));
            }
        }
        return result;
    }

    public JoinWheres() {
    }

    public JoinWheres(Where join,Where where) {
        add(new JoinWhereEntry(join,where));
    }

    public JoinWheres and(JoinWheres joins) {
        JoinWheres result = new JoinWheres();
        // берем все пары joins'ов
        for(JoinWhereEntry whereJoin1 : this)
            for(JoinWhereEntry whereJoin2 : joins) {
                Where andJoin = whereJoin1.join.and(whereJoin2.join);
                Where andWhere = whereJoin1.where.and(whereJoin2.where).followFalse(andJoin.not());
                if(!andWhere.isFalse())
                    result.or(andJoin, andWhere);
            }
        return result;
    }
}
