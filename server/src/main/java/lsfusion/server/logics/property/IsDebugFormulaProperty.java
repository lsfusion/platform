package lsfusion.server.logics.property;

import lsfusion.server.classes.LogicalClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.ServerResourceBundle;

public class IsDebugFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public final static IsDebugFormulaProperty instance = new IsDebugFormulaProperty();

    private IsDebugFormulaProperty() {
        super(ServerResourceBundle.getString("logics.property.current.isdebug"), SQLSession.isDebugParam, LogicalClass.instance);

        finalizeInit();
    }
}
