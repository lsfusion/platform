package lsfusion.server.logics.property.controller.init;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.oraction.ActionOrProperty;

public class CheckExplicitClassesTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Checking explicit classes";
    }

    protected void runTask(ActionOrProperty property) {
        property.checkExplicitInterfaces();
    }
}
