package lsfusion.client.form.cell;

import lsfusion.client.SwingUtils;
import lsfusion.client.form.ClientFormController;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;

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

    public void setForegroundColor(Color foreground) {
        this.foregroundColor = foreground;
    }

    public Color getForegroundColor() {
        return foregroundColor;
    }

    public ClientFormController getForm() {
        return form;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getProperty().notNull) {
            SwingUtils.paintRightBottomCornerTriangle((Graphics2D) g, 7, Color.RED, 0, 0, getWidth(), getHeight());
        } else if (getProperty().hasChangeAction) {
            SwingUtils.paintRightBottomCornerTriangle((Graphics2D) g, 7, new Color(120, 170, 208), 0, 0, getWidth(), getHeight());
        }
    }
}
