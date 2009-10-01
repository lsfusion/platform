package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;

import java.util.Collection;

public class SumGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    public SumGroupProperty(String iSID, Collection<GroupPropertyInterface<T>> iInterfaces, Property<T> iProperty) {
        super(iSID, iInterfaces, iProperty, 1);
    }

    SourceExpr getChangedExpr(SourceExpr changedExpr, SourceExpr changedPrevExpr, SourceExpr prevExpr, SourceExpr newExpr) {
        return changedExpr.sum(changedPrevExpr.scale(-1)).sum(prevExpr);
    }

    @Override
    protected boolean usePrevious() {
        return false;
    }
}
