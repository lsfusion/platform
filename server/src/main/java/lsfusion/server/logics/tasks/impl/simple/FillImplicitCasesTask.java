package lsfusion.server.logics.tasks.impl.simple;

import lsfusion.server.logics.tasks.SimpleBLTask;

public class FillImplicitCasesTask extends SimpleBLTask {

    @Override
    public String getCaption() {
        return "Filling implicit cases";
    }

    @Override
    public void run() {
        getBL().fillImplicitCases();
    }
}
