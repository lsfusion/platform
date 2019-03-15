package lsfusion.server.logics.action.session.action;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.action.session.ApplyFilter;

public class ApplyOnlyDataActionProperty extends ApplyFilterAction {

    public ApplyOnlyDataActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.ONLY_DATA);
    }

}
