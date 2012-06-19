package platform.client.form;

import platform.base.BaseUtils;
import platform.client.form.grid.GridTable;
import platform.client.logics.ClientPropertyDraw;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TableTransferHandler extends TransferHandler {

    private final TableInterface table;

    public interface TableInterface {
        ClientPropertyDraw getProperty(int row, int column);
        void pasteTable(List<List<String>> table);
    }

    public TableTransferHandler(TableInterface table) {
        this.table = table;
    }

    protected Transferable createTransferable(JComponent c) {
        if (c instanceof GridTable) {
            try {
                return new StringSelection(((GridTable) c).getSelectedTable());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else if (c instanceof TableInterface) {
            JTable table = (JTable) c;
            int row = table.getSelectionModel().getLeadSelectionIndex();
            int column = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();

            if (row < 0 || row >= table.getRowCount() || column < 0 || column >= table.getColumnCount()) {
                return null;
            }

            Object value = table.getValueAt(row, column);
            if (value == null) {
                return null;
            }
            if (value instanceof String) {
                value = BaseUtils.rtrim((String) value);
            }

            TableInterface transferTable = (TableInterface) table;
            ClientPropertyDraw property = transferTable.getProperty(row, column);
            if (property != null) {
                try {
                    return new StringSelection(property.formatString(value));
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    private boolean checkFlavor(DataFlavor flavor) {
        return (flavor.getHumanPresentableName().equals("text/plain") || flavor.getHumanPresentableName().equals("Unicode String"));
    }

    List<List<String>> getClipboardTable(String line) {
        List<List<String>> table = new ArrayList<List<String>>();
        Scanner rowScanner = new Scanner(line).useDelimiter("\n");
        while (rowScanner.hasNext()) {
            String rowString = rowScanner.nextLine();
            List<String> row = new ArrayList<String>();
            Scanner cellScanner = new Scanner(rowString).useDelimiter("\t");
            if (rowString.startsWith("\t") || rowString.isEmpty()) {
                row.add(null);
            }
            while (cellScanner.hasNext()) {
                String cell = BaseUtils.nullEmpty(cellScanner.next());
                row.add(cell);
            }
            if (rowString.endsWith("\t")) {
                row.add(null);
            }
            table.add(row);
        }
        if (line.equals("\n") || line.endsWith("\n\n") || line.equals("")) {
            List<String> row = new ArrayList<String>();
            if (table.isEmpty()) {
                row.add(null);
            } else {
                for (int i = 0; i < table.get(0).size(); i++) {
                    row.add(null);
                }
            }
            table.add(row);
        }
        return table;
    }

    @Override
    public boolean importData(JComponent c, Transferable t) {
        if (c == table) {
            for (DataFlavor flavor : t.getTransferDataFlavors()) {
                if (String.class.isAssignableFrom(flavor.getRepresentationClass()) && checkFlavor(flavor)) {
                    String value = null;
                    try {
                        value = (String) t.getTransferData(flavor);
                    } catch (Exception ignored) {
                    }
                    if (value != null) {
                        List<List<String>> clipboardTable = getClipboardTable(value);
                        table.pasteTable(clipboardTable);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public int getSourceActions(JComponent c) {
        return COPY;
    }
}
