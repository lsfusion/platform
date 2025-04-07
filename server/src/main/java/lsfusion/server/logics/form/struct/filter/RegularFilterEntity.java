package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class RegularFilterEntity extends IdentityObject {
    public transient FilterEntity filter;
    public LocalizedString name;
    public InputBindingEvent keyInputEvent;
    public boolean showKey;
    public InputBindingEvent mouseInputEvent;
    public boolean showMouse;

    public RegularFilterEntity() {

    }

    public RegularFilterEntity(int iID, FilterEntity ifilter, LocalizedString iname) {
        this(iID, ifilter, iname, null, false, null, false);
    }

    public RegularFilterEntity(int ID, FilterEntity filter, LocalizedString name,
                               InputBindingEvent keyInputEvent, boolean showKey,
                               InputBindingEvent mouseInputEvent, boolean showMouse) {
        this.ID = ID;
        this.filter = filter;
        this.name = name;
        this.keyInputEvent = keyInputEvent;
        this.showKey = showKey;
        this.mouseInputEvent = mouseInputEvent;
        this.showMouse = showMouse;
    }
}
