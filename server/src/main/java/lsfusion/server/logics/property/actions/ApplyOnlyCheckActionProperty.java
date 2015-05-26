package lsfusion.server.logics.property.actions;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.session.ApplyFilter;

public class ApplyOnlyCheckActionProperty extends ApplyFilterProperty {

    public ApplyOnlyCheckActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.ONLYCHECK);
    }
}
