package lsfusion.client.form.user.queries;

import lsfusion.client.form.layout.JComponentPanel;
import lsfusion.interop.form.layout.Alignment;
import lsfusion.interop.form.layout.FlexAlignment;
import lsfusion.interop.form.layout.FlexConstraints;
import lsfusion.interop.form.layout.FlexLayout;

import javax.swing.*;

import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.StartupProperties.dotSeparator;

public class CalculationsView extends JComponentPanel {
    private JLabel averageLabel = new JLabel();
    private JLabel sumLabel = new JLabel();
    private JLabel quantityLabel = new JLabel();
    
    public CalculationsView() {
        setLayout(new FlexLayout(this, false, Alignment.START));
        add(averageLabel, new FlexConstraints(FlexAlignment.CENTER, 0));
        add(sumLabel, new FlexConstraints(FlexAlignment.CENTER, 0));
        add(quantityLabel, new FlexConstraints(FlexAlignment.CENTER, 0));
    }

    public void updateSelectionInfo(int quantity, String sum, String avg) {
        String avgString = avg == null ? "" : getString("form.grid.selection.average") + ": " + (dotSeparator ? avg.replace(',', '.') : avg) + "  ";
        String sumString = sum == null ? "" : getString("form.grid.selection.sum") + ": " + (dotSeparator ? sum.replace(',', '.') : sum) + "  ";
        String quantityString = quantity <= 1 ? "" : getString("form.grid.selection.quantity") + ": " + quantity;

        updateLabel(averageLabel, avgString);
        updateLabel(sumLabel, sumString);
        updateLabel(quantityLabel, quantityString);
    }
    
    private void updateLabel(JLabel label, String text) {
        if (!text.equals(label.getText())) {
            label.setText(text);
            label.setVisible(!text.isEmpty());
            label.invalidate();
        }
    }
}
