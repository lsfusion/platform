package lsfusion.client.descriptor.editor.base;

import javax.swing.*;
import java.awt.*;

public class NorthBoxPanel extends JPanel {
    JPanel boxPanel = new JPanel();

    public NorthBoxPanel(JComponent... components) {
        boxPanel.setLayout(new BoxLayout(boxPanel, BoxLayout.Y_AXIS));
        addComponents(components);
        setLayout(new BorderLayout());
        add(boxPanel, BorderLayout.NORTH);
    }

    public void addComponents(JComponent... components) {
        if (getComponents().length == 0) {    //если вдруг где-то вызвали removeAll()
            add(boxPanel, BorderLayout.NORTH);
        }
        for (JComponent component : components) {
            boxPanel.add(component);
            boxPanel.add(Box.createRigidArea(new Dimension(5, 5)));
        }
    }
}
