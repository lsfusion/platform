package lsfusion.server.physics.admin.service.action;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.init.GroupPropertiesSingleTask;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.admin.service.task.CheckClassesTask;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class CheckClassesMultiThreadActionProperty extends MultiThreadActionProperty {

    public CheckClassesMultiThreadActionProperty(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);
    }

    @Override
    protected GroupPropertiesSingleTask createTask() {
        return new CheckClassesTask();
    }

    @Override
    protected String getCaptionError() {
        return localize("{logics.check.classes.error}");
    }

    @Override
    protected MessageClientAction createMessageClientAction(GroupPropertiesSingleTask task, boolean errorOccurred) {
        return new MessageClientAction(localize(LocalizedString.createFormatted(errorOccurred ? "{logics.check.failed}" : "{logics.check.completed}",
                localize("{logics.checking.data.classes}"))) + task.getMessages(), localize("{logics.checking.data.classes}"), true);
    }
}