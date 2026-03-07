package lsfusion.server.language.form.object;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.language.property.LP;
import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ScriptingObject {
    public LocalizedString caption;
    public ActionObjectEntity event;
    public String integrationSID;
    public LP parentProperty;
    public ImOrderSet<String> parentMapping;

    public ScriptingObject() {
    }
}
