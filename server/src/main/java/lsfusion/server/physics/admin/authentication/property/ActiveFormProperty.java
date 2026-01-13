package lsfusion.server.physics.admin.authentication.property;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.property.CurrentEnvironmentProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ActiveFormProperty extends CurrentEnvironmentProperty {

    public ActiveFormProperty() {
        super(LocalizedString.create("{logics.property.active.form}"), SQLSession.activeFormParam, StringClass.text);

        finalizeInit();
    }
}
