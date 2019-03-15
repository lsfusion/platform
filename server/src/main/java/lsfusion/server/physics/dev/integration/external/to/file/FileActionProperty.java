package lsfusion.server.physics.dev.integration.external.to.file;

import lsfusion.server.language.property.LP;
import lsfusion.server.logics.action.SystemExplicitAction;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.infer.ClassType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

// тут их все наверное надо рефакторить на addJoinAProp
// остался незарефакторенный только LOAD, который должен исчезнуть
public abstract class FileActionProperty extends SystemExplicitAction {

    protected LP<?> fileProperty;
    protected FileActionProperty(LocalizedString caption, LP fileProperty) {
        super(caption, fileProperty.getInterfaceClasses(ClassType.filePolicy));

        this.fileProperty = fileProperty;
    }

    protected FileActionProperty(LocalizedString caption, ValueClass... valueClasses) {
        super(caption, valueClasses);
    }
}
