package platform.client.descriptor.increment.editor;

import platform.client.descriptor.increment.IncrementView;
import platform.client.descriptor.increment.IncrementDependency;
import platform.base.BaseUtils;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class IncrementTextEditor extends JTextField implements IncrementView, ActionListener {

    public void actionPerformed(ActionEvent e) {
        BaseUtils.invokeSetter(object, field, getText().trim());
    }

    private final Object object;
    private final String field;
    public IncrementTextEditor(Object object, String field) {
        this.addActionListener(this);

        this.object = object;
        this.field = field;

        IncrementDependency.add(object, field, this);
    }

    public void update(Object updateObject, String updateField) {
        setText((String) BaseUtils.invokeGetter(object, field));
    }
}
