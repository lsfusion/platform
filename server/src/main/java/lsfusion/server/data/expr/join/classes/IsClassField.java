package lsfusion.server.data.expr.join.classes;

import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.classes.IsClassType;
import lsfusion.server.data.expr.classes.SingleClassExpr;
import lsfusion.server.data.table.PropertyField;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;

public interface IsClassField {

    PropertyField getField();

    BaseExpr getFollowExpr(BaseExpr joinExpr);

    Where getIsClassWhere(SingleClassExpr expr, ObjectValueClassSet set, IsClassType type);
}
