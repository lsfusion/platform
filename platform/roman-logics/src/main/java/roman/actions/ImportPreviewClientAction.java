package roman.actions;


import platform.interop.action.ClientAction;
import platform.interop.action.ClientActionDispatcher;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.Date;
import java.util.Map;

public class ImportPreviewClientAction implements ClientAction {

    private Map<String, InvoiceProperties> invoiceList;


    public ImportPreviewClientAction(Map<String, InvoiceProperties> invoiceList) {
        this.invoiceList = invoiceList;
    }


    public Object dispatch(ClientActionDispatcher dispatcher) throws IOException {

        ImportPreviewDialog dialog = new ImportPreviewDialog(invoiceList, "Выберите импортируемые инвойсы");
        return dialog.execute();

    }

}

