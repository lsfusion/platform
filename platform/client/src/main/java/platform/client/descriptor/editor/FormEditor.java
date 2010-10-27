package platform.client.descriptor.editor;

import platform.client.descriptor.FormDescriptor;
import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementTextEditor;

import javax.swing.*;

public class FormEditor extends JPanel implements NodeEditor {
    private final FormDescriptor form;

    public FormEditor(FormDescriptor form) {
        this.form = form;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(new TitledPanel("Заголовок", new IncrementTextEditor(form, "caption")));
    }


    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
