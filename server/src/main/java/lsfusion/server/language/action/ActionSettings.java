package lsfusion.server.language.action;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.oraction.ActionOrPropertySettings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ActionSettings extends ActionOrPropertySettings {
    public LocalizedString contextMenuCaption;
    public ScriptingLogicsModule.ActionOrPropertyUsage contextMenuMainPropertyUsage;
    public String eventActionSID;
    public Boolean before;
    public ScriptingLogicsModule.ActionOrPropertyUsage eventActionMainPropertyUsage;
    public Boolean askConfirm;
}
