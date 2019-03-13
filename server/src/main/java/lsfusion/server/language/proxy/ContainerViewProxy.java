package lsfusion.server.language.proxy;

import lsfusion.interop.form.layout.Alignment;
import lsfusion.interop.form.layout.ContainerType;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.server.logics.form.struct.property.CalcPropertyObjectEntity;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ContainerViewProxy extends ComponentViewProxy<ContainerView> {

    public ContainerViewProxy(ContainerView target) {
        super(target);
    }
    
    public void setCaption(LocalizedString caption) {
        target.caption = caption;
    }

    public void setDescription(LocalizedString description) {
        target.description = description;
    }

    public void setType(ContainerType type) {
        target.setType(type);
    }
    
    public void setChildrenAlignment(FlexAlignment falign) {
        Alignment align;
        switch (falign) {
            case START: align = Alignment.START; break;
            case CENTER: align = Alignment.CENTER; break;
            case END: align = Alignment.END; break;
            default:
                throw new IllegalStateException("Children alignment should be either of START, CENTER, END");
        }
        target.setChildrenAlignment(align);
    }

    public void setColumns(int columns) {
        target.columns = columns;
    }

    public void setShowIf(CalcPropertyObjectEntity<?> showIf) {
        target.setShowIf(showIf);
    }
}
