package lsfusion.server.logics.form.controller.init;

import lsfusion.server.logics.form.struct.FormEntity;

public class FinalizeFormsTask extends GroupFormsTask {

    protected void runTask(FormEntity form) {
        form.finalizeAroundInit();
    }

    public String getCaption() {
        return "Finalizing forms";
    }
}
