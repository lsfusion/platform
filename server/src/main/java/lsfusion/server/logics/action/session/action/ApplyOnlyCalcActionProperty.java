package lsfusion.server.logics.action.session.action;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.ApplyFilter;

public class ApplyOnlyCalcActionProperty extends ApplyFilterAction {

    public ApplyOnlyCalcActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.ONLY_CALC);
    }
}
