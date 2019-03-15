package lsfusion.server.logics.action.session.action;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.ApplyFilter;

public class ApplyOnlyCheckActionProperty extends ApplyFilterAction {

    public ApplyOnlyCheckActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.ONLYCHECK);
    }
}
