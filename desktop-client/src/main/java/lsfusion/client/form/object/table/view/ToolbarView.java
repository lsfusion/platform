package lsfusion.client.form.object.table.view;

import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.design.view.widget.PanelWidget;
import lsfusion.client.form.object.table.ClientToolbar;
import lsfusion.interop.base.view.FlexAlignment;

import javax.swing.*;
import java.awt.*;

public class ToolbarView extends FlexPanel {
    private PanelWidget mainPanel;

    public ToolbarView(ClientToolbar toolbar) {
        super(false);
        initBottomContainer();
//        toolbar.installMargins(this);
    }

    private void initBottomContainer() {
        mainPanel = new PanelWidget();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        add(mainPanel, FlexAlignment.CENTER, 0.0);
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