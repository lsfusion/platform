package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.logics.tasks.SimpleBLTask;

public class InitClassFormsTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing class forms";
    }

    public void run() {
        getBL().LM.initClassForms();
    }
}
