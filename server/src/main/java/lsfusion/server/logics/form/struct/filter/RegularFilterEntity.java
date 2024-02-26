package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.MouseInputEvent;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class RegularFilterEntity extends IdentityObject {
    public transient FilterEntity filter;
    public LocalizedString name;
    public KeyInputEvent keyInputEvent;
    public Integer keyPriority;
    public boolean showKey;
    public MouseInputEvent mouseInputEvent;
    public Integer mousePriority;
    public boolean showMouse;

    public RegularFilterEntity() {

    }

    public RegularFilterEntity(int iID, FilterEntity ifilter, LocalizedString iname) {
        this(iID, ifilter, iname, null, null, false, null, null, false);
    }

    public RegularFilterEntity(int ID, FilterEntity filter, LocalizedString name,
                               KeyInputEvent keyInputEvent, Integer keyPriority, boolean showKey,
                               MouseInputEvent mouseInputEvent, Integer mousePriority, boolean showMouse) {
        this.ID = ID;
        this.filter = filter;
        this.name = name;
        this.keyInputEvent = keyInputEvent;
        this.keyPriority = keyPriority;
        this.showKey = showKey;
        this.mouseInputEvent = mouseInputEvent;
        this.mousePriority = mousePriority;
        this.showMouse = showMouse;
    }
}
