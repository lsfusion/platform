package lsfusion.client.form.object.table.view;

import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.object.table.ClientToolbar;
import lsfusion.interop.base.view.FlexAlignment;
import lsfusion.interop.base.view.FlexConstraints;

import javax.swing.*;
import java.awt.*;

public class ToolbarView extends FlexPanel {
    private JPanel mainPanel;

    public ToolbarView(ClientToolbar toolbar) {
        super(false);
        initBottomContainer();
        toolbar.installMargins(this);
    }

    private void initBottomContainer() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        add(mainPanel, new FlexConstraints(FlexAlignment.CENTER, 0));
    }
    
    public void addSeparator() {
        if (!isEmpty()) {
            addComponent(Box.createHorizontalStrut(2));
            addComponent(new JSeparator(SwingConstants.VERTICAL));
            addComponent(Box.createHorizontalStrut(2));
        }
    }
    
    public boolean isEmpty() {
        return mainPanel.getComponentCount() == 0;
    }

    public void addComponent(Component component) {
        mainPanel.add(component);
    }
}