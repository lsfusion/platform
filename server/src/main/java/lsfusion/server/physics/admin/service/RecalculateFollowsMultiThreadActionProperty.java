package lsfusion.server.physics.admin.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.property.init.GroupPropertiesSingleTask;
import lsfusion.server.physics.admin.service.task.RecalculateFollowsTask;

import static lsfusion.server.base.context.ThreadLocalContext.localize;

public class RecalculateFollowsMultiThreadActionProperty extends MultiThreadActionProperty {

    public RecalculateFollowsMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);
    }

    @Override
    protected GroupPropertiesSingleTask createTask() {
        return new RecalculateFollowsTask();
    }

    @Override
    protected String getCaptionError() {
        return localize("{logics.recalculation.follows.error}");
    }

    @Override
    protected MessageClientAction createMessageClientAction(GroupPropertiesSingleTask task, boolean errorOccurred) {
        return new MessageClientAction(localize(LocalizedString.createFormatted(errorOccurred ? "{logics.recalculation.failed}" : "{logics.recalculation.completed}",
                localize("{logics.recalculation.follows}"))) + task.getMessages(), localize("{logics.recalculation.follows}"));
    }
}