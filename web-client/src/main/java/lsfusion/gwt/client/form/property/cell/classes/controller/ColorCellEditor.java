package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;

import java.text.ParseException;

public class ColorCellEditor extends TextBasedCellEditor {

    public ColorCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected String tryFormatInputText(PValue value) {
        return value == null ? "" : PValue.getColorStringValue(value);
    }

    @Override
    public void start(EventHandler handler, Element parent, RenderContext renderContext, boolean notFocusable, PValue oldValue) {
        super.start(handler, parent, renderContext, notFocusable, oldValue);

        if(started)
            inputElement.click();
    }

    @Override
    protected PValue tryParseInputText(String inputText, boolean onCommit) throws ParseException {
        return inputText != null ? PValue.getPValue(new ColorDTO(inputText.substring(1))) : null;
    }
}
