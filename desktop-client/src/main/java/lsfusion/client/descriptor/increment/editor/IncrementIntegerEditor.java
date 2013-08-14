package lsfusion.client.descriptor.increment.editor;

import lsfusion.base.context.ApplicationContextProvider;

public class IncrementIntegerEditor extends IncrementTextEditor {
    public IncrementIntegerEditor(ApplicationContextProvider object, String field) {
        super(object, field);
    }

    @Override
    protected Object parseText(String text) {
        return Integer.parseInt(text);
    }

    @Override
    protected String valueToString(Object value) {
        return String.valueOf(value);
    }
}
