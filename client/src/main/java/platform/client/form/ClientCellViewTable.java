package platform.client.form;

import platform.client.logics.ClientCellView;

interface ClientCellViewTable {

    boolean isDataChanging();
    ClientCellView getCellView(int row, int col);
}
