package platform.server.logics.properties;

import platform.interop.Compare;
import platform.server.data.query.exprs.SourceExpr;

import java.util.Collection;

public class MaxGroupProperty<T extends PropertyInterface> extends GroupProperty<T> {

    public MaxGroupProperty(String sID, String caption, Collection<GroupPropertyInterface<T>> interfaces, Property<T> property) {
        super(sID, caption, interfaces, property, 0);
    }

    SourceExpr getChangedExpr(SourceExpr changedExpr, SourceExpr changedPrevExpr, SourceExpr prevExpr, SourceExpr newExpr) {
//        return newExpr;
        return changedExpr.ifElse(changedExpr.compare(prevExpr,Compare.GREATER).or(prevExpr.getWhere().not()),
                newExpr.ifElse(changedPrevExpr.compare(prevExpr,Compare.EQUALS), prevExpr));
    }
}
