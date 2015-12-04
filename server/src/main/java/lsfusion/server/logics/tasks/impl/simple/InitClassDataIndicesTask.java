package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.logics.tasks.SimpleBLTask;

public class InitClassDataIndicesTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing class data indexes";
    }

    public void run() {
        getBL().initClassDataIndices();
    }
}
