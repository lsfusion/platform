package platform.server.logics.property;

import platform.base.*;
import platform.server.classes.ValueClass;
import platform.server.data.expr.*;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.BaseMutableModifier;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;

import java.util.*;

import static platform.base.BaseUtils.crossJoin;
import static platform.base.BaseUtils.join;
import static platform.base.BaseUtils.merge;

public class SessionDataProperty extends DataProperty {

    public SessionDataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, classes, value);

        finalizeInit();
    }

    @Override
    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, boolean propClasses, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(propClasses)
            return getClassTableExpr(joinImplement);
        return CaseExpr.NULL;
    }

    public boolean isStored() {
        return false;
    }
}

