package platform.client.descriptor.increment.editor;

import platform.client.descriptor.increment.IncrementView;
import platform.client.descriptor.increment.IncrementDependency;
import platform.base.BaseUtils;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class IncrementTextEditor extends JTextField implements IncrementView {
    private final Object object;
    private final String field;
    public IncrementTextEditor(Object object, String field) {
        this.object = object;
        this.field = field;

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                updateField();
            }
        });

        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateField();
            }
        });

        IncrementDependency.add(object, field, this);
    }

    private void updateField() {
        BaseUtils.invokeSetter(object, field, getText().trim());
    }

    public void update(Object updateObject, String updateField) {
        setText((String) BaseUtils.invokeGetter(object, field));
    }
}
