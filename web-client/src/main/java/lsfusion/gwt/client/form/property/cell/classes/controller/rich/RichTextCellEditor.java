package lsfusion.gwt.client.form.property.cell.classes.controller.rich;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.event.GMouseStroke;
import lsfusion.gwt.client.form.property.cell.classes.controller.ARequestValueCellEditor;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

public class RichTextCellEditor extends ARequestValueCellEditor {

    public RichTextCellEditor(EditManager editManager) {
        super(editManager);
    }

    @Override
    public void onBrowserEvent(Element parent, EventHandler handler) {
        Event event = handler.event;
        if (GMouseStroke.isEvent(event))
            start(handler.event, parent, null);
        else if (event.getType().equals(BrowserEvents.BLUR) && !checkEventTarget(parent, event.getRelatedEventTarget()))
            commit(parent, CommitReason.BLURRED);

        handler.consume(true, false);
    }

    protected native boolean checkEventTarget(Element parent, EventTarget target)/*-{
        return parent.contains(target);
    }-*/;

    @Override
    public void start(Event event, Element parent, Object oldValue) {
        focusEditor(parent);
    }

    protected native void focusEditor(Element element)/*-{
        element.quill.focus();
    }-*/;

    @Override
    public Object getValue(Element parent, Integer contextAction) {
        return getEditorValue(parent);
    }

    protected native String getEditorValue(Element element)/*-{
        var quill = element.quill;
        return quill != null ? quill.root.innerHTML : '';
    }-*/;
}
