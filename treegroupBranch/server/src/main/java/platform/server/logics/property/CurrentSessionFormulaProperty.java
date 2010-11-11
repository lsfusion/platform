package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.data.SQLSession;

public class CurrentSessionFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentSessionFormulaProperty(String sID, ValueClass paramClass) {
        super(sID, "Тек. сессия", SQLSession.sessionParam, paramClass);
    }
}
