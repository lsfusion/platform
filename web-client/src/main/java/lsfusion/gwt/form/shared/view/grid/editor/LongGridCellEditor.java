package lsfusion.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.Style;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.grid.EditManager;

public class LongGridCellEditor extends TextBasedGridCellEditor {
    public LongGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, Style.TextAlign.RIGHT);
    }

    @Override
    protected Object tryParseInputText(String inputText) throws TextBasedGridCellEditor.ParseException {
        try {
            return property.parseString(inputText);
        } catch (Exception e) {
            throw new TextBasedGridCellEditor.ParseException();
        }
    }
}
