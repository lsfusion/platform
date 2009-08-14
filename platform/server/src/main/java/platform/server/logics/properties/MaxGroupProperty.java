package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;
import platform.interop.Compare;

import java.util.Collection;

public class MaxGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    public MaxGroupProperty(String iSID, Collection<GroupPropertyInterface<T>> iInterfaces, Property<T> iProperty) {
        super(iSID, iInterfaces, iProperty, 0);
    }

    SourceExpr getChangedExpr(SourceExpr changedExpr, SourceExpr changedPrevExpr, SourceExpr prevExpr, SourceExpr newExpr) {
//        return newExpr;
        return changedExpr.ifElse(changedExpr.greater(prevExpr), newExpr.ifElse(changedPrevExpr.compare(prevExpr,Compare.EQUALS), prevExpr));
    }
}
