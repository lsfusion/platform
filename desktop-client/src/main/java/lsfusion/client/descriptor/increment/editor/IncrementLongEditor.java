package lsfusion.client.descriptor.increment.editor;

import lsfusion.base.context.ApplicationContextProvider;

public class IncrementLongEditor extends IncrementTextEditor {
    public IncrementLongEditor(ApplicationContextProvider object, String field) {
        super(object, field);
    }

    @Override
    protected Object parseText(String text) {
        return Long.parseLong(text);
    }

    @Override
    protected String valueToString(Object value) {
        return String.valueOf(value);
    }
}
