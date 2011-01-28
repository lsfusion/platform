package platform.client.form.cell;

import platform.client.form.ClientFormController;
import platform.client.logics.ClientPropertyDraw;

import java.awt.*;

public interface CellTableInterface {

    boolean isDataChanging();

    ClientPropertyDraw getProperty(int col);
    boolean isCellHighlighted(int row, int column);
    Color getHighlightColor(int row, int column);

    ClientFormController getForm();
}
