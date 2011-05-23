package platform.client.descriptor.editor.base;

import javax.swing.*;
import java.awt.*;

public class NorthBoxPanel extends JPanel {

    public NorthBoxPanel(JComponent component){
        setLayout(new BorderLayout());
        add(component, BorderLayout.NORTH);
    }

    public NorthBoxPanel(JComponent... components){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        for(JComponent component : components){
            panel.add(component);
            panel.add(Box.createRigidArea(new Dimension(5, 5)));
        }
        setLayout(new BorderLayout());
        add(panel, BorderLayout.NORTH);
    }
}
