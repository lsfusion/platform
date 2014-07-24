package lsfusion.server.logics.property;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.ServerResourceBundle;

public class CurrentUserFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentUserFormulaProperty(ValueClass paramClass) {
        super(ServerResourceBundle.getString("logics.property.current.user"), SQLSession.userParam, paramClass.getUpSet());

        finalizeInit();
    }
}
