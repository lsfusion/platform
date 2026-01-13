package lsfusion.server.language.form.object;

import lsfusion.server.logics.form.struct.action.ActionObjectEntity;
import lsfusion.server.logics.form.struct.object.ObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ScriptingObject {
    public LocalizedString caption;
    public ActionObjectEntity event;
    public String integrationSID;

    public ScriptingObject(LocalizedString caption, ActionObjectEntity event, String integrationSID) {
        this.caption = caption;
        this.event = event;
        this.integrationSID = integrationSID;
    }
}
