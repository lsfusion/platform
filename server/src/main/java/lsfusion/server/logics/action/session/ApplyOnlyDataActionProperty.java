package lsfusion.server.logics.action.session;

import lsfusion.server.logics.BaseLogicsModule;

public class ApplyOnlyDataActionProperty extends ApplyFilterAction {

    public ApplyOnlyDataActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.ONLY_DATA);
    }

}
