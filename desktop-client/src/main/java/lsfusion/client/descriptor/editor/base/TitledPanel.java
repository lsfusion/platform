package lsfusion.client.descriptor.editor.base;

import javax.swing.*;
import java.awt.*;

public class TitledPanel extends JPanel {
    public TitledPanel() {
        this(null);
    }

    public TitledPanel(String title) {
        this(title, new BorderLayout());
    }

    public TitledPanel(String title, LayoutManager layout) {
        super(layout);
        
        if (title != null) {
            setBorder( BorderFactory.createTitledBorder(title) );
        }
    }
    
    public TitledPanel(String title, Component component) {
        this(title);

        add(BorderLayout.CENTER, component);
    }
}