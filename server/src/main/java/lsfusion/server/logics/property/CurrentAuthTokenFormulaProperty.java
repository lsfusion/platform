package lsfusion.server.logics.property;

import lsfusion.server.classes.StringClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class CurrentAuthTokenFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentAuthTokenFormulaProperty() {
        super(LocalizedString.create("{logics.property.current.auth.token}"), SQLSession.authTokenParam, StringClass.text);
    }
}
