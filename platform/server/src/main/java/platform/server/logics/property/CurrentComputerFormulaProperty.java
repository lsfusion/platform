package platform.server.logics.property;

import platform.server.classes.ValueClass;
import platform.server.data.SQLSession;
import platform.server.logics.ServerResourceBundle;

public class CurrentComputerFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentComputerFormulaProperty(String sID, ValueClass paramClass) {
        super(sID, ServerResourceBundle.getString("logics.property.current.computer"), SQLSession.computerParam, paramClass.getUpSet());

        finalizeInit();
    }
}
