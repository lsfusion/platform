package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.i18n.FormatLocalizedString;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.logics.tasks.impl.recalculate.RecalculateStatsTask;

import static lsfusion.server.context.ThreadLocalContext.localize;

public class RecalculateStatsMultiThreadActionProperty extends MultiThreadActionProperty {

    public RecalculateStatsMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);
    }

    @Override
    protected GroupPropertiesSingleTask createTask() {
        return new RecalculateStatsTask();
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