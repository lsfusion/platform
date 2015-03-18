package lsfusion.server.data.expr;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;

public interface NotNullExprInterface {

    ImSet<NotNullExprInterface> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive);

    void fillFollowSet(MSet<DataWhere> result);

    boolean hasNotNull();
    Where getNotNullWhere();
}
