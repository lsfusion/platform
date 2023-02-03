package lsfusion.server.physics.dev.property;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.property.CurrentEnvironmentProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class IsLightStartProperty extends CurrentEnvironmentProperty {

    public final static IsLightStartProperty instance = new IsLightStartProperty();

    private IsLightStartProperty() {
        super(LocalizedString.create("{logics.property.current.islightstart}"), SQLSession.isLightStartParam, LogicalClass.instance);

        finalizeInit();
    }
}