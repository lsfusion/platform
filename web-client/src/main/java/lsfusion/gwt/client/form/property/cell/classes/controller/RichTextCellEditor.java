package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.controller.KeepCellEditor;

public class RichTextCellEditor extends ARequestValueCellEditor implements RequestEmbeddedCellEditor, KeepCellEditor {

    private String oldValue;

    public RichTextCellEditor(EditManager editManager) {
        super(editManager);
    }

    @Override
    public void start(EventHandler handler, Element parent, ResizableComplexPanel attachContainer, PValue oldValue) {
        this.oldValue = getEditorValue(parent);

        String value = handler != null ? checkStartEvent(handler.event, parent, null) : null;
        boolean selectAll = value == null;

        if(value == null) {
            value = PValue.getStringValue(oldValue);
            if (value != null)
                value = value.replaceAll("<div", "<p").replaceAll("</div>", "</p>");
        }

        enableEditing(parent, false);

        start(parent, value, selectAll);

        parent.addClassName("property-hide-toolbar");
    }

    protected native void start(Element element, String value, boolean selectAll)/*-{
        this.@RichTextCellEditor::enableEditing(*)(element, true);

        var quill = element.quill;
        quill.focus();

        this.@RichTextCellEditor::setEditorValue(*)(element, value);
        if (selectAll === true) {
            if (value === "")
                quill.deleteText(0, quill.getLength());

            if (value != null)
                this.@RichTextCellEditor::selectContent(*)(quill, 0, value.length);
        } else {
            this.@RichTextCellEditor::selectContent(*)(quill, quill.getLength(), 0); //set the cursor to the end
        }
    }-*/;

    protected native void selectContent(Element quill, int from, int to)/*-{
        setTimeout(function setSelection() {
            quill.setSelection(from, to);
        }, 0);
    }-*/;


    protected native void setEditorValue(Element element, String value)/*-{
        if (this.@RichTextCellEditor::getEditorValue(*)(element) !== value) {
            var quill = element.quill;
            quill.deleteText(0, quill.getLength());
            quill.root.innerHTML = value;
        }
    }-*/;

    protected native String getEditorValue(Element element)/*-{
        var quill = element.quill;
        return quill != null ? quill.root.innerHTML : '';
    }-*/;

    protected native void enableEditing(Element element, boolean enableEditing)/*-{
        element.quill.enable(enableEditing);
    }-*/;

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        parent.removeClassName("property-hide-toolbar");

        enableEditing(parent, false);

        if (!blurred)
            FocusUtils.focus(parent, FocusUtils.Reason.OTHER); //return focus to the parent

        if (cancel)
            setEditorValue(parent, oldValue); //to return the previous value after pressing esc
    }

    @Override
    public PValue getCommitValue(Element parent, Integer contextAction) {
        return PValue.getPValue(getEditorValue(parent));
    }

    @Override
    public boolean checkEnterEvent(Event event) {
        return event.getShiftKey();
    }
}
