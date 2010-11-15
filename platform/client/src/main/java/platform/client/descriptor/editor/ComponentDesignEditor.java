package platform.client.descriptor.editor;

import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementColorEditor;
import platform.client.descriptor.increment.editor.IncrementFontEditor;
import platform.client.logics.ClientComponent;

import javax.swing.*;
import java.awt.*;

public class ComponentDesignEditor extends TitledPanel {
    protected final ClientComponent component;

    public ComponentDesignEditor(String title, ClientComponent component) {
        super(title);

        this.component = component;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(new IncrementColorEditor("Цет фона: ", component, "background"));
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(new IncrementColorEditor("Цвет текста: ", component, "foreground"));
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(new IncrementFontEditor("Шрифт: ", component, "font"));
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(new IncrementFontEditor("Шрифт заголовка: ", component, "headerFont"));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
