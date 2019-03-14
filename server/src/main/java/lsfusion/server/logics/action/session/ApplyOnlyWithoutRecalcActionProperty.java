package lsfusion.server.logics.action.session;

import lsfusion.server.logics.BaseLogicsModule;

public class ApplyOnlyWithoutRecalcActionProperty extends ApplyFilterAction {

    public ApplyOnlyWithoutRecalcActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.WITHOUT_RECALC);
    }
}
