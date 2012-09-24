package platform.client.form.panel;

import platform.client.ClientResourceBundle;
import platform.client.StartupProperties;
import platform.client.form.queries.ToolbarGridButton;

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

        infoLabel = new JLabel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(super.getPreferredSize().width, ToolbarGridButton.DEFAULT_SIZE.height);
            }
        };
        infoLabel.setMinimumSize(new Dimension(1, ToolbarGridButton.DEFAULT_SIZE.height));

        add(mainPanel, BorderLayout.WEST);
        add(infoLabel, BorderLayout.CENTER);
    }

    public void addComponent(Component component) {
        mainPanel.add(component);
    }

    public boolean updateSelectionInfo(int quantity, String sum, String avg) {
        String text = "";
        text += avg == null ? "" : ClientResourceBundle.getString("form.grid.selection.average") + (StartupProperties.dotSeparator ? avg.replace(',', '.') : avg) + "  ";
        text += sum == null ? "" : ClientResourceBundle.getString("form.grid.selection.sum") + (StartupProperties.dotSeparator ? sum.replace(',', '.') : sum) + "  ";
        text += quantity <= 1 ? "" : ClientResourceBundle.getString("form.grid.selection.quantity") + quantity;
        if (!text.equals(infoLabel.getText())) {
            infoLabel.setText(text);
            infoLabel.setVisible(!text.isEmpty());
            infoLabel.invalidate();
            return true;
        }
        return false;
    }
}
