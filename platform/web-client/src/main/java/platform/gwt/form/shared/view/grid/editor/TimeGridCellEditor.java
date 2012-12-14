package platform.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.cellview.client.cell.Cell;
import platform.gwt.form.shared.view.grid.EditEvent;
import platform.gwt.form.shared.view.grid.EditManager;

import java.sql.Time;

public class TimeGridCellEditor extends TextFieldGridCellEditor {
    private DateTimeFormat format = GwtSharedUtils.getDefaultTimeFormat();

    public TimeGridCellEditor(EditManager editManager) {
        super(editManager, Style.TextAlign.RIGHT);
    }

    @Override
    protected Object tryParseInputText(String inputText) throws ParseException {
        try {
            if (inputText.split(":").length == 2) {
                inputText += ":00";
            }
            return inputText.isEmpty() ? null : new Time(format.parse(inputText).getTime());
        } catch(IllegalArgumentException e) {
            throw new ParseException();
        }
    }

    @Override
    public void startEditing(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        super.startEditing(editEvent, context, parent, oldValue == null ? "12:00:00" : format.format((Time) oldValue));
    }
}
