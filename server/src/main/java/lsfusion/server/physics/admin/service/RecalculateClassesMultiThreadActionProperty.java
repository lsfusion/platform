package lsfusion.server.physics.admin.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.init.GroupPropertiesSingleTask;
import lsfusion.server.physics.admin.service.task.RecalculateClassesTask;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class RecalculateClassesMultiThreadActionProperty extends MultiThreadActionProperty {

    public RecalculateClassesMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);
    }

    @Override
    protected GroupPropertiesSingleTask createTask() {
        return new RecalculateClassesTask();
    }

    @Override
    protected String getCaptionError() {
        return localize("{logics.recalculation.classes.error}");
    }

    @Override
    protected MessageClientAction createMessageClientAction(GroupPropertiesSingleTask task, boolean errorOccurred) {
        return new MessageClientAction(localize(LocalizedString.createFormatted(errorOccurred ? "{logics.recalculation.failed}" : "{logics.recalculation.completed}",
                localize("{logics.recalculating.data.classes}"))) + task.getMessages(), localize("{logics.recalculating.data.classes}"));
    }
}