package platform.client.form.cell;

import platform.client.form.ClientFormController;
import platform.client.logics.ClientPropertyDraw;

import java.awt.*;

public interface CellTableInterface {

    boolean isDataChanging();

    ClientPropertyDraw getProperty(int col);
    Object getHighlightValue(int row);
    Color getHighlightColor();

    ClientFormController getForm();
}
