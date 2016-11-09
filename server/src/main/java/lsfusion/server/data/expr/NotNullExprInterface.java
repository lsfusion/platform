package lsfusion.server.data.expr;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.server.data.where.DataWhere;
import lsfusion.server.data.where.Where;

public interface NotNullExprInterface {

    ImSet<NotNullExprInterface> getExprFollows(boolean includeInnerWithoutNotNull, boolean recursive);

    boolean hasNotNull(); // используется именно для Follow, assert что getNotNullWhere будет DataWhere (см. реализацию fillFollowSet)
    void fillFollowSet(MSet<DataWhere> result); // default реализация есть

    Where getNotNullWhere();
}
