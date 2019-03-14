package lsfusion.server.logics.action.session;

import lsfusion.server.logics.action.ActionProperty;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.property.classes.ClassDataProperty;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public enum ApplyFilter {
    WITHOUT_RECALC, ONLYCHECK, NO, ONLY_CALC, ONLY_DATA;

/*    public String getSID() {
        switch (this) {
            case WITHOUT_RECALC:
                return "enableOnlyWithoutRecalc";
            case ONLYCHECK:
                return "enableOnlyCheck";
            case NO:
                return "enableAll";
        }
        throw new UnsupportedOperationException();
    }*/

    public static boolean isCheck(ActionOrProperty property) {
        return property instanceof ActionProperty && ((ActionProperty) property).hasFlow(ChangeFlowType.CANCEL);
    }

    public boolean contains(ActionOrProperty property) {
        switch (this) {
            case NO:
                return true;
            case ONLY_CALC:
            case ONLY_DATA:
                return property instanceof DataProperty || property instanceof ClassDataProperty;
            case WITHOUT_RECALC:
                return property instanceof DataProperty || property instanceof ClassDataProperty || ((property instanceof ActionProperty)
                        && !((ActionProperty)property).hasResolve());
            case ONLYCHECK:
                return isCheck(property);
        }
        throw new UnsupportedOperationException();
    }
}
