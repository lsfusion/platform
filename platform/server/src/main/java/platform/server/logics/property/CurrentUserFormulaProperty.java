package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.data.SQLSession;
import platform.server.logics.ServerResourceBundle;

public class CurrentUserFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentUserFormulaProperty(String sID, ValueClass paramClass) {
        super(sID, ServerResourceBundle.getString("logics.property.current.user"), SQLSession.userParam, paramClass.getUpSet());

        finalizeInit();
    }
}
