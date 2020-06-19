package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.classes.data.GTimeType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GTimeDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;

public class TimeGridCellEditor extends TextBasedGridCellEditor {
    private static final GTimeDTO midday = new GTimeDTO(12, 0, 0);

    private static DateTimeFormat format = GwtSharedUtils.getDefaultTimeFormat();

    public TimeGridCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
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
    public void startEditing(Event event, Element parent, Object oldValue) {
        GTimeDTO time = oldValue == null ? midday : (GTimeDTO)oldValue;
        super.startEditing(event, parent, format.format(time.toTime()));
    }
}
