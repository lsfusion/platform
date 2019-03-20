package lsfusion.client.form.property.cell;

import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.classes.ClientType;

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
