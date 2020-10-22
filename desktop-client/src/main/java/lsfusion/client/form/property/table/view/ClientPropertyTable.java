package lsfusion.client.form.property.table.view;

import com.google.common.base.Preconditions;
import lsfusion.base.BaseUtils;
import lsfusion.base.SystemUtils;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.classes.ClientActionClass;
import lsfusion.client.classes.ClientType;
import lsfusion.client.classes.data.ClientTextClass;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.cell.EditBindingMap;
import lsfusion.client.form.property.cell.controller.ClientAbstractCellEditor;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;
import lsfusion.client.form.property.cell.view.ClientAbstractCellRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.interop.form.event.BindingMode;
import lsfusion.interop.form.event.KeyInputEvent;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.ParseException;
import java.util.EventObject;

import static lsfusion.client.form.property.cell.EditBindingMap.*;

public abstract class ClientPropertyTable extends JTable implements TableTransferHandler.TableInterface, CellTableInterface {
    private final EditPropertyDispatcher editDispatcher;
    protected final EditBindingMap editBindingMap = new EditBindingMap(true);
    private final CellTableContextMenuHandler contextMenuHandler = new CellTableContextMenuHandler(this);
    
    protected final ClientFormController form;
    protected final ClientGroupObject groupObject;

    protected EventObject editEvent;
    protected int editRow;
    protected int editCol;
    protected ClientType currentEditType;
    protected Object currentEditValue;
    protected boolean editPerformed;
    protected boolean commitingValue;
    
    protected ClientPropertyTable(TableModel model, ClientFormController form, ClientGroupObject groupObject) {
        this(model, form, groupObject, new ClientAbstractCellRenderer());
    }

    protected ClientPropertyTable(TableModel model, ClientFormController form, ClientGroupObject groupObject, TableCellRenderer tableCellRenderer) {
        super(model);
        
        this.form = form;
        this.groupObject = groupObject;

        editDispatcher = new EditPropertyDispatcher(this, form.getDispatcherListener());

        SwingUtils.setupClientTable(this);

        setDefaultRenderer(Object.class, tableCellRenderer);
        setDefaultEditor(Object.class, new ClientAbstractCellEditor(this));

        initializeActionMap();

        contextMenuHandler.install();
    }

    private void initializeActionMap() {
        //  Have the enter key work the same as the tab key
        if(groupObject != null) {
            form.addBinding(new KeyInputEvent(KeyStrokes.getEnter()), getEnterBinding(false));
            form.addBinding(new KeyInputEvent(KeyStrokes.getShiftEnter()), getEnterBinding(true));
        }
    }

    private ClientFormController.Binding getEnterBinding(boolean shiftPressed) {
        ClientFormController.Binding binding = new ClientFormController.Binding(groupObject, -100) {
            @Override
            public boolean pressed(KeyEvent ke) {
                tabAction(!shiftPressed);
                return true;
            }
            @Override
            public boolean showing() {
                return true;
            }
        };
        binding.bindEditing = BindingMode.NO;
        binding.bindGroup = BindingMode.ONLY;
        return binding;
    }

    protected abstract void tabAction(boolean forward);

    public ClientType getCurrentEditType() {
        return currentEditType;
    }

    public Object getCurrentEditValue() {
        return currentEditValue;
    }
    
    @Override
    public Object getEditValue() {
        if(!checkEditBounds())
            return null;
        return getValueAt(editRow, editCol);
    }

    public boolean editCellAt(int row, int column, EventObject e) {
        if (!form.commitCurrentEditing()) {
            return false;
        }

        if (row < 0 || row >= getRowCount() || column < 0 || column >= getColumnCount()) {
            return false;
        }

        ClientPropertyDraw property = getProperty(row, column);
        ClientGroupObjectValue columnKey = getColumnKey(row, column);

        String actionSID = getPropertyEventActionSID(e, property, editBindingMap);

        if (actionSID == null) {
            return false;
        }

        if (isEditableAwareEditEvent(actionSID) && !isCellEditable(row, column)) {
            return false;
        }

        quickLog("formTable.editCellAt: " + e);

        //здесь немного запутанная схема...
        //executePropertyEventAction возвращает true, если редактирование произошло на сервере, необязательно с вводом значения...
        //но из этого editCellAt мы должны вернуть true, только если началось редактирование значения
        editPerformed = edit(property, columnKey, actionSID, row, column, e);
        return editorComp != null;
    }

    public boolean edit(ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID, int row, int column, EventObject e) {
        editRow = row;
        editCol = column;
        commitingValue = false;
        editEvent = e;

        return editDispatcher.executePropertyEventAction(property, columnKey, actionSID, editEvent);
    }

    public abstract int getCurrentRow();

    // edit cell can be out of bounds because of grid change
    private boolean checkEditBounds() {
        return editRow < getRowCount() && editCol < getColumnCount();
    }

    public boolean requestValue(ClientType valueType, Object oldValue) {
        quickLog("formTable.requestValue: " + valueType);

        //пока чтение значения можно вызывать только один раз в одном изменении...
        //если получится безусловно задержать фокус, то это ограничение можно будет убрать
        Preconditions.checkState(!commitingValue, "You can request value only once per edit action.");

        // need this because we use getTableCellEditorComponent infrastructure and we need to pass currentEditValue there somehow
        currentEditType = valueType;
        currentEditValue = oldValue;

        if (!super.editCellAt(editRow, editCol, editEvent)) {
            return false;
        }

        // is checked in upper call
        assert checkEditBounds();
        prepareTextEditor();

        editorComp.requestFocusInWindow();

        form.setCurrentEditingTable(this);

        return true;
    }

    public void prepareTextEditor() {
        ClientPropertyDraw property = getProperty(editRow, editCol);
        if (editorComp instanceof JTextComponent) {
            JTextComponent textEditor = (JTextComponent) editorComp;
            if(!property.notSelectAll) {
                textEditor.selectAll();
            }
            if (getProperty(editRow, editCol).clearText) {
                textEditor.setText("");
            }
        } else if (editorComp instanceof ClientPropertyTableEditorComponent) {
            ClientPropertyTableEditorComponent propertyTableEditorComponent = (ClientPropertyTableEditorComponent) editorComp;
            propertyTableEditorComponent.prepareTextEditor(property.clearText, !property.notSelectAll);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component component = super.prepareEditor(editor, row, column);
        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent) component;
            // у нас есть возможность редактировать нефокусную таблицу, и тогда после редактирования фокус теряется,
            // поэтому даём возможность FocusManager'у самому поставить фокус
            if (!isFocusable() && jComponent.getNextFocusableComponent() == this) {
                jComponent.setNextFocusableComponent(null);
                return component;
            }
        }
        return component;
    }

    public void updateEditValue(Object value) {
        if(checkEditBounds())
            setValueAt(value, editRow, editCol);
    }

    @Override
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {
            Object value = editor.getCellEditorValue();
            internalRemoveEditor();
            commitValue(value);
        }
    }

    private void commitValue(Object value) {
        quickLog("formTable.commitValue: " + value);
        commitingValue = true;
        editDispatcher.commitValue(value);
        form.clearCurrentEditingTable(this);
    }

    @Override
    public void editingCanceled(ChangeEvent e) {
        quickLog("formTable.cancelEdit");
        internalRemoveEditor();
        editDispatcher.cancelEdit();
        form.clearCurrentEditingTable(this);
    }

    @SuppressWarnings("deprecation")
    protected void internalRemoveEditor() {
        Component nextComp = null;
        if (editorComp instanceof JComponent) {
            nextComp = ((JComponent) editorComp).getNextFocusableComponent();
        }

        //copy/paste из JTable
        // изменён, чтобы не запрашивать фокус обратно в таблицу,
        // потому что на самом деле нам надо, чтобы он переходил на editorComponent.getNextFocusableComponent()
        // в обычных случаях - это и будет таблица, но при редактировании по хоткею - предыдущий компонент,
        // а в случае начала редактирование новой таблицы - эта новая таблица
        TableCellEditor editor = getCellEditor();
        if(editor != null) {
            editor.removeCellEditorListener(this);
            if (editorComp != null) {
                remove(editorComp);
            }

            Rectangle cellRect = getCellRect(editingRow, editingColumn, false);

            setCellEditor(null);
            setEditingColumn(-1);
            setEditingRow(-1);
            editorComp = null;

            repaint(cellRect);
        }
        super.removeEditor();

        if (nextComp != null) {
            nextComp.requestFocusInWindow();
        }
    }

    @Override
    public void removeEditor() {
        // removeEditor иногда вызывается напрямую, поэтому вызываем cancelCellEditing сами
        TableCellEditor cellEditor = getCellEditor();
        if (cellEditor != null) {
            cellEditor.cancelCellEditing();
        }
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {
        //copy/paste from JTable minus removeEditor...
        TableColumn resizingColumn = (tableHeader == null) ? null : tableHeader.getResizingColumn();
        if (resizingColumn != null && autoResizeMode == AUTO_RESIZE_OFF) {
            resizingColumn.setPreferredWidth(resizingColumn.getWidth());
        }
        resizeAndRepaint();
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        editPerformed = false;
        boolean consumed = e.isConsumed() || super.processKeyBinding(ks, e, condition, pressed);
        return consumed || editPerformed;
    }

    @Override
    protected void processKeyEvent(final KeyEvent e) {
        int row = getCurrentRow();
        int column = getSelectedColumn();
        if (row >= 0 && row < getRowCount() && column >= 0 && column < getColumnCount()) {
            ClientPropertyDraw property = getProperty(row, column);
            ClientGroupObjectValue columnKey = getColumnKey(row, column);

            String keyPressedActionSID = getPropertyKeyPressActionSID(e, property);
            if (keyPressedActionSID != null) {
                edit(property, columnKey, keyPressedActionSID, row, column, new InternalEditEvent(this, keyPressedActionSID));
            }
        }
        
        SwingUtils.getAroundTooltipListener(this, e, new Runnable() {
            @Override
            public void run() {
                ClientPropertyTable.super.processKeyEvent(e);
            }
        });
    }

    private MouseListener listener;

    private boolean isSubstituteListener(MouseListener listener) {
        return listener != null && "javax.swing.plaf.basic.BasicTableUI$Handler".equals(listener.getClass().getName()) ||
                "com.apple.laf.AquaTableUI$MouseInputHandler".equals(listener.getClass().getName());
    }

    @Override
    public synchronized void addMouseListener(MouseListener listener) {
        //подменяем стандартный MouseListener
        if (isSubstituteListener(listener)) {
            if(this.listener == null)
                this.listener = new ClientPropertyTableUIHandler(this);
            listener = this.listener;
        }
        super.addMouseListener(listener);
    }

    @Override
    public synchronized void removeMouseListener(MouseListener listener) {
        //подменяем стандартный MouseListener
        if (isSubstituteListener(listener)) {
            listener = this.listener;
        }
        super.removeMouseListener(listener);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        java.awt.Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);

        if (rowIndex != -1 && colIndex != -1) {
            ClientPropertyDraw cellProperty = getProperty(rowIndex, colIndex);
            
            // todo: временно отключил тултипы для richText'а для Java старше 8. часто вылетает (особенно при вставке из Word). следует убрать проверку после перехода на Java 8:
            // https://bugs.openjdk.java.net/browse/JDK-8034955
            Double javaVersion = SystemUtils.getJavaSpecificationVersion();
            if ((javaVersion == null || javaVersion < 1.8) && cellProperty.baseType instanceof ClientTextClass && ((ClientTextClass) cellProperty.baseType).rich) {
                return null;
            }
            
            if (cellProperty.baseType instanceof ClientActionClass) {
                return null;
            }
            
            if (!cellProperty.echoSymbols) {
                Object value = getValueAt(rowIndex, colIndex);
                if (value != null) {
                    if (value instanceof Double) {
                        value = (double) Math.round(((Double) value) * 1000) / 1000;
                    }

                    String formattedValue;
                    try {
                        formattedValue = cellProperty.formatString(value);
                    } catch (ParseException e1) {
                        formattedValue = String.valueOf(value);
                    }

                    if (!BaseUtils.isRedundantString(formattedValue)) {
                        return SwingUtils.toMultilineHtml(formattedValue, createToolTip().getFont());
                    }
                } else if (cellProperty.isEditableNotNull()) {
                    return PropertyRenderer.REQUIRED_STRING;
                }
            }
        }
        return null;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        //JViewport по умолчанию использует тупо константу - переопределяем это поведение
        return getPreferredSize();
    }

    protected void quickLog(String msg) {
//        if (form.isDialog()) {
//            return;
//        }
//        System.out.println("-------------------------------------------------");
//        System.out.println(this + ": ");
//        System.out.println("    " + msg);
//        ExceptionUtils.dumpStack();
    }

    public EditPropertyDispatcher getEditPropertyDispatcher() {
        return editDispatcher;
    }
}
