package lsfusion.server.physics.dev.property;

import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.logics.classes.data.TextClass;
import lsfusion.server.logics.property.CurrentEnvironmentProperty;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class ProjectLSFDirProperty extends CurrentEnvironmentProperty {
    public final static ProjectLSFDirProperty instance = new ProjectLSFDirProperty();

    public ProjectLSFDirProperty() {
        super(LocalizedString.create("projectLSFDirParam"), SQLSession.projectLSFDirParam, TextClass.instance);

        finalizeInit();
    }
}
