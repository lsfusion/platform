package lsfusion.client.descriptor.editor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.FormDescriptor;
import lsfusion.client.descriptor.ObjectDescriptor;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.editor.base.TitledPanel;
import lsfusion.client.descriptor.increment.editor.IncrementCheckBox;
import lsfusion.client.descriptor.increment.editor.IncrementMultipleListEditor;
import lsfusion.client.descriptor.increment.editor.IncrementMultipleListSelectionModel;
import lsfusion.client.descriptor.increment.editor.IncrementTextEditor;
import lsfusion.interop.FormEventType;

import javax.swing.*;
import java.awt.*;
import java.util.*;

public class ObjectEditor extends JPanel implements NodeEditor {

    private final ObjectDescriptor object;

    public ObjectEditor(ObjectDescriptor object, FormDescriptor form) {
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.object = object;

        add(new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.title"), new IncrementTextEditor(object, "caption")));
        add(Box.createRigidArea(new Dimension(5, 5)));

        add(new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.add.new.object.on.event"), new IncrementMultipleListEditor(
                new IncrementMultipleListSelectionModel(object, "addOnEvent") {
                    public java.util.List<?> getList() {
                        return Arrays.asList(FormEventType.values());
                    }
                })));
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(new TitledPanel(null, new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.object.editor.show.class.tree"), object.client.classChooser, "visible")));
        add(Box.createRigidArea(new Dimension(5, 5)));

        add(new TitledPanel(ClientResourceBundle.getString("descriptor.editor.object.editor.class"), new ValueClassEditor(object, "baseClass", form)));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        if (object.getBaseClass() == null) {
            JOptionPane.showMessageDialog(this, ClientResourceBundle.getString("descriptor.editor.object.editor.choose.object.class"), ClientResourceBundle.getString("descriptor.editor.object.editor.error"), JOptionPane.ERROR_MESSAGE);
            return false;
        }

        return true;
    }
}
