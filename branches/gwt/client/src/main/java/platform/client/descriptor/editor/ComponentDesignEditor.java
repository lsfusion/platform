package platform.client.descriptor.editor;

import platform.client.descriptor.editor.base.NorthBoxPanel;
import platform.client.descriptor.editor.base.TitledPanel;
import platform.client.descriptor.increment.editor.IncrementColorEditor;
import platform.client.descriptor.increment.editor.IncrementFontEditor;
import platform.interop.ComponentDesign;

import javax.swing.*;
import java.awt.*;

public class ComponentDesignEditor extends TitledPanel {

    public ComponentDesignEditor(String title, ComponentDesign design) {
        super(title);

        JPanel panel1 = new JPanel();
        panel1.setLayout(new FlowLayout(FlowLayout.LEFT));
        panel1.add(new IncrementColorEditor("Цвет фона: ", design, "background"));
        panel1.add(new IncrementColorEditor("Цвет текста: ", design, "foreground"));

        JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayout(1, 2, 5, 5));
        panel2.add(new IncrementFontEditor("Шрифт заголовка: ", design, "headerFont"));
        panel2.add(new IncrementFontEditor("Шрифт: ", design, "font"));

        add(new NorthBoxPanel(panel1, panel2));
    }

    public JComponent getComponent() {
        return this;
    }

    public boolean validateEditor() {
        return true;
    }
}
