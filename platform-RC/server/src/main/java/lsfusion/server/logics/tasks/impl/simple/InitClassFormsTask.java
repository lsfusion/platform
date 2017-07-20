package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.logics.tasks.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitClassFormsTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing class forms";
    }

    public void run(Logger logger) {
        getBL().LM.initClassForms();
    }
}
