package lsfusion.gwt.client.form.property.cell.classes.controller.rich;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestEmbeddedCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;

public class RichTextCellEditor implements RequestEmbeddedCellEditor {

    private final EditManager editManager;

    public RichTextCellEditor(EditManager editManager) {
        this.editManager = editManager;
    }

    @Override
    public void start(Event event, Element parent, Object oldValue) {
        start(parent);
    }

    protected native void start(Element element)/*-{
        this.@RichTextCellEditor::enableEditing(*)(element, true);
        element.quill.focus();
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
        editManager.commitEditing(new GUserInputResult(getEditorValue(parent)), CommitReason.BLURRED);
    }

    @Override
    public void cancel(Element parent) {
        editManager.cancelEditing();
    }

    @Override
    public boolean checkEnterEvent(Event event) {
        return false;
    }
}
