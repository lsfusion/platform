package lsfusion.server.logics.tasks.impl;

import lsfusion.server.Settings;
import lsfusion.server.SystemProperties;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.tasks.GroupPropertiesTask;

import static lsfusion.base.BaseUtils.serviceLogger;
import static lsfusion.base.BaseUtils.systemLogger;

public class PrereadPropertyCachesTask extends GroupPropertiesTask {

    public String getCaption() {
        return "Prereading caches for properties";
    }

    @Override
    public boolean isEndLoggable() {
        return true;
    }

    public String getEndCaption() {
        return "Prereading caches for properties ended";
    }

    @Override
    protected boolean prerun() {
        if (SystemProperties.isDebug) {
            return false;
        }
        return true;
    }

    protected void runTask(Property property) {

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
