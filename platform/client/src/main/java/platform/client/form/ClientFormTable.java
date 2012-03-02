package platform.client.form;

import platform.client.form.cell.ClientAbstractCellEditor;
import platform.client.form.cell.ClientAbstractCellRenderer;
import platform.interop.KeyStrokes;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;

public abstract class ClientFormTable extends JTable implements TableTransferHandler.TableInterface {

    protected ClientFormTable() {
        this(null);
    }


    abstract protected boolean isEditOnSingleClick(int row, int column);

    protected ClientFormTable(TableModel model) {
        super(model);

        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        setSurrendersFocusOnKeystroke(true);

        putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        getTableHeader().setFocusable(false);
        getTableHeader().setReorderingAllowed(false);

        setDefaultRenderer(Object.class, new ClientAbstractCellRenderer());
        setDefaultEditor(Object.class, new ClientAbstractCellEditor());

        setupActionMap();

        setTransferHandler(new TableTransferHandler() {
            @Override
            protected TableInterface getTable() {
                return ClientFormTable.this;
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    buildShortcut(e.getComponent(), e.getPoint());
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_CONTEXT_MENU) {
                    Rectangle rect = getCellRect(getSelectedRow(), getSelectedColumn(), true);
                    buildShortcut(getComponentAt(rect.getLocation()), new Point(rect.x, rect.y + rect.height - 1));
                }
            }
        });
    }

    protected ClientAbstractCellEditor getAbstractCellEditor(int row, int column) {
        TableCellEditor editor = getCellEditor(row, column);
        return editor instanceof ClientAbstractCellEditor
               ? (ClientAbstractCellEditor) editor
               : null;
    }

    abstract public void buildShortcut(Component invoker,  Point point);

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
            if (selRow != -1 && selRow != row && !isEditOnSingleClick(row, column)) {
                return false;
            }
        }

        boolean result = super.editCellAt(row, column, e);
        if (result) {
            final Component editor = getEditorComponent();
            if (editor instanceof JTextComponent) {
                JTextComponent textEditor = (JTextComponent) editor;
                textEditor.selectAll();
                if (clearText(row, column, e)) {
                    textEditor.setText("");
                }
            }
        }

        return result;
    }

    public boolean clearText(int row, int column, EventObject e) {
       return false;
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        // проверяем на editPerformed, чтобы предотвратить редактирование свойства с editKey после редактирования текущего по нажатию клавиши
        ClientAbstractCellEditor cellEditor = getAbstractCellEditor(getSelectionModel().getLeadSelectionIndex(),
                getColumnModel().getSelectionModel().getLeadSelectionIndex());
        if (cellEditor != null)
            cellEditor.editPerformed = false;

        boolean consumed = super.processKeyBinding(ks, e, condition, pressed);
        // Вырежем кнопки фильтров, чтобы startEditing не поглощало его
        if (ks.equals(KeyStrokes.getFindKeyStroke(0))) return false;
        if (ks.equals(KeyStrokes.getFilterKeyStroke(0))) return false;
        //noinspection SimplifiableIfStatement
        if (ks.equals(KeyStrokes.getF8())) return false;

        return consumed || cellEditor != null && cellEditor.editPerformed;
    }
}
