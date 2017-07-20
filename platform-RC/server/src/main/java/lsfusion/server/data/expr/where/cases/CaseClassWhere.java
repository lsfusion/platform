package lsfusion.server.data.expr.where.cases;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.where.classes.ClassExprWhere;

public interface CaseClassWhere<K,V> {
    V getCaseClassWhere(ImMap<K, BaseExpr> mapCase, ClassExprWhere caseClassWhere);
}
