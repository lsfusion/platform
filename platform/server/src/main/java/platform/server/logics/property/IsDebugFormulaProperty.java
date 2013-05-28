package platform.server.logics.property;

import platform.server.classes.LogicalClass;
import platform.server.data.SQLSession;
import platform.server.logics.ServerResourceBundle;

public class IsDebugFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public final static IsDebugFormulaProperty instance = new IsDebugFormulaProperty();

    private IsDebugFormulaProperty() {
        super("IsDebug", ServerResourceBundle.getString("logics.property.current.isdebug"), SQLSession.isDebugParam, LogicalClass.instance);

        finalizeInit();
    }
}
