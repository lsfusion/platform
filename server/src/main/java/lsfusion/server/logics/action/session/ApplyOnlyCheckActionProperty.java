package lsfusion.server.logics.action.session;

import lsfusion.server.logics.BaseLogicsModule;

public class ApplyOnlyCheckActionProperty extends ApplyFilterProperty {

    public ApplyOnlyCheckActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.ONLYCHECK);
    }
}
