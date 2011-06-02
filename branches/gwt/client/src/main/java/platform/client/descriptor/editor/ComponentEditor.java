package platform.client.descriptor.editor;

import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.NorthBoxPanel;
import platform.client.descriptor.increment.editor.IncrementCheckBox;
import platform.client.logics.ClientComponent;

import javax.swing.*;
import java.awt.*;

public class ComponentEditor extends JTabbedPane implements NodeEditor {

    protected JPanel defaultComponentEditor;
    protected ComponentConstraintsEditor constraintsEditor;
    protected ComponentDesignEditor designEditor;

    public ComponentEditor(final ClientComponent component) {

        defaultComponentEditor = new JPanel();
        defaultComponentEditor.setLayout(new FlowLayout(FlowLayout.LEFT));
        defaultComponentEditor.add(new IncrementCheckBox("Компонент по умолчанию", component, "defaultComponent"));
        constraintsEditor = new ComponentConstraintsEditor(component.constraints);
        designEditor = new ComponentDesignEditor("Дизайн компонента", component.design);

        addTab("Отображение", new NorthBoxPanel(defaultComponentEditor, designEditor));
        addTab("Расположение", new NorthBoxPanel(constraintsEditor));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
