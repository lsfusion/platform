package lsfusion.server.physics.dev.property;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.logics.classes.data.LogicalClass;
import lsfusion.server.logics.property.CurrentEnvironmentProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class InTestModeProperty extends CurrentEnvironmentProperty {

    public final static InTestModeProperty instance = new InTestModeProperty();

    public InTestModeProperty() {
        super(LocalizedString.create("isInTestModeParam"), SQLSession.inTestModeParam, LogicalClass.instance);

        finalizeInit();
    }
}
