package lsfusion.server.logics.action.session.action;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.ApplyFilter;

public class ApplyOnlyCalcAction extends ApplyFilterAction {

    public ApplyOnlyCalcAction(BaseLogicsModule lm) {
        super(lm, ApplyFilter.ONLY_CALC);
    }
}
