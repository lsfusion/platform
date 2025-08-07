package lsfusion.server.language.action;

import lsfusion.server.language.ScriptingLogicsModule;
import lsfusion.server.language.property.oraction.ActionOrPropertySettings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ActionSettings extends ActionOrPropertySettings {
    public LocalizedString contextMenuForCaption;
    public ScriptingLogicsModule.ActionOrPropertyUsage contextMenuForMainPropertyUsage;
    public String eventActionSID;
    public Boolean eventActionBefore;
    public ScriptingLogicsModule.ActionOrPropertyUsage eventActionMainPropertyUsage;
    public Boolean askConfirm;
}
