package lsfusion.server.physics.dev;

import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.property.CurrentEnvironmentProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class IsDevProperty extends CurrentEnvironmentProperty {

    public final static IsDevProperty instance = new IsDevProperty();

    private IsDevProperty() {
        super(LocalizedString.create("{logics.property.current.isdebug}"), SQLSession.isDevParam, LogicalClass.instance);

        finalizeInit();
    }
}
