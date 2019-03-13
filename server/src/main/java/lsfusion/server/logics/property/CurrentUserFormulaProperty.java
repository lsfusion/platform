package lsfusion.server.logics.property;

import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.data.SQLSession;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class CurrentUserFormulaProperty extends CurrentEnvironmentFormulaProperty {

    public CurrentUserFormulaProperty(ValueClass paramClass) {
        super(LocalizedString.create("{logics.property.current.user}"), SQLSession.userParam, paramClass.getUpSet());

        finalizeInit();
    }
}
