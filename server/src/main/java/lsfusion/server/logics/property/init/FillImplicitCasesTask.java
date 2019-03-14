package lsfusion.server.logics.property.init;

import lsfusion.server.logics.init.SimpleBLTask;
import org.apache.log4j.Logger;

public class FillImplicitCasesTask extends SimpleBLTask {

    @Override
    public String getCaption() {
        return "Filling implicit cases";
    }

    @Override
    public void run(Logger logger) {
        getBL().fillImplicitCases();
    }
}
