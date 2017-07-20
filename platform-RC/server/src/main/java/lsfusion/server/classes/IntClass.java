package lsfusion.server.classes;

import lsfusion.server.logics.i18n.LocalizedString;

public abstract class IntClass<T extends Number> extends IntegralClass<T> {

    public IntClass(LocalizedString caption) {
        super(caption);
    }
}
