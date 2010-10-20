package platform.client.descriptor.editor.logics;

import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementCheckBox;
import platform.client.logics.ClientComponent;

import javax.swing.*;
import java.awt.*;

public class ClientComponentEditor extends TitledPanel {
    protected final ClientComponent component;

    public ClientComponentEditor(String title, ClientComponent component) {
        super(title);

        this.component = component;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(new TitledPanel(null, new IncrementCheckBox("Показывать компонент", component, "show")));
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(new TitledPanel(null, new IncrementCheckBox("Компонент по умолчанию", component, "defaultComponent")));
        add(Box.createRigidArea(new Dimension(5, 5)));

        //todo: foreground, background, font
        
    }
}
