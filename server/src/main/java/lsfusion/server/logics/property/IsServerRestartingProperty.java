package lsfusion.server.logics.property;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class IsServerRestartingProperty extends CurrentEnvironmentProperty {

    public IsServerRestartingProperty() {
        super(LocalizedString.create("{logics.property.server.reboots}"), SQLSession.isServerRestartingParam, LogicalClass.instance);

        finalizeInit();
    }
}
