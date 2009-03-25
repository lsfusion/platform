package platform.server.data.query.exprs.cases;

import platform.server.where.Where;
import platform.server.data.query.exprs.AndExpr;

import java.util.Map;

public class MapCaseList<K> extends CaseList<Map<K, AndExpr>,MapCase<K>> {

    MapCaseList() {
    }
    MapCaseList(Where where, Map<K, AndExpr> data) {
        super(where, data);
    }

    MapCase<K> create(Where where, Map<K, AndExpr> data) {
        return new MapCase<K>(where,data);
    }
}
