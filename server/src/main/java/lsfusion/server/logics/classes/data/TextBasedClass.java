package lsfusion.server.logics.classes.data;

import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class TextBasedClass<T> extends DataClass<T> {

    public TextBasedClass(LocalizedString caption) {
        super(caption);
    }

    @Override
    public boolean useInputTag() {
        return Settings.get().isUseInputTagForTextBased();
    }
}
