package platform.client.form.cell;

import platform.client.form.ClientForm;
import platform.client.logics.ClientCellView;

import java.util.EventObject;

public interface ClientCellViewTable {

    boolean isDataChanging();
    ClientCellView getCellView(int col);
    ClientForm getForm();

    void setEditEvent(EventObject editEvent);
}
