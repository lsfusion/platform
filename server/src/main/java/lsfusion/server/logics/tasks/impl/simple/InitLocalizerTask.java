package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.logics.tasks.SimpleBLTask;

public class InitLocalizerTask extends SimpleBLTask {

    @Override
    public String getCaption() {
        return "Initializing localizer";
    }

    @Override
    public void run() {
        getBL().initLocalizer();
    }
}
