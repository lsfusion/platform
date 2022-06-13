package lsfusion.client.form.filter.user.view;

import lsfusion.client.base.SwingUtils;
import lsfusion.client.classes.data.ClientRichTextClass;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.controller.ClientFormController;
import lsfusion.client.form.design.view.widget.TableWidget;
import lsfusion.client.form.object.ClientGroupObjectValue;
import lsfusion.client.form.object.table.controller.TableController;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.async.ClientInputList;
import lsfusion.client.form.property.async.ClientInputListAction;
import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.classes.controller.suggest.CompletionType;
import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;
import lsfusion.client.form.property.cell.view.PropertyRenderer;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.client.form.property.table.view.AsyncInputComponent;
import lsfusion.client.form.property.table.view.TableTransferHandler;
import lsfusion.interop.action.ServerResponse;
import lsfusion.interop.form.event.KeyStrokes;
import lsfusion.interop.form.property.Compare;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.text.ParseException;
import java.util.EventObject;
import java.util.List;

class DataFilterValueViewTable extends TableWidget implements TableTransferHandler.TableInterface, AsyncChangeInterface {
    private DataFilterValueView valueFilterView;
    private final Model model;
    private EventObject editEvent;
    private final TableController logicsSupplier;

    private ClientInputList inputList;

    private boolean applied;

    public DataFilterValueViewTable(DataFilterValueView valueFilterView, ClientPropertyDraw property, Compare compare, TableController ilogicsSupplier) {
        super(new Model());

        logicsSupplier = ilogicsSupplier;

        model = (Model) getModel();
        model.setProperty(property);
        
        changeInputList(compare);

        SwingUtils.setupClientTable(this);
        SwingUtils.setupSingleCellTable(this);
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getF3(), "none");

        //вырезаем Ввод, чтобы он обработался кнопкой apply
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getEnter(), "none");

        setDefaultRenderer(Object.class, new Renderer());
        setDefaultEditor(Object.class, new Editor());

        this.valueFilterView = valueFilterView;
    }

    @Override
    public boolean richTextSelected() {
        ClientPropertyDraw property = getProperty();
        return property.baseType instanceof ClientRichTextClass;
    }

    @Override
    public void pasteTable(List<List<String>> table) {
        if (!table.isEmpty()) {
            List<String> row = table.get(0);
            if (!row.isEmpty()) {
                try {
                    setValueAt(getProperty().parseBasePaste(row.get(0)), 0, 0);
                } catch (ParseException ignored) {
                }
            }
        }
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        if (!logicsSupplier.getFormController().commitCurrentEditing()) {
            return false;
        }

        if (e instanceof KeyEvent && !KeyStrokes.isSuitableEditKeyEventForRegularFilter(e)) {
            return false;
        }

        editEvent = e;

        boolean result = super.editCellAt(row, column, e);
        if (result) {
            final Component editor = getEditorComponent();
            if (editor instanceof JTextComponent) {
                JTextComponent textEditor = (JTextComponent) editor;
                textEditor.selectAll();
            }
            if (editor != null) {
                editor.requestFocusInWindow();
                logicsSupplier.getFormController().setCurrentEditingTable(this);
            }

            if (editorComp instanceof AsyncInputComponent) {
                ((AsyncInputComponent) editorComp).initEditor();
            }
        }

        return result;
    }

    @Override
    public void editingStopped(ChangeEvent e) {
        // inside editingStopped setValueAt is called before editor is being removed
        // setValueAt requests synchronous filter apply, which in applyFormChanges may call commitCurrentEditing()
        // and this will result in one more editingStopped() call with nested synchronous apply filter request
        // so clearCurrentEditingTable should go before editingStopped
        logicsSupplier.getFormController().clearCurrentEditingTable(this);
        super.editingStopped(e);
    }

    @Override
    public void editingCanceled(ChangeEvent e) {
        super.editingCanceled(e);
        logicsSupplier.getFormController().clearCurrentEditingTable(this);
        valueFilterView.editingCancelled();
    }

    @Override
    public void removeEditor() {
        super.removeEditor();
        logicsSupplier.getFormController().clearCurrentEditingTable(this);
        valueFilterView.editingCancelled();
    }

    @Override
    protected void processKeyEvent(final KeyEvent e) {
        SwingUtils.getAroundTooltipListener(this, e, new Runnable() {
            @Override
            public void run() {
                DataFilterValueViewTable.super.processKeyEvent(e);
            }
        });
    }

    /**
     * see {@link javax.swing.JTable#columnMarginChanged(javax.swing.event.ChangeEvent)}
     */
    @Override
    public void columnMarginChanged(ChangeEvent e) {
        // в JTable данный метод иногда вызывается при быстрой фильтрации при показе диалога,
        // при этом removeEditor - убивает редактирование в фильтре.
        // т.к. эта таблица - одиначная и без хедера - то просто спокойно убираем всё лишнеее
        resizeAndRepaint();
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        valueFilterView.valueChanged(value);
    }

    public Object getValue() {
        return model.getValue();
    }

    public void setValue(Object value) {
        model.setValue(value);
    }

    public ClientPropertyDraw getProperty() {
        return model.getProperty();
    }

    @Override
    public ClientPropertyDraw getProperty(int row, int column) {
        return row==0 && column==0 ? getProperty() : null;
    }

    public void setProperty(ClientPropertyDraw property) {
        model.setProperty(property);

        // cell height/width is calculated without row/column margins (getCellRect()). Row/column margin = intercell spacing.
        int valueHeight = property.getValueHeight(this) + getRowMargin();
        int valueWidth = property.getValueWidth(this) + getColumnModel().getColumnMargin();
        Dimension valueSize = new Dimension(valueWidth, valueHeight);
        setMinimumSize(valueSize);
        setPreferredSize(valueSize);

        setRowHeight(valueHeight);
    }

    @Override
    public EventObject getCurrentEditEvent() {
        return editEvent;
    }

    @Override
    public ClientInputList getCurrentInputList() {
        return inputList;
    }

    @Override
    public String getCurrentActionSID() {
        return ServerResponse.VALUES;
    }

    @Override
    public Integer getContextAction() {
        return null; //no need, no actions in filter
    }

    @Override
    public void setContextAction(Integer contextAction) {
        //no need, no actions in filter
    }

    @Override
    public ClientGroupObjectValue getColumnKey(int row, int col) {
        return valueFilterView.getColumnKey();
    }

    @Override
    public ClientFormController getForm() {
        return logicsSupplier.getFormController();
    }

    private final class Renderer extends JComponent implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            PropertyRenderer renderer = getProperty().getRendererComponent();
            renderer.updateRenderer(value, isSelected, hasFocus, false, DataFilterValueViewTable.this.hasFocus());
            renderer.getComponent().setBackground(applied ? SwingDefaults.getSelectionColor() : SwingDefaults.getTableCellBackground());
            return renderer.getComponent();
        }
    }

    private final class Editor extends AbstractCellEditor implements PropertyTableCellEditor {
        private PropertyEditor propertyEditor;
        public boolean enterPressed;

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            propertyEditor = getProperty().getValueEditorComponent(valueFilterView.getForm(), DataFilterValueViewTable.this, value);
            propertyEditor.setTableEditor(this);

            if (propertyEditor != null) {
                return propertyEditor.getComponent(null, null, editEvent);
            }

            return null;
        }

        @Override
        public JTable getTable() {
            return DataFilterValueViewTable.this;
        }

        @Override
        public void stopCellEditingLater() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    stopCellEditing();
                }
            });
        }

        @Override
        public void preCommit(boolean enterPressed) {
            this.enterPressed = enterPressed;
        }

        @Override
        public void postCommit() {
            enterPressed = false;
        }

        @Override
        public boolean stopCellEditing() {
            return propertyEditor.stopCellEditing() && super.stopCellEditing();
        }

        @Override
        public Object getCellEditorValue() {
            return propertyEditor.getCellEditorValue();
        }
    }
    
    public boolean editorEnterPressed() {
        TableCellEditor editor = getCellEditor();
        return editor != null && ((Editor) editor).enterPressed;
    }

    @Override
    public void updateUI() {
        super.updateUI();

        setBorder(SwingDefaults.getTextFieldBorder());
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
        repaint();
    }

    public void changeInputList(Compare compare) {
        inputList = new ClientInputList(new ClientInputListAction[0],
                compare == Compare.EQUALS || compare == Compare.NOT_EQUALS ? CompletionType.SEMI_STRICT : CompletionType.NON_STRICT, compare);
    }

    private static final class Model extends AbstractTableModel {
        private ClientPropertyDraw property;
        private Object value;

        public ClientPropertyDraw getProperty() {
            return property;
        }

        public void setProperty(ClientPropertyDraw property) {
            this.property = property;
        }

        public Object getValue() {
            return value;
        }

        public void setValue(Object value) {
            this.value = value;
            fireTableCellUpdated(0, 0);
        }

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return true;
        }

        @Override
        public Object getValueAt(int row, int column) {
            return value;
        }
    }
}
