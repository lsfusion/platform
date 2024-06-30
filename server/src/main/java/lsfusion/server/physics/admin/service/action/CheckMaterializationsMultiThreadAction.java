package lsfusion.server.physics.admin.service.action;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.controller.init.GroupPropertiesSingleTask;
import lsfusion.server.physics.admin.service.ServiceLogicsModule;
import lsfusion.server.physics.admin.service.task.CheckMaterializationsTask;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import static lsfusion.server.base.controller.thread.ThreadLocalContext.localize;

public class CheckMaterializationsMultiThreadAction extends MultiThreadAction {

    public CheckMaterializationsMultiThreadAction(ServiceLogicsModule LM, ValueClass... classes) {
        super(LM,classes);
    }

    @Override
    protected GroupPropertiesSingleTask createTask() {
        return new CheckMaterializationsTask();
    }

    @Override
    protected String getCaptionError() {
        return localize("{logics.checking.materializations.error}");
    }

    @Override
    protected Messages getMessages(GroupPropertiesSingleTask task, boolean errorOccurred) {
        return new Messages(localize(LocalizedString.createFormatted(errorOccurred ? "{logics.check.failed}" : "{logics.check.completed}",
                localize("{logics.checking.materializations}"))) + task.getMessages(), localize("{logics.checking.materializations}"), true);
    }
}