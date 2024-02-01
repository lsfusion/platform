package lsfusion.server.logics.property.controller.init;

import lsfusion.server.logics.property.oraction.ActionOrProperty;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.SystemProperties;

import static lsfusion.base.BaseUtils.systemLogger;

public class PrereadPropertyCachesTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Prereading caches for properties";
    }

    @Override
    protected boolean ignoreTaskException() {
        return true;
    }

    @Override
    public boolean isEndLoggable() {
        return true;
    }

    @Override
    protected boolean prerun() {
        if (SystemProperties.lightStart || Settings.get().isDisablePrereadCaches()) {
            return false;
        }
        return true;
    }

    protected void runTask(ActionOrProperty property) {

        final long maxPrereadCachesTime = Settings.get().getMaxPrereadCachesTime();
        long start = System.currentTimeMillis();

        property.prereadCaches();

        long time = System.currentTimeMillis() - start;
        if (time > maxPrereadCachesTime) {
            systemLogger.info(String.format("Preread Caches: %sms, %s", time, property.toString()));
        }
    }

    @Override
    protected int getSplitCount() {
        return 1000000;
    }
}
