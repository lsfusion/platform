package lsfusion.server.logics.action.session.action;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.ApplyFilter;

public class ApplyAllAction extends ApplyFilterAction {

    public ApplyAllAction(BaseLogicsModule lm) {
        super(lm, ApplyFilter.NO);
    }
}
