package lsfusion.gwt.shared.view.classes;

import lsfusion.gwt.client.form.MainFrameMessages;
import lsfusion.gwt.client.grid.EditManager;
import lsfusion.gwt.client.grid.editor.GridCellEditor;
import lsfusion.gwt.client.grid.editor.LongGridCellEditor;
import lsfusion.gwt.shared.view.GPropertyDraw;

import java.text.ParseException;

public class GLongType extends GIntegralType {
    public static GLongType instance = new GLongType();

    @Override
    public GridCellEditor createGridCellEditor(EditManager editManager, GPropertyDraw editProperty) {
        return new LongGridCellEditor(editManager, editProperty);
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
