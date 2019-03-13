package lsfusion.server.logics.action.file;

import lsfusion.server.classes.ValueClass;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.language.linear.LCP;
import lsfusion.server.logics.property.ClassType;
import lsfusion.server.logics.property.actions.SystemExplicitActionProperty;

// тут их все наверное надо рефакторить на addJoinAProp
// остался незарефакторенный только LOAD, который должен исчезнуть
public abstract class FileActionProperty extends SystemExplicitActionProperty {

    protected LCP<?> fileProperty;
    protected FileActionProperty(LocalizedString caption, LCP fileProperty) {
        super(caption, fileProperty.getInterfaceClasses(ClassType.filePolicy));

        this.fileProperty = fileProperty;
    }

    protected FileActionProperty(LocalizedString caption, ValueClass... valueClasses) {
        super(caption, valueClasses);
    }
}
