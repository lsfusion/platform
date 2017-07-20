package lsfusion.server.logics.tasks;

import lsfusion.server.logics.BusinessLogics;

public abstract class BLTask extends PublicTask {

    private BusinessLogics BL;

    public BusinessLogics getBL() {
        return BL;
    }

    public void setBL(BusinessLogics BL) {
        this.BL = BL;
    }
}
