package lsfusion.server.logics.property.actions;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.session.ApplyFilter;

public class ApplyOnlyDataActionProperty extends ApplyFilterProperty {

    public ApplyOnlyDataActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.ONLY_DATA);
    }

}
