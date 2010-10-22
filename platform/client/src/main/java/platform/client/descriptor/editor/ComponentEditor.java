package platform.client.descriptor.editor;

import platform.client.descriptor.editor.base.NodeEditor;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementCheckBox;
import platform.client.logics.ClientComponent;

import javax.swing.*;
import java.awt.*;

public class ComponentEditor extends TitledPanel implements NodeEditor {
    protected final ClientComponent component;

    public ComponentEditor(String title, ClientComponent component) {
        super(title);

        this.component = component;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(new TitledPanel(null, new IncrementCheckBox("Показывать компонент", component, "show")));
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(new TitledPanel(null, new IncrementCheckBox("Компонент по умолчанию", component, "defaultComponent")));
        add(Box.createRigidArea(new Dimension(5, 5)));

        //todo: foreground, background, font
        
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
