package platform.client.form;

import platform.client.interop.ClientCellView;

interface ClientCellViewTable {

    boolean isDataChanging();
    ClientCellView getCellView(int row, int col);
}
