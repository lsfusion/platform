package lsfusion.server.logics.navigator.controller.init;

import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.base.AppServerImage;
import lsfusion.server.logics.navigator.NavigatorElement;
import org.apache.log4j.Logger;

public class PrereadIconsNavigatorElementsTask extends GroupNavigatorElementsTask {

    @Override
    protected int getSplitCount() {
        return 1000;
    }

    @Override
    public String getCaption() {
        return "Prereading auto icons in navigator elements";
    }

    @Override
    protected void runTask(NavigatorElement element) {
        element.getImage();
    }

    @Override
    protected void runGroupTask(ImSet<NavigatorElement> objSet, Logger logger) {
        AppServerImage.prereadDefaultImages(getBL(), getDbManager(), () -> super.runGroupTask(objSet, logger));
    }
}
