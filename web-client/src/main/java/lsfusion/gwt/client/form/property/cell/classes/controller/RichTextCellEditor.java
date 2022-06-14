package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;

public class RichTextCellEditor implements RequestEmbeddedCellEditor {

    private final EditManager editManager;
    private String oldValue;

    public RichTextCellEditor(EditManager editManager) {
        this.editManager = editManager;
    }

    @Override
    public void start(Event event, Element parent, Object oldValue) {
        String value = oldValue == null ? "" : oldValue.toString();
        this.oldValue = getEditorValue(parent);

        String startEventValue = checkStartEvent(event, parent, null);
        boolean selectAll = startEventValue == null;
        value = startEventValue != null ? startEventValue : value;

        start(parent, value, selectAll);
    }

    protected native void start(Element element, String value, boolean selectAll)/*-{
        this.@RichTextCellEditor::enableEditing(*)(element, true);
        var quill = element.quill;
        quill.focus();

        this.@RichTextCellEditor::setEditorValue(*)(element, value);
        if (selectAll === true) {
            if (value === "")
                quill.deleteText(0, quill.getLength());

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
        enableEditing(parent, false);

        if (!blurred)
            parent.focus(); //return focus to the parent

        if (cancel)
            setEditorValue(parent, oldValue); //to return the previous value after pressing esc
    }

    @Override
    public void commit(Element parent, CommitReason commitReason) {
        editManager.commitEditing(new GUserInputResult(getEditorValue(parent)), commitReason);
    }

    @Override
    public void cancel(Element parent, CancelReason cancelReason) {
        editManager.cancelEditing(cancelReason);
    }

    @Override
    public boolean checkEnterEvent(Event event) {
        return event.getShiftKey();
    }
}
