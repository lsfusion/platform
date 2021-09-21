package lsfusion.client.form.object.table.grid.user.toolbar.view;

import lsfusion.client.form.design.view.FlexPanel;
import lsfusion.client.form.design.view.widget.LabelWidget;
import lsfusion.interop.base.view.FlexAlignment;

import javax.swing.*;

import static lsfusion.client.ClientResourceBundle.getString;
import static lsfusion.client.StartupProperties.dotSeparator;

public class CalculationsView extends FlexPanel {
    private LabelWidget averageLabel = new LabelWidget();
    private LabelWidget sumLabel = new LabelWidget();
    private LabelWidget quantityLabel = new LabelWidget();
    
    public CalculationsView() {
        super(false);
        add(averageLabel, FlexAlignment.CENTER, 0.0);
        add(sumLabel, FlexAlignment.CENTER, 0.0);
        add(quantityLabel, FlexAlignment.CENTER, 0.0);
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
