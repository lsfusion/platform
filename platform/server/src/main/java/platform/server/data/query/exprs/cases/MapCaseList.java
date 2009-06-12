package platform.server.data.query.exprs.cases;

import platform.server.data.query.exprs.AndExpr;
import platform.server.where.Where;

import java.util.Map;

public class MapCaseList<K> extends AddCaseList<Map<K, AndExpr>,MapCase<K>> {

    MapCaseList() {
    }

    // добавляет Case, проверяя все что можно
    public void add(Where where,Map<K,AndExpr> map) {

        where = where.followFalse(upWhere);
        if(!where.isFalse()) {
            MapCase<K> lastCase = size()>0?get(size()-1):null;
            if(lastCase!=null && lastCase.data.equals(map)) // заOr'им
                lastCase.where = lastCase.where.or(where);
            else
                add(new MapCase<K>(where, map));
            upWhere = upWhere.or(where);
        }
    }
}
