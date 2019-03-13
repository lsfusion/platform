package lsfusion.server.logics.action.session;

import lsfusion.server.logics.BaseLogicsModule;

public class ApplyOnlyCalcActionProperty extends ApplyFilterProperty {

    public ApplyOnlyCalcActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.ONLY_CALC);
    }
}
