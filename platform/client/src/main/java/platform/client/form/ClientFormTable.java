package platform.client.form;

import platform.interop.KeyStrokes;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.EventObject;

public abstract class ClientFormTable extends JTable {

    protected ClientFormTable() {
        this(null);
    }

    protected ClientFormTable(TableModel model) {
        super(model);

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setSurrendersFocusOnKeystroke(true);

        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        getTableHeader().setFocusable(false);
        getTableHeader().setReorderingAllowed(false);

        setupActionMap();

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

    private void setupActionMap() {
        //  Have the enter key work the same as the tab key
        InputMap im = getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        im.put(KeyStrokes.getEnter(), im.get(KeyStrokes.getTab()));
    }

    public boolean editCellAt(int row, int column, EventObject e){
        if (e instanceof MouseEvent) {
            // чтобы не срабатывало редактирование при изменении ряда,
            // потому что всё равно будет апдейт
            int selRow = getSelectedRow();
            if (selRow != -1 && selRow != row) {
                return false;
            }
        }

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
        // Вырежем кнопки фильтров, чтобы startEditing не поглощало его
        if (ks.equals(KeyStrokes.getFindKeyStroke(0))) return false;
        if (ks.equals(KeyStrokes.getFilterKeyStroke(0))) return false;
        //noinspection SimplifiableIfStatement
        if (ks.equals(KeyStrokes.getF8())) return false;

        return consumed;
    }
}
