package lsfusion.gwt.client.form.property.cell.classes.controller.rich;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestEmbeddedCellEditor;
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
        this.oldValue = value;

        String startEventValue = checkStartEvent(event, parent, null);
        boolean selectAll = startEventValue == null;
        value = startEventValue != null ? startEventValue : value;

        start(parent, value, selectAll);
    }

    protected native void start(Element element, String value, boolean selectAll)/*-{
        this.@RichTextCellEditor::enableEditing(*)(element, true);
        var quill = element.quill;
        quill.focus();

        if (selectAll) {
            if (value === "")
                quill.deleteText(0, quill.getLength());

            quill.setSelection(0, this.@RichTextCellEditor::getEditorValue(*)(element).length);
        } else {
            this.@RichTextCellEditor::setEditorValue(*)(element, value);
            setTimeout(function setSelection() {
                quill.setSelection(quill.getLength(), 0); //set the cursor to the end
            }, 0);
        }
    }-*/;

    protected native void setEditorValue(Element element, String value)/*-{
        var quill = element.quill;
        quill.deleteText(0, quill.getLength());
        quill.root.innerHTML = value;
    }-*/;

    protected native String getEditorValue(Element element)/*-{
        var quill = element.quill;
        return quill != null ? quill.root.innerHTML : '';
    }-*/;

    protected native void enableEditing(Element element, boolean enableEditing)/*-{
        element.quill.enable(enableEditing);
    }-*/;

    @Override
    public void stop(Element parent, boolean cancel) {
        enableEditing(parent, false);
    }

    @Override
    public void commit(Element parent, CommitReason commitReason) {
        editManager.commitEditing(new GUserInputResult(getEditorValue(parent)), commitReason);
        parent.focus(); //return focus to the parent
    }

    @Override
    public void cancel(Element parent, CancelReason cancelReason) {
        setEditorValue(parent, oldValue); //to return the previous value after pressing esc
        editManager.cancelEditing(cancelReason);
        parent.focus(); //return focus to the parent
    }

    @Override
    public boolean checkEnterEvent(Event event) {
        return event.getShiftKey();
    }
}
