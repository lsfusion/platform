package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;

import java.util.Collection;

public class SumGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    public SumGroupProperty(String sID, String caption, Collection<GroupPropertyInterface<T>> interfaces, Property<T> property) {
        super(sID, caption, interfaces, property, 1);
    }

    SourceExpr getChangedExpr(SourceExpr changedExpr, SourceExpr changedPrevExpr, SourceExpr prevExpr, SourceExpr newExpr) {
        return changedExpr.sum(changedPrevExpr.scale(-1)).sum(prevExpr);
    }
}
