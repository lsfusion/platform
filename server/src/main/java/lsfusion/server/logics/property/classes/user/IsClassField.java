package lsfusion.server.logics.property.classes.user;

import lsfusion.server.data.PropertyField;
import lsfusion.server.data.expr.BaseExpr;
import lsfusion.server.data.expr.SingleClassExpr;
import lsfusion.server.data.where.Where;
import lsfusion.server.logics.classes.user.ObjectValueClassSet;

public interface IsClassField {

    PropertyField getField();

    BaseExpr getFollowExpr(BaseExpr joinExpr);

    Where getIsClassWhere(SingleClassExpr expr, ObjectValueClassSet set, boolean inconsistent);
}
