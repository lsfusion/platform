package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.data.SQLSession;

public class CurrentComputerFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentComputerFormulaProperty(String sID, ValueClass paramClass) {
        super(sID, "Тек. компьютер", SQLSession.computerParam, paramClass);
    }
}
