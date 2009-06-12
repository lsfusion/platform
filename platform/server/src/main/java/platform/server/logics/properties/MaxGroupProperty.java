package platform.server.logics.properties;

import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.cases.CaseExpr;

import java.util.Collection;

public class MaxGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    public MaxGroupProperty(String iSID, Collection<GroupPropertyInterface<T>> iInterfaces, Property<T> iProperty) {
        super(iSID, iInterfaces, iProperty, 0);
    }

    SourceExpr getChangedExpr(SourceExpr changedExpr, SourceExpr changedPrevExpr, SourceExpr prevExpr, SourceExpr newExpr) {
        return new CaseExpr(changedExpr.greater(prevExpr), changedExpr, new CaseExpr(changedPrevExpr.greater(changedExpr), newExpr, prevExpr));
    }
}
