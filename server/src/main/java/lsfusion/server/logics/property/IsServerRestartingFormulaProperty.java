package lsfusion.server.logics.property;

import lsfusion.server.classes.LogicalClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.i18n.LocalizedString;

public class IsServerRestartingFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public IsServerRestartingFormulaProperty() {
        super(LocalizedString.create("{logics.property.server.reboots}"), SQLSession.isServerRestartingParam, LogicalClass.instance);

        finalizeInit();
    }
}
