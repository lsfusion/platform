package platform.client.form.cell;

import platform.client.form.ClientFormController;
import platform.client.logics.ClientPropertyDraw;

public interface CellTableInterface {

    boolean isDataChanging();
    ClientPropertyDraw getProperty(int col);
    ClientFormController getForm();
}
