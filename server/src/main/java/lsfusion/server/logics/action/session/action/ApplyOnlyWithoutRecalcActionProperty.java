package lsfusion.server.logics.action.session.action;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.ApplyFilter;

public class ApplyOnlyWithoutRecalcActionProperty extends ApplyFilterAction {

    public ApplyOnlyWithoutRecalcActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.WITHOUT_RECALC);
    }
}
