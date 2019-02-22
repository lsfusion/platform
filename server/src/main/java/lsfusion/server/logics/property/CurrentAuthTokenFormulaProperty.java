package lsfusion.server.logics.property;

import lsfusion.server.classes.StringClass;
import lsfusion.server.classes.sets.AndClassSet;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.i18n.LocalizedString;

public class CurrentAuthTokenFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentAuthTokenFormulaProperty() {
        super(LocalizedString.create("{logics.property.current.auth.token}"), SQLSession.authTokenParam, StringClass.text);
    }
}
