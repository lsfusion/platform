package lsfusion.server.logics.service;

import lsfusion.interop.action.MessageClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.logics.ServiceLogicsModule;
import lsfusion.server.logics.i18n.FormatLocalizedString;
import lsfusion.server.logics.tasks.GroupPropertiesSingleTask;
import lsfusion.server.logics.tasks.impl.recalculate.CheckClassesTask;

import static lsfusion.server.context.ThreadLocalContext.localize;

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
        return new MessageClientAction(localize(new FormatLocalizedString(errorOccurred ? "{logics.check.failed}" : "{logics.check.completed}",
                localize("{logics.checking.data.classes}"))) + task.getMessages(), localize("{logics.checking.data.classes}"), true);
    }
}