package lsfusion.gwt.client.form.property.cell.classes.controller.rich;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.cell.classes.controller.RequestEmbeddedCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;

public class RichTextCellEditor implements RequestEmbeddedCellEditor {

    private final EditManager editManager;
    private final GPropertyDraw property;

    public RichTextCellEditor(EditManager editManager, GPropertyDraw property) {
        this.editManager = editManager;
        this.property = property;
    }

    @Override
    public void start(Event event, Element parent, Object oldValue) {
        start(parent);
    }

    protected native void start(Element element)/*-{
        var quill = element.quill;
        quill.enable(!this.@RichTextCellEditor::property.@GPropertyDraw::isReadOnly()());
        quill.focus();
    }-*/;

    protected native String getEditorValue(Element element)/*-{
        var quill = element.quill;
        return quill != null ? quill.root.innerHTML : '';
    }-*/;

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
        return event.getShiftKey();
    }
}
