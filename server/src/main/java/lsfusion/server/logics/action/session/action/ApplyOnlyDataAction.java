package lsfusion.server.logics.action.session.action;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.ApplyFilter;

public class ApplyOnlyDataAction extends ApplyFilterAction {

    public ApplyOnlyDataAction(BaseLogicsModule lm) {
        super(lm, ApplyFilter.ONLY_DATA);
    }

}
