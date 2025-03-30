package lsfusion.server.logics.classes.data;

import lsfusion.server.physics.dev.i18n.LocalizedString;

public abstract class FormatClass<T> extends TextBasedClass<T> {

    public FormatClass(LocalizedString caption) {
        super(caption);
    }

    @Override
    public T parseUI(String value, String pattern) throws ParseException {
        // should use pattern
        throw new UnsupportedOperationException();
    }

    @Override
    public String formatUI(T object, String pattern) {
        // should use pattern
        throw new UnsupportedOperationException();
    }
}
