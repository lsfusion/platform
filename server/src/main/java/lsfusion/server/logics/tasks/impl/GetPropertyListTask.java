package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.tasks.SimpleBLTask;

public class GetPropertyListTask extends SimpleBLTask {

    public String getCaption() {
        return "Building property list";
    }

    public void run() {
        getBL().getPropertyList();
    }
}
