package platform.client.form;

import platform.interop.ClientCellView;

interface ClientCellViewTable {

    boolean isDataChanging();
    ClientCellView getCellView(int row, int col);
}
