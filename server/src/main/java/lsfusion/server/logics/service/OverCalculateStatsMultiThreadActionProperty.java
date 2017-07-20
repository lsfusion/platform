package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.i18n.FormatLocalizedString;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.logics.tasks.impl.recalculate.OverCalculateStatsTask;

import static lsfusion.server.context.ThreadLocalContext.localize;

public class OverCalculateStatsMultiThreadActionProperty extends MultiThreadActionProperty {

    public OverCalculateStatsMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);
    }

    @Override
    protected GroupPropertiesSingleTask createTask() {
        return new OverCalculateStatsTask();
    }

    @Override
    protected String getCaptionError() {
        return localize("{logics.recalculation.stats.error}");
    }

    @Override
    protected MessageClientAction createMessageClientAction(GroupPropertiesSingleTask task, boolean errorOccurred) {
        return new MessageClientAction(localize(new FormatLocalizedString(errorOccurred ? "{logics.recalculation.failed}" : "{logics.recalculation.completed}",
                localize("{logics.recalculation.stats}"))) + task.getMessages(), localize("{logics.recalculation.stats}"));
    }
}