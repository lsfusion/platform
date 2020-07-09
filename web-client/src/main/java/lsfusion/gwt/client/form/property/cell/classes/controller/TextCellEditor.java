package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.*;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.view.TextCellRenderer;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.form.property.cell.view.UpdateContext;

import static lsfusion.gwt.client.view.StyleDefaults.*;

public class TextCellEditor extends TextBasedCellEditor {
    public TextCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property, "textarea");
    }

    @Override
    public void renderDom(Element cellParent, RenderContext renderContext, UpdateContext updateContext) {
        DivElement div = Document.get().createDivElement();
        div.getStyle().setPaddingRight(8, Style.Unit.PX);
        div.getStyle().setPaddingLeft(0, Style.Unit.PX);
        div.getStyle().setHeight(100, Style.Unit.PCT);

        TextAreaElement textArea = div.appendChild(Document.get().createTextAreaElement());
        textArea.setTabIndex(-1);
        
        textArea.addClassName("textBasedGridCellEditor");

        Style textareaStyle = textArea.getStyle();
//        textareaStyle.setBorderWidth(0, Style.Unit.PX);
        textareaStyle.setWidth(100, Style.Unit.PCT);
        textareaStyle.setHeight(100, Style.Unit.PCT);
//        textareaStyle.setProperty("resize", "none");
//        textareaStyle.setOutlineStyle(Style.OutlineStyle.NONE);

        TextCellRenderer.setPadding(textareaStyle, true);
        cellParent.getStyle().setPadding(0, Style.Unit.PX);
        textareaStyle.setProperty("wordWrap", "break-word");
        setBaseTextFonts(textareaStyle, updateContext);

        cellParent.appendChild(div);
    }

    @Override
    protected String tryParseInputText(String inputText, boolean onCommit) {
        return (inputText == null || inputText.isEmpty()) ? null : inputText;
    }

    @Override
    protected boolean checkEnterEvent(Event event) {
        return event.getShiftKey();
    }

    protected void arrowPressed(NativeEvent event, Element parent, boolean down) {
        //NOP
    }

    @Override
    protected InputElement getInputElement(Element parent) {
        return parent.getFirstChild().getFirstChild().cast();
    }
}
