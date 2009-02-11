package platformlocal;

import java.util.ArrayList;
import java.util.Iterator;

class JoinWhereEntry {
    Where join;
    Where where;

    JoinWhereEntry(Where iJoin, Where iWhere) {
        join = iJoin;
        where = iWhere;
    }
}

// нужен для "расщепления" Where на типы Join'ов,равенств ключей и остальных Where
class JoinWheres extends ArrayList<JoinWhereEntry> {

    static boolean MEANS = true;

    void or(JoinWheres joins) {
        for(JoinWhereEntry whereJoin : joins)
            or(whereJoin.join,whereJoin.where);
    }

    void or(Where join,Where where) {
        if(!where.isFalse()) {
            // ищем одинаковое
            for(JoinWhereEntry whereJoin : this)
                if(whereJoin.join.hashEquals(join)) {
                    whereJoin.where = whereJoin.where.or(where);
                    return;
                }

            if(MEANS) {
                // ищем кого-нибудь кого он means
                for(JoinWhereEntry whereJoin : this)
                    if(join.means(whereJoin.join)) {
                        whereJoin.where = whereJoin.where.or(where.and(join.followFalse(whereJoin.join.not())));
                        return;
                    }
                // ищем все кто его means и удаляем
                for(Iterator<JoinWhereEntry> i = iterator();i.hasNext();) {
                    JoinWhereEntry whereJoin = i.next();
                    if(whereJoin.join.means(join)) {
                        where = where.or(whereJoin.where.and(whereJoin.join.followFalse(join.not())));
                        i.remove();
                    }                    
                }
            }
            add(new JoinWhereEntry(join,where));
        }
    }

    JoinWheres() {
    }

    JoinWheres(Where join,Where where) {
        add(new JoinWhereEntry(join,where));
    }

    JoinWheres and(JoinWheres joins) {
        JoinWheres result = new JoinWheres();
        // берем все пары joins'ов
        for(JoinWhereEntry whereJoin1 : this)
            for(JoinWhereEntry whereJoin2 : joins)
                result.or(whereJoin1.join.and(whereJoin2.join),whereJoin1.where.and(whereJoin2.where));
        return result;
    }
}
