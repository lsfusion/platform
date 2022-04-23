package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.GwtSharedUtils;
import lsfusion.gwt.client.classes.GType;
import lsfusion.gwt.client.classes.data.GFormatType;
import lsfusion.gwt.client.classes.data.GTimeType;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.GTimeDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import java.text.ParseException;

public class TimeCellEditor extends TextBasedCellEditor implements FormatCellEditor {
    private static final GTimeDTO midday = new GTimeDTO(12, 0, 0);

    private final GTimeType type;

    @Override
    public GFormatType getFormatType() {
        return type;
    }

    public TimeCellEditor(GTimeType type, EditManager editManager, GPropertyDraw property) {
        super(editManager, property);

        this.type = type;
    }

    @Override
    protected boolean isStringValid(String string) {
        //чтобы можно было вводить промежуточные некорректные значения
        return true;
    }
}
