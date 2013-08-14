package lsfusion.client.descriptor.editor;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.descriptor.editor.base.NodeEditor;
import lsfusion.client.descriptor.editor.base.NorthBoxPanel;
import lsfusion.client.descriptor.increment.editor.IncrementCheckBox;
import lsfusion.client.logics.ClientComponent;

import javax.swing.*;
import java.awt.*;

public class ComponentEditor extends JTabbedPane implements NodeEditor {

    protected JPanel defaultComponentEditor;
    protected ComponentConstraintsEditor constraintsEditor;
    protected SizesEditor sizesEditor;
    protected ComponentDesignEditor designEditor;

    public ComponentEditor(final ClientComponent component) {

        defaultComponentEditor = new JPanel();
        defaultComponentEditor.setLayout(new FlowLayout(FlowLayout.LEFT));
        defaultComponentEditor.add(new IncrementCheckBox(ClientResourceBundle.getString("descriptor.editor.display.default.component"), component, "defaultComponent"));
        sizesEditor = new SizesEditor(component);
        constraintsEditor = new ComponentConstraintsEditor(component);
        designEditor = new ComponentDesignEditor(ClientResourceBundle.getString("descriptor.editor.component.design"), component.design);

        addTab(ClientResourceBundle.getString("descriptor.editor.display"), new NorthBoxPanel(defaultComponentEditor, sizesEditor, designEditor));
        addTab(ClientResourceBundle.getString("descriptor.editor.arrangement"), new NorthBoxPanel(constraintsEditor));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
