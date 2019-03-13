package lsfusion.server.logics.property.actions;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.ApplyFilter;

public class ApplyOnlyCalcActionProperty extends ApplyFilterProperty {

    public ApplyOnlyCalcActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.ONLY_CALC);
    }
}
