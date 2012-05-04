package platform.client.descriptor.editor.base;

import javax.swing.*;
import java.awt.*;

public class NamedContainer extends JPanel {

    public NamedContainer(String caption, boolean vertical, Component component) {
        setLayout(new BoxLayout(this, vertical ? BoxLayout.Y_AXIS : BoxLayout.X_AXIS));

        add(new JLabel(caption));
        add(Box.createRigidArea(new Dimension(5, 5)));
        add(component);
    }
}
