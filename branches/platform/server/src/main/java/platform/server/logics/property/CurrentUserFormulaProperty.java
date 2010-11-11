package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.data.SQLSession;

public class CurrentUserFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentUserFormulaProperty(String sID, ValueClass paramClass) {
        super(sID, "Тек. польз.", SQLSession.userParam, paramClass);
    }
}
