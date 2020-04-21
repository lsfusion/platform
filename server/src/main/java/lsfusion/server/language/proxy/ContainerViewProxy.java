package lsfusion.server.language.proxy;

import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.form.design.Alignment;
import lsfusion.interop.form.design.ContainerType;
import lsfusion.server.logics.form.interactive.design.ContainerView;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ContainerViewProxy extends ComponentViewProxy<ContainerView> {

    public ContainerViewProxy(ContainerView target) {
        super(target);
    }
    
    public void setCaption(Object caption) {
        if(caption instanceof LocalizedString)
            target.caption = (LocalizedString) caption;
        else {
            target.caption = LocalizedString.NONAME;
            target.propertyCaption = (PropertyObjectEntity<?>) caption;
        }
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

    public void setShowIf(PropertyObjectEntity<?> showIf) {
        target.setShowIf(showIf);
    }
}
