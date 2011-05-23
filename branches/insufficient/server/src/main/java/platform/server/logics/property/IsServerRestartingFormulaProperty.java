package platform.server.logics.property;

import platform.server.classes.LogicalClass;
import platform.server.data.SQLSession;

public class IsServerRestartingFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public IsServerRestartingFormulaProperty(String sID) {
        super(sID, "Сервер перезагружается", SQLSession.isServerRestartingParam, LogicalClass.instance);
    }
}
