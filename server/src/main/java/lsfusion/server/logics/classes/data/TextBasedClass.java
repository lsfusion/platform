package lsfusion.server.logics.classes.data;

import lsfusion.server.data.type.Type;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class TextBasedClass<T> extends DataClass<T> {

    public TextBasedClass(LocalizedString caption) {
        super(caption);
    }

    // all children that has renderers not inherited from SimpleTextBasedCellRenderer should return false
    // otherwise CellRendererer.createCellRenderer !isTagInput assertion will be broken
    @Override
    public boolean useInputTag(boolean isPanel, boolean useBootstrap, Type changeType) {
        if(!isPanel)
            return false;

        return Settings.get().getUseInputTagForTextBasedInPanel() <= (useBootstrap ? 1 : 0) && changeType != null && getCompatible(changeType) != null;
    }
}
