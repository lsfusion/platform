package lsfusion.server.logics.property.env;

import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class IsServerRestartingFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public IsServerRestartingFormulaProperty() {
        super(LocalizedString.create("{logics.property.server.reboots}"), SQLSession.isServerRestartingParam, LogicalClass.instance);

        finalizeInit();
    }
}
