package lsfusion.server.logics.action.session;

import lsfusion.server.logics.BaseLogicsModule;

public class ApplyOnlyWithoutRecalcActionProperty extends ApplyFilterProperty {

    public ApplyOnlyWithoutRecalcActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.WITHOUT_RECALC);
    }
}
