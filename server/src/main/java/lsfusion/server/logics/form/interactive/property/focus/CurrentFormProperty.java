package lsfusion.server.logics.form.interactive.property.focus;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.CurrentEnvironmentProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class CurrentFormProperty extends CurrentEnvironmentProperty {

    public CurrentFormProperty(ValueClass paramClass) {
        super(LocalizedString.create("{logics.property.current.form}"), SQLSession.formParam, paramClass);

        finalizeInit();
    }
}