package lsfusion.client.form.panel;

import lsfusion.client.ClientResourceBundle;
import lsfusion.client.StartupProperties;

import javax.swing.*;
import java.awt.*;

public class ToolbarView extends JPanel {
    private JPanel mainPanel;

    private JLabel infoLabel;

    public ToolbarView() {
        super(new BorderLayout());
        initBottomContainer();
    }

    private void initBottomContainer() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.X_AXIS));
        mainPanel.setAlignmentY(Component.TOP_ALIGNMENT);

        infoLabel = new JLabel();

        add(mainPanel, BorderLayout.WEST);
        add(infoLabel, BorderLayout.CENTER);
    }

    public void addComponent(Component component) {
        mainPanel.add(component);
    }

    public void updateSelectionInfo(int quantity, String sum, String avg) {
        String text = "";
        text += avg == null ? "" : ClientResourceBundle.getString("form.grid.selection.average") + (StartupProperties.dotSeparator ? avg.replace(',', '.') : avg) + "  ";
        text += sum == null ? "" : ClientResourceBundle.getString("form.grid.selection.sum") + (StartupProperties.dotSeparator ? sum.replace(',', '.') : sum) + "  ";
        text += quantity <= 1 ? "" : ClientResourceBundle.getString("form.grid.selection.quantity") + quantity;
        if (!text.equals(infoLabel.getText())) {
            infoLabel.setText(text);
            infoLabel.setVisible(!text.isEmpty());
            infoLabel.invalidate();
        }
    }
}