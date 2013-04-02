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

    public static List<List<String>> getClipboardTable(String line) {
        List<List<String>> table = new ArrayList<List<String>>();
        List<String> row = new ArrayList<String>();

        char[] charline = line.toCharArray();

        int quotesCount = 0;
        boolean quotesOpened = false;
        boolean hasSeparator = false;
        boolean isFirst = true;

        int start = 0;

        for (int i = 0; i <= charline.length; i++) {
            boolean isCellEnd, isRowEnd, isQuote = false, isSeparator;

            boolean isLast = i >= charline.length;
            if (!isLast) {
                isCellEnd = charline[i] == '\t';
                isRowEnd = charline[i] == '\n';
                isQuote = charline[i] == '"';
                isSeparator = isCellEnd || isRowEnd;
            } else {
                if (isFirst)
                    break;
                isRowEnd = true;
                isSeparator = true;
            }

            if (quotesOpened) {
                if (isQuote)
                    quotesCount++;
                else {
                    if (isSeparator) {
                        if (quotesCount % 2 == 1 || isLast) {
                            quotesOpened = false;
                            String cell = line.substring(hasSeparator ? start : (start - 1), hasSeparator ? (i - 1) : i).replace("\"\"", "\"");
                            row.add(BaseUtils.nullEmpty(cell));
                            start = i;
                            if (isRowEnd) {
                                table.add(row);
                                row = new ArrayList<String>();
                            }
                            isFirst = true;
                            hasSeparator = false;
                        } else {
                            hasSeparator = true;
                        }
                    } else
                        quotesCount = 0;
                }

            } else {
                if (isSeparator) {
                    row.add(BaseUtils.nullEmpty(line.substring(start, i)));
                    start = i;
                    if (isRowEnd) {
                        table.add(row);
                        row = new ArrayList<String>();
                    }
                    isFirst = true;
                } else if (isFirst) {
                    if (isQuote) {
                        start = i + 1;
                        quotesOpened = true;
                    } else {
                        start = i;
                    }
                    isFirst = false;
                }
            }
        }

        if (table.isEmpty()) {
            row.add(null);
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
