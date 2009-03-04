package platform.server.data.query.exprs;

import platform.server.where.Where;

import java.util.ArrayList;

abstract class CaseList<D,C extends Case<D>> extends ArrayList<C> {

    CaseList() {
    }
    CaseList(D data) {
        add(create(Where.TRUE,data));
    }
    CaseList(Where where,D data) {
        add(create(where, data));
        lastWhere = where;
    }
    CaseList(Where where,D dTrue,D dFalse) {
        super();
        add(create(where,dTrue));
        add(create(Where.TRUE,dFalse));
    }
    CaseList(Where falseWhere) {
        prevUpWhere = falseWhere;
    }

    Where prevUpWhere = Where.FALSE;
    Where lastWhere = null;
    Where getUpWhere() { // для оптимизации последних элементов
        if(lastWhere !=null) {
            prevUpWhere = prevUpWhere.or(lastWhere);
            lastWhere = null;
        }

        return prevUpWhere;
    }

    D followWhere(Where where, D data, Where upWhere) {
        return data;
    }

    // добавляет Case, проверяя все что можно
    void add(Where where,D data) {

        Where upWhere = getUpWhere();
        where = where.followFalse(upWhere);
        if(!where.isFalse()) {
            data = followWhere(where,data,upWhere);

            C lastCase = size()>0?get(size()-1):null;
            if(lastCase!=null && lastCase.data.equals(data)) // заOr'им
                lastCase.where = lastCase.where.or(where);
            else
                add(create(where, data));
            lastWhere = where;
        }
    }

    public Where getWhere(CaseWhere<C> caseInterface) {

        Where result = Where.FALSE;
        Where up = Where.FALSE;
        for(C cCase : this) {
            Where CaseWhere = caseInterface.getCaseWhere(cCase);
            result = result.or(cCase.where.and(CaseWhere).and(up.not()));
            up = up.or(cCase.where);
        }

        return result;
    }

    abstract C create(Where Where,D Data);
}
