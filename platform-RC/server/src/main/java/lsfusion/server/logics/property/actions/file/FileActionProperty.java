package lsfusion.server.logics.property.actions.file;

import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.linear.LCP;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;

// тут их все наверное надо рефакторить на addJoinAProp
public abstract class FileActionProperty extends SystemExplicitActionProperty {

    protected LCP<?> fileProperty;
    protected FileActionProperty(LocalizedString caption, LCP fileProperty) {
        super(caption, fileProperty.getInterfaceClasses(ClassType.filePolicy));
        
        this.fileProperty = fileProperty;
    }
}
