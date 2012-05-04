package platform.client.form.cell;

import platform.client.form.ClientFormController;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import java.awt.*;

public class DataCellViewTable extends SingleCellTable {
    private final ClientFormController form;
    private final DataCellView valueCellView;

    private Color backgroundColor;
    private Color foregroundColor;

    public DataCellViewTable(ClientFormController form, DataCellView valueCellView, ClientGroupObjectValue columnKey, ClientPropertyDraw property) {
        super(false, columnKey);

        this.form = form;
        this.valueCellView = valueCellView;

        setProperty(property);
    }

    protected void forceChangeValue(Object value, boolean aggValue) {
        valueCellView.forceChangeValue(value, aggValue);
    }

    public boolean isPressed(int row, int column) {
        return false;
    }

    public Color getBackgroundColor(int row, int column) {
        return backgroundColor;
    }

    public void setBackgroundColor(Object background) {
        this.backgroundColor = (Color) background;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getForegroundColor(int row, int column) {
        return foregroundColor;
    }

    public void setForegroundColor(Object background) {
        this.foregroundColor = (Color) background;
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public ClientFormController getForm() {
        return form;
    }

    @Override
    public void buildShortcut(Component invoker, Point point) {
        form.controllers.get(getProperty().groupObject).showShortcut(invoker, point, getProperty());
    }

    @Override
    public boolean invokeDefaultAction(ClientPropertyDraw property) {
        return form.controllers.get(property.groupObject).invokeDefaultAction(property);
    }
}
