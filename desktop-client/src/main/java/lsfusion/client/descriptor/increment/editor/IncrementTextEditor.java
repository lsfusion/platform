package lsfusion.client.descriptor.increment.editor;

import lsfusion.base.ReflectionUtils;
import lsfusion.base.context.ApplicationContextProvider;
import lsfusion.base.context.IncrementView;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.text.ParseException;

public class IncrementTextEditor extends JTextField implements IncrementView {
    private final Object object;
    private final String field;

    public IncrementTextEditor(ApplicationContextProvider object, String field) {
        this.object = object;
        this.field = field;

        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateField();
            }
        });

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                updateField();
            }
        });

        object.getContext().addDependency(object, field, this);
    }

    private void updateField() {
        try {
            ReflectionUtils.invokeSetter(object, field, parseText(getText().trim()));
        } catch (ParseException ignore) {
            update(object, field);
        } catch (NumberFormatException nfe) {
            update(object, field);
        }
    }

    public void update(Object updateObject, String updateField) {
        String newText = valueToString(ReflectionUtils.invokeGetter(object, field));
        if (!getText().equals(newText)) {
            setText(newText);
        }
    }

    protected Object parseText(String text) throws ParseException {
        return text;
    }

    protected String valueToString(Object value) {
        return (String)value;
    }
}
