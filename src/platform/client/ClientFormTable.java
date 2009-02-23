package platform.client;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import java.awt.event.KeyEvent;
import java.awt.*;

public class ClientFormTable extends JTable {

    public ClientFormTable() {
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

    }

    // Решение проблемы конфликта JTable и DockingFrames
    // Так сделано, чтобы setNextFocusableComponent в super.prepareEditor "словил" не DockFocusTraversalPolicy
    // у BasicDockableDisplayer (так как он при этом зацикливается), а DefaultFocusTraversalPolicy у JTable.

    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        setFocusCycleRoot(true);
        return super.prepareEditor(editor, row, column);
    }

    @Override
    public void removeEditor() {
        super.removeEditor();
        setFocusCycleRoot(false);
        setFocusTraversalPolicy(new DefaultFocusTraversalPolicy());
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {

        boolean consumed = super.processKeyBinding(ks, e, condition, pressed);
        // Вырежем F2, чтобы startEditing не поглощало его
        if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0))) return false;
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
