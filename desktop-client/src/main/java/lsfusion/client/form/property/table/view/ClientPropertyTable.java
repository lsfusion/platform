package lsfusion.client.form.property.table.view;

import com.google.common.base.Preconditions;
import lsfusion.base.BaseUtils;
import lsfusion.base.Result;
import lsfusion.client.base.SwingUtils;
import lsfusion.client.classes.ClientActionClass;
import lsfusion.client.classes.ClientType;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.widget.TableWidget;
import lsfusion.client.form.object.ClientGroupObject;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.async.ClientInputList;
import lsfusion.client.form.property.cell.EditBindingMap;
import lsfusion.client.form.property.cell.controller.ClientAbstractCellEditor;
import lsfusion.client.form.property.cell.controller.dispatch.EditPropertyDispatcher;
import lsfusion.client.form.property.cell.view.ClientAbstractCellRenderer;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.panel.view.SingleCellTable;
import lsfusion.interop.action.ServerResponse;
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
import java.awt.event.*;
import java.text.ParseException;
import java.util.EventObject;

import static lsfusion.client.form.property.cell.EditBindingMap.*;

public abstract class ClientPropertyTable extends TableWidget implements TableTransferHandler.TableInterface, AsyncChangeCellTableInterface {
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
    protected ClientInputList currentInputList;
    protected String currentActionSID;
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

        enableEvents(AWTEvent.MOUSE_EVENT_MASK); // just in case, because we override processMouseEvent (however there are addMouseListeners)
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
            public boolean pressed(InputEvent ke) {
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

    public EventObject getCurrentEditEvent() {
        return editEvent;
    }

    public ClientInputList getCurrentInputList() {
        return currentInputList;
    }

    public String getCurrentActionSID() {
        return currentActionSID;
    }

    Integer contextAction = null;
    @Override
    public Integer getContextAction() {
        return contextAction;
    }
    @Override
    public void setContextAction(Integer contextAction) {
        this.contextAction = contextAction;
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

        Result<Integer> contextAction = new Result<>();
        String actionSID = getPropertyEventActionSID(e, contextAction, property, editBindingMap);

        if (actionSID == null) {
            return false;
        }

        if (isChangeEvent(actionSID) && !isCellEditable(row, column)) {
            return false;
        }
        if(ServerResponse.EDIT_OBJECT.equals(actionSID) && !property.hasEditObjectAction) {
            return false;
        }

        quickLog("formTable.editCellAt: " + e);

        //здесь немного запутанная схема...
        //executePropertyEventAction возвращает true, если редактирование произошло на сервере, необязательно с вводом значения...
        //но из этого editCellAt мы должны вернуть true, только если началось редактирование значения
        editPerformed = edit(property, columnKey, actionSID, row, column, e, contextAction.result);
        return editorComp != null;
    }

    public boolean edit(ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID, int row, int column, EventObject e, Integer contextAction) {
        editRow = row;
        editCol = column;
        commitingValue = false;
        editEvent = e;

        return editDispatcher.executePropertyEventAction(property, columnKey, actionSID, editEvent, contextAction);
    }

    public abstract int getCurrentRow();

    // edit cell can be out of bounds because of grid change
    private boolean checkEditBounds() {
        return editRow < getRowCount() && editCol < getColumnCount();
    }

    public boolean requestValue(ClientType valueType, Object oldValue, ClientInputList inputList, String actionSID) {
        quickLog("formTable.requestValue: " + valueType);

        //пока чтение значения можно вызывать только один раз в одном изменении...
        //если получится безусловно задержать фокус, то это ограничение можно будет убрать
        Preconditions.checkState(!commitingValue, "You can request value only once per edit action.");

        // need this because we use getTableCellEditorComponent infrastructure and we need to pass currentEditValue there somehow
        currentEditType = valueType;
        currentEditValue = oldValue;
        currentInputList = inputList;
        currentActionSID = actionSID;

        if (!super.editCellAt(editRow, editCol, editEvent)) {
            return false;
        }

        // is checked in upper call
        assert checkEditBounds();
        prepareTextEditor();

        if (editorComp instanceof AsyncInputComponent) {
            ((AsyncInputComponent) editorComp).initEditor(!KeyStrokes.isChangeAppendKeyEvent(editEvent));
        }

        if(editorComp != null) {
            editorComp.requestFocusInWindow();
        }

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
        editDispatcher.commitValue(value, contextAction);
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

    @Override
    protected void processMouseEvent(MouseEvent e) {
        checkMouseEvent(e, true);

        super.processMouseEvent(e);

        checkMouseEvent(e, false);
    }

    protected abstract ClientPropertyDraw getSelectedProperty();

    private void checkMouseEvent(MouseEvent e, boolean preview) {
        form.checkMouseEvent(e, preview, getSelectedProperty(), () -> groupObject, this instanceof SingleCellTable);
    }

    private void checkKeyEvent(KeyStroke ks, boolean preview, KeyEvent e, int condition, boolean pressed) {
        form.checkKeyEvent(ks, e, preview, getSelectedProperty(), () -> groupObject, this instanceof SingleCellTable, condition, pressed);
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        checkKeyEvent(ks, true, e, condition, pressed);

        editPerformed = false;
        boolean consumed = e.isConsumed() || super.processKeyBinding(ks, e, condition, pressed) || editPerformed;

        checkKeyEvent(ks, false, e, condition, pressed);

        return consumed || e.isConsumed();
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
                edit(property, columnKey, keyPressedActionSID, row, column, new InternalEditEvent(this, keyPressedActionSID), null);
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

    private static boolean isSubstituteListener(MouseListener listener) {
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
                    } catch (ParseException | IllegalArgumentException e1) {
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
