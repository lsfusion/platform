package lsfusion.client.form.panel;

import lsfusion.client.form.layout.JComponentPanel;
import lsfusion.client.logics.ClientToolbar;
import lsfusion.interop.form.layout.Alignment;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.interop.form.layout.FlexConstraints;
import lsfusion.interop.form.layout.FlexLayout;

import javax.swing.*;
import java.awt.*;

public class ToolbarView extends JComponentPanel {
    private JPanel mainPanel;

    public ToolbarView(ClientToolbar toolbar) {
        setLayout(new FlexLayout(this, false, Alignment.START));
        initBottomContainer();
        toolbar.installMargins(this);
    }

    private void initBottomContainer() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        add(mainPanel, new FlexConstraints(FlexAlignment.CENTER, 0));
    }

    public void addComponent(Component component) {
        mainPanel.add(component);
    }
}