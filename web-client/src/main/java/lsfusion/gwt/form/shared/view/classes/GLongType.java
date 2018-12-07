package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.client.grid.EditManager;
import lsfusion.gwt.form.client.grid.editor.AbstractGridCellEditor;
import lsfusion.gwt.form.client.grid.editor.GridCellEditor;

import java.text.ParseException;

public class GLongType extends GIntegralType {
    public static GLongType instance = new GLongType();

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return AbstractGridCellEditor.createGridCellEditor(this, editManager, editProperty);
    }

    @Override
    public Long parseString(String s, String pattern) throws ParseException {
        return parseToDouble(s, pattern).longValue();
    }

    @Override
    protected int getLength() {
        return 10;
    }

    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeLongCaption();
    }
}
