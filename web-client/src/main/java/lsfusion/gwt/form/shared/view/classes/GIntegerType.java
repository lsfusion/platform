package lsfusion.gwt.form.shared.view.classes;

import lsfusion.gwt.form.client.MainFrameMessages;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.client.grid.EditManager;
import lsfusion.gwt.form.client.grid.editor.AbstractGridCellEditor;
import lsfusion.gwt.form.client.grid.editor.GridCellEditor;

import java.text.ParseException;

public class GIntegerType extends GIntegralType {
    public static GIntegerType instance = new GIntegerType();

    @Override
    public GridCellEditor visit(GTypeVisitor visitor) {
        return (GridCellEditor) visitor.visit(this);
    }

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return AbstractGridCellEditor.createGridCellEditor(this, editManager, editProperty);
    }

    @Override
    public Integer parseString(String s, String pattern) throws ParseException {
        return parseToDouble(s, pattern).intValue();
    }

    @Override
    protected int getLength() {
        return 8;
    }

    @Override
    public String toString() {
        return MainFrameMessages.Instance.get().typeIntegerCaption();
    }
}
