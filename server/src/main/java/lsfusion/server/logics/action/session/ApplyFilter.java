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

    public static ApplyFilter get(String id) {
        if(id != null) {
            switch (id) {
                case "onlyCalc": return ApplyFilter.ONLY_CALC;
                case "onlyCheck": return ApplyFilter.ONLYCHECK;
                case "onlyData": return ApplyFilter.ONLY_DATA;
                case "session" : return ApplyFilter.SESSION;
                case "withoutRecalc": return ApplyFilter.WITHOUT_RECALC;
            }
        }
        return ApplyFilter.NO;
    }

    public static boolean isCheck(ActionOrProperty property) {
        return property instanceof Action && ((Action) property).hasFlow(ChangeFlowType.CANCEL);
    }

    public boolean containsChange(ActionOrProperty property) {
        if(this == SESSION)
            return property instanceof Action && ((Action<?>) property).getSessionEnv(SystemEvent.SESSION) != null;

        // GLOBAL
        if(property.getApplyEvent() == null)
            return false;
        else
            assert property instanceof Action || ((Property<?>)property).isStored() || (property instanceof ChangedProperty && ((ChangedProperty<?>) property).isSingleApplyDroppedIsClassProp());

        // we need "action change" links for all global filters, since event actions / properties can depend on each other
        return true;
    }

    public boolean contains(ActionOrProperty property) {
        if(!containsChange(property))
            return false;

        switch (this) {
            case SESSION:
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
