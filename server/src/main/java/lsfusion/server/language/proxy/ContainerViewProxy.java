package lsfusion.server.language.proxy;

import lsfusion.interop.base.view.FlexAlignment;
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
        target.setChildrenAlignment(falign);
    }

    public void setColumns(int columns) {
        target.columns = columns;
    }

    public void setShowIf(PropertyObjectEntity<?> showIf) {
        target.setShowIf(showIf);
    }
}
