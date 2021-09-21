package lsfusion.server.physics.admin.authentication.property;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.CurrentEnvironmentProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class CurrentComputerProperty extends CurrentEnvironmentProperty {

    public CurrentComputerProperty(ValueClass paramClass) {
        super(LocalizedString.create("{logics.property.current.computer}"), SQLSession.computerParam, paramClass);

        finalizeInit();
    }
}
