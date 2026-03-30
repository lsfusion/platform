package lsfusion.client.form.property.table.view;

import lsfusion.base.BaseUtils;
import lsfusion.client.form.object.table.grid.view.GridTable;
import lsfusion.client.form.property.ClientPropertyDraw;

import javax.swing.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.singletonList;

public class TableTransferHandler extends TransferHandler {

    private final TableInterface table;

    public interface TableInterface {
        boolean richTextSelected();
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

    private boolean checkFlavor(DataFlavor flavor, boolean rich) {
        return (rich && flavor.getHumanPresentableName().equals("text/html")) || flavor.getHumanPresentableName().equals("text/plain") || flavor.getHumanPresentableName().equals("Unicode String");
    }

    public static List<List<String>> getClipboardTable(String line) {
        List<List<String>> table = new ArrayList<>();
        List<String> row = new ArrayList<>();

        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;
        boolean cellStarted = false;
        boolean quotedCell = false;
        boolean quotedCellClosed = false;
        boolean separatorInsideQuotes = false;
        boolean escapedQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);

            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        cell.append('"');
                        escapedQuote = true;
                        i++;
                    } else {
                        inQuotes = false;
                        quotedCellClosed = true;
                    }
                } else {
                    if (ch == '\t' || ch == '\n' || ch == '\r') {
                        separatorInsideQuotes = true;
                    }
                    cell.append(ch);
                }
                cellStarted = true;
            } else if (!cellStarted && ch == '"') {
                inQuotes = true;
                cellStarted = true;
                quotedCell = true;
            } else if (ch == '\t') {
                addClipboardCell(row, cell, quotedCell, quotedCellClosed, separatorInsideQuotes, escapedQuote);
                cellStarted = false;
                quotedCell = false;
                quotedCellClosed = false;
                separatorInsideQuotes = false;
                escapedQuote = false;
            } else if (ch == '\n') {
                addClipboardCell(row, cell, quotedCell, quotedCellClosed, separatorInsideQuotes, escapedQuote);
                table.add(row);
                row = new ArrayList<>();
                cellStarted = false;
                quotedCell = false;
                quotedCellClosed = false;
                separatorInsideQuotes = false;
                escapedQuote = false;
            } else if (ch == '\r') {
                addClipboardCell(row, cell, quotedCell, quotedCellClosed, separatorInsideQuotes, escapedQuote);
                table.add(row);
                row = new ArrayList<>();
                cellStarted = false;
                quotedCell = false;
                quotedCellClosed = false;
                separatorInsideQuotes = false;
                escapedQuote = false;
                if (i + 1 < line.length() && line.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                cell.append(ch);
                cellStarted = true;
            }
        }

        if (cellStarted || !row.isEmpty()) {
            addClipboardCell(row, cell, quotedCell, quotedCellClosed, separatorInsideQuotes, escapedQuote);
            table.add(row);
        } else if (table.isEmpty()) {
            row.add(null);
            table.add(row);
        }

        return table;
    }

    private static void addClipboardCell(List<String> row, StringBuilder cell, boolean quotedCell, boolean quotedCellClosed,
                                         boolean separatorInsideQuotes, boolean escapedQuote) {
        row.add(BaseUtils.nullEmpty(getClipboardCellValue(cell, quotedCell, quotedCellClosed, separatorInsideQuotes, escapedQuote)));
        cell.setLength(0);
    }

    private static String getClipboardCellValue(StringBuilder cell, boolean quotedCell, boolean quotedCellClosed,
                                                boolean separatorInsideQuotes, boolean escapedQuote) {
        String value = cell.toString();
        if (!quotedCell) {
            return value;
        }
        if (quotedCellClosed && (separatorInsideQuotes || escapedQuote)) {
            return value;
        }
        return quotedCellClosed ? "\"" + value + "\"" : "\"" + value;
    }

    @Override
    public boolean importData(JComponent c, Transferable t) {
        if (c == table) {
            boolean rich = table.richTextSelected();
            for (DataFlavor flavor : t.getTransferDataFlavors()) {
                if (String.class.isAssignableFrom(flavor.getRepresentationClass()) && checkFlavor(flavor, rich)) {
                    String value = null;
                    try {
                        value = (String) t.getTransferData(flavor);
                    } catch (Exception ignored) {
                    }
                    if (value != null) {
                        List<List<String>> clipboardTable = rich ? singletonList(singletonList(value)) : getClipboardTable(value);
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
