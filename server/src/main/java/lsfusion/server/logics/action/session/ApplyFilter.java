package lsfusion.server.logics.action.session;

import lsfusion.server.logics.action.Action;
import lsfusion.server.logics.action.flow.ChangeFlowType;
import lsfusion.server.logics.action.session.changed.ChangedProperty;
import lsfusion.server.logics.event.SystemEvent;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.user.ClassDataProperty;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public enum ApplyFilter {
    // LOCAL (events with sessionEnv LOCAL)
    SESSION,
    // GLOBAL (events with sessionEnv GLOBAL + materialized properties)
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
        return property instanceof Action && ((Action) property).hasFlow(ChangeFlowType.CANCEL);
    }

    public boolean contains(ActionOrProperty property) {
        if(this == SESSION)
            return property instanceof Action && ((Action<?>) property).getSessionEnv(SystemEvent.SESSION) != null;

        // GLOBAL
        if(property.getApplyEvent() == null)
            return false;
        else
            assert property instanceof Action || ((Property<?>)property).isStored() || (property instanceof ChangedProperty && ((ChangedProperty<?>) property).isSingleApplyDroppedIsClassProp());
//        if(property instanceof Property) {
//            if(!((Property<?>) property).isStored())
//                return false;
//        } else {
//            if(((Action<?>) property).getSessionEnv(SystemEvent.APPLY) == null)
//                return false;
//        }
//
        switch (this) {
            case NO:
                return true;
            case ONLY_CALC:
            case ONLY_DATA:
                return property instanceof DataProperty || property instanceof ClassDataProperty;
            case WITHOUT_RECALC:
                return property instanceof DataProperty || property instanceof ClassDataProperty || ((property instanceof Action)
                        && !((Action)property).hasResolve());
            case ONLYCHECK:
                return isCheck(property);
        }
        throw new UnsupportedOperationException();
    }
}
