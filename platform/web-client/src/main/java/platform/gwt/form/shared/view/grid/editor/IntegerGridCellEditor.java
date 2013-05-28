package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.Style;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.grid.EditManager;

public class IntegerGridCellEditor extends TextBasedGridCellEditor {
    public IntegerGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, Style.TextAlign.RIGHT);
    }

    @Override
    protected Object tryParseInputText(String inputText) throws ParseException {
        try {
            return property.parseString(inputText);
        } catch (Exception e) {
            throw new ParseException();
        }
    }
}
