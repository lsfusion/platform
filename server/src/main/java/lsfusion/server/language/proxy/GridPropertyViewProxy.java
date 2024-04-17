package lsfusion.server.language.proxy;

import lsfusion.server.logics.form.interactive.design.ComponentView;
import lsfusion.server.logics.form.interactive.design.object.GridPropertyView;
import lsfusion.server.logics.form.struct.property.PropertyObjectEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class GridPropertyViewProxy<T extends GridPropertyView> extends ComponentViewProxy<T> {

    public GridPropertyViewProxy(T target) {
        super(target);
    }

    public void setValueClass(Object caption) {
        if(caption instanceof LocalizedString)
            target.valueClass = ((LocalizedString) caption).getSourceString();
        else
            target.propertyValueClass = (PropertyObjectEntity<?>) caption;
    }
}
