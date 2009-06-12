package platform.server.data.query.exprs.cases;

import platform.server.data.classes.where.ClassExprWhere;
import platform.server.data.query.exprs.AndExpr;

import java.util.Map;

public interface CaseClassWhere<K,V> {
    V getCaseClassWhere(Map<K, AndExpr> mapCase, ClassExprWhere caseClassWhere);
}
