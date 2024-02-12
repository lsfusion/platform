package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import javax.swing.*;

public class RegularFilterEntity extends IdentityObject {
    public transient FilterEntity filter;
    public LocalizedString name;
    public KeyInputEvent keyEvent;
    public Integer priority;
    public boolean showKey = true;

    public RegularFilterEntity() {

    }

    public RegularFilterEntity(int iID, FilterEntity ifilter, LocalizedString iname) {
        this(iID, ifilter, iname, null, null);
    }

    public RegularFilterEntity(int ID, FilterEntity filter, LocalizedString name, KeyInputEvent keyEvent, Integer priority) {
        this.ID = ID;
        this.filter = filter;
        this.name = name;
        this.keyEvent = keyEvent;
        this.priority = priority;
    }
}
