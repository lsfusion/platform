package lsfusion.server.logics.property.actions;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.ApplyFilter;

public class ApplyOnlyWithoutRecalcActionProperty extends ApplyFilterProperty {

    public ApplyOnlyWithoutRecalcActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.WITHOUT_RECALC);
    }
}
