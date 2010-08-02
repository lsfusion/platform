package platform.client.form;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.util.EventObject;

public abstract class ClientFormTable extends JTable {

    protected ClientFormTable() {
        super();

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setSurrendersFocusOnKeystroke(true);

        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        getTableHeader().setFocusable(false);
        getTableHeader().setReorderingAllowed(false);

        //  Have the enter key work the same as the tab key
        InputMap im = getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        KeyStroke tab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0);
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        im.put(enter, im.get(tab));

        setTransferHandler(new TransferHandler() {
            protected Transferable createTransferable(JComponent c) {
                if (c instanceof JTable) {
                    JTable table = (JTable) c;
                    int row = table.getSelectionModel().getLeadSelectionIndex();
                    int column = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();

                    if (row < 0 || row >= getRowCount() || column < 0 || column >= getColumnCount()) return null;

                    Object value = table.getValueAt(row, column);
                    if (value == null) {
                        return null;
                    }
                    return new StringSelection(value.toString());
                }

                return null;
            }

            @Override
            public boolean importData(JComponent c, Transferable t) {
                if (c == ClientFormTable.this) {
                    for (DataFlavor flavor : t.getTransferDataFlavors()) {
                        if (String.class.isAssignableFrom(flavor.getRepresentationClass())) {
                            String value = null;
                            try {
                                value = (String) t.getTransferData(flavor);
                            } catch (Exception ignored) {
                            }
                            if (value != null) {
                                JTable table = (JTable) c;
                                int row = table.getSelectionModel().getLeadSelectionIndex();
                                int column = table.getColumnModel().getSelectionModel().getLeadSelectionIndex();

                                Object oValue = convertValueFromString(value, row, column);
                                if (oValue != null) {
                                    table.setValueAt(oValue, row, column);
                                }

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
        });
    }

    public boolean editCellAt(int row, int column, EventObject e){
        boolean result = super.editCellAt(row, column, e);
        if (result) {
            final Component editor = getEditorComponent();
            if (editor instanceof JTextComponent) {
                ((JTextComponent) editor).selectAll();
            }
        }

        return result;
    }

    public abstract Object convertValueFromString(String value, int row, int column);

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {

        boolean consumed = super.processKeyBinding(ks, e, condition, pressed);
        // Вырежем F2, чтобы startEditing не поглощало его
        if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0))) return false;
        if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0))) return false;
        //noinspection SimplifiableIfStatement
        if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0))) return false;

        return consumed;
    }

    protected void selectRow(int rowNumber) {

        final int colSel = getColumnModel().getSelectionModel().getLeadSelectionIndex();
        if (colSel == -1)
            changeSelection(rowNumber, 0, false, false);
        else
            getSelectionModel().setLeadSelectionIndex(rowNumber);

        scrollRectToVisible(getCellRect(rowNumber, (colSel == -1) ? 0 : colSel, true));
    }
}
