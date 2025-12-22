package lsfusion.server.language.action;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.oraction.ActionOrPropertySettings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ActionSettings extends ActionOrPropertySettings {
    public ImSet<EditEvent> actionEditEvents = SetFact.EMPTY();
    public Boolean askConfirm;

    public void addActionEditEvent(ScriptingLogicsModule.ActionOrPropertyUsage action, String actionType, Boolean before, LocalizedString contextMenuCaption, String keyPress) {
        actionEditEvents = actionEditEvents.merge(new EditEvent(action, actionType, before, contextMenuCaption, keyPress));
    }
}
