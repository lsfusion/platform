package lsfusion.gwt.form.shared.view.grid.editor;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import lsfusion.gwt.base.shared.GwtSharedUtils;
import lsfusion.gwt.cellview.client.cell.Cell;
import lsfusion.gwt.form.shared.view.GPropertyDraw;
import lsfusion.gwt.form.shared.view.changes.dto.GTimeDTO;
import lsfusion.gwt.form.shared.view.classes.GTimeType;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.grid.EditManager;

import java.text.ParseException;

public class TimeGridCellEditor extends TextBasedGridCellEditor {
    private static final GTimeDTO midday = new GTimeDTO(12, 0, 0);

    private static DateTimeFormat format = GwtSharedUtils.getDefaultTimeFormat();

    public TimeGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, Style.TextAlign.RIGHT);
    }

    @Override
    protected boolean isStringValid(String string) {
        //чтобы можно было вводить промежуточные некорректные значения
        return true;
    }

    @Override
    protected GTimeDTO tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        return GTimeType.instance.parseString(inputText, property.pattern);
    }

    @Override
    public void startEditing(EditEvent editEvent, Cell.Context context, Element parent, Object oldValue) {
        GTimeDTO time = oldValue == null ? midday : (GTimeDTO)oldValue;
        super.startEditing(editEvent, context, parent, format.format(time.toTime()));
    }
}
