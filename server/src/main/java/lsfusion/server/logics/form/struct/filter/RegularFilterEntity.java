package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.event.InputEvent;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class RegularFilterEntity extends IdentityObject {
    public transient FilterEntity filter;
    public LocalizedString name;
    public InputEvent inputEvent;
    public Integer priority;
    public boolean showKey = true;

    public RegularFilterEntity() {

    }

    public RegularFilterEntity(int iID, FilterEntity ifilter, LocalizedString iname) {
        this(iID, ifilter, iname, null, null);
    }

    public RegularFilterEntity(int ID, FilterEntity filter, LocalizedString name, InputEvent inputEvent, Integer priority) {
        this.ID = ID;
        this.filter = filter;
        this.name = name;
        this.inputEvent = inputEvent;
        this.priority = priority;
    }
}
