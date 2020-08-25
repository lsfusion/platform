package lsfusion.server.logics.classes.controller.init;

import lsfusion.server.logics.classes.user.CustomClass;

// this task is needed to avoid dead locks - see initDialogForm method
public class InitClassDialogFormsTask extends GroupClassesTask {

    @Override
    protected void runTask(CustomClass customClass) {
        customClass.initDialogForm(getBL().LM);
    }

    @Override
    public String getCaption() {
        return "Initializing class dialog forms";
    }
}
