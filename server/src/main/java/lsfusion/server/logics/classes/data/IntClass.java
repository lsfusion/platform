package lsfusion.server.logics.classes.data;

import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class IntClass<T extends Number> extends IntegralClass<T> {

    public IntClass(LocalizedString caption) {
        super(caption);
    }
}
