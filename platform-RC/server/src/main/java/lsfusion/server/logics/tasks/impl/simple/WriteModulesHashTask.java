package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.logics.tasks.SimpleBLTask;
import org.apache.log4j.Logger;

public class WriteModulesHashTask extends SimpleBLTask {

    @Override
    public String getCaption() {
        return "Writing modules hash";
    }

    @Override
    public void run(Logger logger) {
        getBL().getDbManager().writeModulesHash();
    }
}
