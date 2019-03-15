package lsfusion.server.logics.action.session.action;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.ApplyFilter;

public class ApplyAllActionProperty extends ApplyFilterAction {

    public ApplyAllActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.NO);
    }
}
