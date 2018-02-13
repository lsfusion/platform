package lsfusion.server.logics.tasks.impl;

import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.tasks.ReflectionTask;
import org.apache.log4j.Logger;

public class FirstRecalculateStatsTask extends ReflectionTask {

    private BusinessLogics BL;

    public BusinessLogics getBL() {
        return BL;
    }

    public void setBL(BusinessLogics BL) {
        this.BL = BL;
    }

    public String getCaption() {
        return "Recalculating Stats at first start";
    }

    @Override
    public void run(Logger logger) {
        BL.firstRecalculateStats();
    }
}