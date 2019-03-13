package lsfusion.server.logics.action.session;

import lsfusion.server.logics.BaseLogicsModule;

public class ApplyOnlyDataActionProperty extends ApplyFilterProperty {

    public ApplyOnlyDataActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.ONLY_DATA);
    }

}
