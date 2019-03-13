package lsfusion.server.logics.property;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class CurrentConnectionFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentConnectionFormulaProperty(ValueClass paramClass) {
        super(LocalizedString.create("{logics.property.current.connection}"), SQLSession.connectionParam, paramClass.getUpSet());

        finalizeInit();
    }
}