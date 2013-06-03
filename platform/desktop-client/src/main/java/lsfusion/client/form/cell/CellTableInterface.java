package lsfusion.client.form.cell;

import lsfusion.client.form.ClientFormController;
import lsfusion.client.logics.ClientGroupObjectValue;
import lsfusion.client.logics.ClientPropertyDraw;
import lsfusion.client.logics.classes.ClientType;

import java.awt.*;

public interface CellTableInterface {
    ClientType getCurrentEditType();
    Object getCurrentEditValue();

    boolean isPressed(int row, int column);

    ClientPropertyDraw getProperty(int row, int col);
    ClientGroupObjectValue getColumnKey(int row, int col);

    boolean isSelected(int row, int column);
    Color getBackgroundColor(int row, int column);
    Color getForegroundColor(int row, int column);

    ClientFormController getForm();
}
