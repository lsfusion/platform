package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.client.form.grid.EditManager;
import lsfusion.gwt.client.form.grid.editor.GridCellEditor;
import lsfusion.gwt.client.form.grid.editor.IntegerGridCellEditor;
import lsfusion.gwt.shared.view.GPropertyDraw;

import java.text.ParseException;

public class GIntegerType extends GIntegralType {
    public static GIntegerType instance = new GIntegerType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new IntegerGridCellEditor(editManager, editProperty);
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
