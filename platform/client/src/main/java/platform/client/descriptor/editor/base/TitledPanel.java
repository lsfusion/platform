package platform.client.descriptor.editor.base;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class TitledPanel extends JPanel {
    public TitledPanel(String title, boolean vertical, Component component) {
        if (title != null) {
            TitledBorder border = BorderFactory.createTitledBorder(title);
            setBorder(border);
        }

        setLayout(new BorderLayout());
        add(BorderLayout.CENTER, component);
    }
}
