package platform.client.form.cell;

import platform.client.logics.ClientCellView;
import platform.client.form.ClientForm;

public interface ClientCellViewTable {

    boolean isDataChanging();
    ClientCellView getCellView(int col);
    ClientForm getForm();
}
