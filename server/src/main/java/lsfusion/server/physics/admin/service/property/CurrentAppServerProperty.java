package lsfusion.server.physics.admin.service.property;


import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.CurrentEnvironmentProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class CurrentAppServerProperty extends CurrentEnvironmentProperty {

    public CurrentAppServerProperty(ValueClass paramClass) {
        super(LocalizedString.create("{service.scaling.app.server.current}"), SQLSession.appServerParam, paramClass);

        finalizeInit();
    }
}
