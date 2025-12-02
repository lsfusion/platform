package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.identity.IdentityObject;
import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.server.logics.BaseLogicsModule;
import lsfusion.server.logics.form.ObjectMapping;
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

    // copy-constructor
    public RegularFilterEntity(RegularFilterEntity src) {
        super(src);
        this.ID = BaseLogicsModule.generateStaticNewID();
        this.name = src.name;
        this.keyInputEvent = src.keyInputEvent;
        this.showKey = src.showKey;
        this.mouseInputEvent = src.mouseInputEvent;
        this.showMouse = src.showMouse;
    }

    public void copy(RegularFilterEntity src, ObjectMapping mapping) {
        this.filter = mapping.get(src.filter);
    }
}
