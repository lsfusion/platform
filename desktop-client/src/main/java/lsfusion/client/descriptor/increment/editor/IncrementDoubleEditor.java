package lsfusion.client.descriptor.increment.editor;

import lsfusion.base.context.ApplicationContextProvider;

public class IncrementDoubleEditor extends IncrementTextEditor {
    public IncrementDoubleEditor(ApplicationContextProvider object, String field) {
        super(object, field);
    }

    @Override
    protected Object parseText(String text) {
        return Double.parseDouble(text);
    }

    @Override
    protected String valueToString(Object value) {
        return String.valueOf(value);
    }
}
