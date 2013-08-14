package lsfusion.server.logics.property.actions;

import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;
import lsfusion.server.session.ApplyFilter;

public class ApplyAllActionProperty extends ApplyFilterProperty {

    public ApplyAllActionProperty(BaseLogicsModule lm) {
        super(lm, ApplyFilter.NO);
    }
}
