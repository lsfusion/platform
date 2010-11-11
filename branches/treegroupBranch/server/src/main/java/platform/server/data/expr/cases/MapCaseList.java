package platform.server.data.expr.cases;

import platform.base.BaseUtils;
import platform.server.data.expr.BaseExpr;
import platform.server.data.where.Where;

import java.util.Map;

public class MapCaseList<K> extends AddCaseList<Map<K, BaseExpr>,MapCase<K>> {

    MapCaseList() {
    }

    // добавляет Case, проверяя все что можно
    public void add(Where where,Map<K, BaseExpr> map) {

        where = where.followFalse(upWhere);
        if(!where.isFalse()) {
            MapCase<K> lastCase = size()>0?get(size()-1):null;
            if(lastCase!=null && BaseUtils.hashEquals(lastCase.data,map)) // заOr'им
                lastCase.where = lastCase.where.or(where);
            else
                add(new MapCase<K>(where, map));
            upWhere = upWhere.or(where);
        }
    }
}
