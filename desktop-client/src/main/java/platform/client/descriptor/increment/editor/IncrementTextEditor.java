package platform.client.descriptor.increment.editor;

import platform.base.BaseUtils;
import platform.base.context.*;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

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
        BaseUtils.invokeSetter(object, field, getText().trim());
    }

    public void update(Object updateObject, String updateField) {
        String newText = (String) BaseUtils.invokeGetter(object, field);
        if (!getText().equals(newText)) {
            setText(newText);
        }
    }
}
