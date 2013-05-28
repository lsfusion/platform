package platform.server.data.expr.where.cases;

import platform.base.col.interfaces.immutable.ImMap;
import platform.server.data.expr.BaseExpr;
import platform.server.data.where.classes.ClassExprWhere;

public interface CaseClassWhere<K,V> {
    V getCaseClassWhere(ImMap<K, BaseExpr> mapCase, ClassExprWhere caseClassWhere);
}
