package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.logics.tasks.SimpleBLTask;

public class WriteModulesHashTask extends SimpleBLTask {

    @Override
    public String getCaption() {
        return "Writing modules hash";
    }

    @Override
    public void run() {
        getBL().getDbManager().writeModulesHash();
    }
}
