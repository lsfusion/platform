package lsfusion.server.logics.action.session.action;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.ApplyFilter;

public class ApplyOnlyCheckAction extends ApplyFilterAction {

    public ApplyOnlyCheckAction(BaseLogicsModule lm) {
        super(lm, ApplyFilter.ONLYCHECK);
    }
}
