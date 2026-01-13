package lsfusion.server.logics.form.struct.filter;

import lsfusion.base.identity.IDGenerator;
import lsfusion.interop.form.event.InputBindingEvent;
import lsfusion.server.logics.form.ObjectMapping;
import lsfusion.server.logics.form.interactive.design.filter.RegularFilterView;
import lsfusion.server.logics.form.struct.IdentityEntity;
import lsfusion.server.logics.form.struct.object.GroupObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class RegularFilterEntity extends IdentityEntity<RegularFilterEntity, GroupObjectEntity> {
    public transient FilterEntity filter;
    public LocalizedString name;
    public InputBindingEvent keyInputEvent;
    public boolean showKey;
    public InputBindingEvent mouseInputEvent;
    public boolean showMouse;

    public RegularFilterView view;

    @Override
    protected String getDefaultSIDPrefix() {
        return "filter";
    }

    public RegularFilterEntity(IDGenerator ID, String sID, FilterEntity filter, LocalizedString name,
                               InputBindingEvent keyInputEvent, boolean showKey,
                               InputBindingEvent mouseInputEvent, boolean showMouse) {
        super(ID, sID, null);

        this.filter = filter;
        this.name = name;
        this.keyInputEvent = keyInputEvent;
        this.showKey = showKey;
        this.mouseInputEvent = mouseInputEvent;
        this.showMouse = showMouse;
    }

    // copy-constructor
    protected RegularFilterEntity(RegularFilterEntity src, ObjectMapping mapping) {
        super(src, mapping);

        name = src.name;
        keyInputEvent = src.keyInputEvent;
        showKey = src.showKey;
        mouseInputEvent = src.mouseInputEvent;
        showMouse = src.showMouse;

        filter = mapping.get(src.filter);
        view = mapping.get(src.view);
    }

    @Override
    public RegularFilterEntity copy(ObjectMapping mapping) {
        return new RegularFilterEntity(this, mapping);
    }
}
