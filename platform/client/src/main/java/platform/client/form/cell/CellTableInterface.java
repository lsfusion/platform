package platform.client.form.cell;

import platform.client.form.ClientFormController;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;

import java.awt.*;

public interface CellTableInterface {

    boolean isDataChanging();
    boolean isPressed(int row, int column);

    ClientPropertyDraw getProperty(int row, int col);
    ClientGroupObjectValue getKey(int row, int col);

    boolean isSelected(int row, int column);
    boolean isCellHighlighted(int row, int column);
    Color getHighlightColor(int row, int column);

    ClientFormController getForm();
}
