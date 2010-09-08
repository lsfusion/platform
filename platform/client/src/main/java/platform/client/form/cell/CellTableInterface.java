package platform.client.form.cell;

import platform.client.form.ClientFormController;
import platform.client.logics.ClientCell;

import java.util.EventObject;

public interface CellTableInterface {

    boolean isDataChanging();
    ClientCell getCell(int col);
    ClientFormController getForm();
}
