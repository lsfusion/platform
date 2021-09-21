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
            target.setCaption((LocalizedString) caption);
        else {
            // we want to keep base name for example for form name prediction in async form opening
            if (target.caption == null) {
                // it's important for web-client to know that caption may appear as container caption is implemented as wrapper panel
                target.caption = LocalizedString.NONAME;
            }
            target.propertyCaption = (PropertyObjectEntity<?>) caption;
        }
    }

    public void setType(ContainerType type) {
        target.setType(type);
    }
    
    public void setChildrenAlignment(FlexAlignment falign) {
        target.setChildrenAlignment(falign);
    }

    //backward compatibility
    public void setColumns(int columns) {
        target.lines = columns;
    }

    public void setLines(int lines) {
        target.lines = lines;
    }

    public void setShowIf(PropertyObjectEntity<?> showIf) {
        target.setShowIf(showIf);
    }
}
