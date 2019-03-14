package lsfusion.server.logics.init;

import org.apache.log4j.Logger;

public class CheckDuplicateSystemElementsTask extends SimpleBLTask {
    @Override
    public String getCaption() {
        return "Checking for duplicate elements";
    }

    @Override
    public void run(Logger logger) {
        getBL().checkForDuplicateElements();
    }
}
