package platform.client.form.cell;

import platform.client.form.ClientFormController;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import java.awt.*;

public class DataPanelViewTable extends SingleCellTable {
    private final ClientFormController form;

    private Color backgroundColor;
    private Color foregroundColor;

    public DataPanelViewTable(ClientFormController form, ClientGroupObjectValue columnKey, ClientPropertyDraw property) {
        super(columnKey);

        this.form = form;

        setProperty(property);
    }

    public boolean isPressed(int row, int column) {
        return false;
    }

    public Color getBackgroundColor(int row, int column) {
        return backgroundColor;
    }

    public void setBackgroundColor(Color background) {
        this.backgroundColor = background;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getForegroundColor(int row, int column) {
        return foregroundColor;
    }

    public void setForegroundColor(Color background) {
        this.foregroundColor = background;
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public ClientFormController getForm() {
        return form;
    }
}
