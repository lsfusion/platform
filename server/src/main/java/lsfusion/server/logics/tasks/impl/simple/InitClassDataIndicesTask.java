package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.logics.tasks.SimpleBLTask;
import org.apache.log4j.Logger;

public class InitClassDataIndicesTask extends SimpleBLTask {

    public String getCaption() {
        return "Initializing class data indexes";
    }

    public void run(Logger logger) {
        getBL().initClassDataIndices();
    }
}
