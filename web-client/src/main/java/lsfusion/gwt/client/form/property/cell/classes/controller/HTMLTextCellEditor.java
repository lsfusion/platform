package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;
import lsfusion.gwt.client.view.MainFrame;

public class HTMLTextCellEditor implements RequestEmbeddedCellEditor {
    private final EditManager editManager;
    private final String colorThemeName;

    public HTMLTextCellEditor(EditManager editManager) {
        this.editManager = editManager;
        this.colorThemeName = MainFrame.colorTheme.name();
    }

    protected native String getEditorValue(Element element)/*-{
        var aceEditor = element.aceEditor;
        return aceEditor != null ? aceEditor.getValue() : '';
    }-*/;

    @Override
    public void commit(Element parent, CommitReason commitReason) {
        editManager.commitEditing(new GUserInputResult(getEditorValue(parent)), CommitReason.BLURRED);
    }

    @Override
    public void cancel(Element parent, CancelReason cancelReason) {
        editManager.cancelEditing(cancelReason);
    }

    @Override
    public boolean checkEnterEvent(Event event) {
        return false;
    }

    @Override
    public void start(Event editEvent, Element parent, Object oldValue) {
        initAceEditor(parent, oldValue, colorThemeName);
    }

    @Override
    public void stop(Element parent, boolean cancel) {
        stop(parent);
    }

    protected native void stop(Element element)/*-{
        var aceEditor = element.aceEditor;
        if (aceEditor != null) {
            aceEditor.destroy();
            element.aceEditor = null;
        }
    }-*/;

    protected native void initAceEditor(Element element, Object oldValue, String colorTheme)/*-{
        var aceEditor = $wnd.ace.edit(element, {
            enableLiveAutocompletion: true,
            mode: 'ace/mode/html',
            theme: colorTheme === 'LIGHT' ? 'ace/theme/chrome' : 'ace/theme/ambiance',
            showPrintMargin: false
        });
        element.aceEditor = aceEditor;
        aceEditor.setValue(oldValue != null ? oldValue : '');
        aceEditor.focus();
    }-*/;
}
