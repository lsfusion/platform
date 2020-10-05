package lsfusion.client.form.property.cell.classes.controller.rich;

import lsfusion.client.form.property.cell.classes.controller.PropertyEditor;
import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;
import lsfusion.client.form.property.table.view.ClientPropertyTableEditorComponent;
import lsfusion.interop.form.design.ComponentDesign;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.EventObject;


@SuppressWarnings({"FieldCanBeLocal"})
public class RichTextPropertyEditor extends JPanel implements PropertyEditor, ClientPropertyTableEditorComponent {
    private final RichEditorPane richArea;

    private PropertyTableCellEditor tableEditor;

    public RichTextPropertyEditor(Object value, ComponentDesign design) {
        this(null, value, design);
    }

    public RichTextPropertyEditor(Component owner, Object value, ComponentDesign design) {
        super(new BorderLayout());
        richArea = new RichEditorPane();
        richArea.setText(value == null ? "" : value.toString());
        
        //не показываем тулбар сразу, чтобы он не ловил событие начала редактирования
        richArea.setToolbarVisible(false);
        add(richArea);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                richArea.setToolbarVisible(true);
                revalidate();
                repaint();
            }
        });
        
        getActionMap().put("commitEditAction", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tableEditor.stopCellEditing();
            }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStrokes.getCtrlEnter(), "commitEditAction");
    }

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        this.tableEditor = tableEditor;
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return this;
    }

    public Object getCellEditorValue() {
        String text = richArea.getText();
        return text.isEmpty() ? null : text;
    }

    @Override
    public boolean requestFocusInWindow() {
        return richArea.requestFocusInWindow();
    }

   @Override
    public boolean stopCellEditing() {
        return true;
    }

    @Override
    public void cancelCellEditing() { }

    public void prepareTextEditor(boolean clear, boolean select) {
        if (clear) {
            richArea.setText("");
        } else if (select) {
            richArea.getWysEditor().selectAll();
        }
    }
}
