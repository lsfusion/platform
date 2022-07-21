package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.view.MainFrame;

public class HTMLTextCellEditor extends RequestReplaceValueCellEditor {
    private final String colorThemeName;

    public HTMLTextCellEditor(EditManager editManager) {
        super(editManager);
        this.colorThemeName = MainFrame.colorTheme.name();
    }

    protected native String getEditorValue(Element element)/*-{
        var aceEditor = element.aceEditor;
        return aceEditor != null ? aceEditor.getValue() : '';
    }-*/;

    @Override
    public boolean checkEnterEvent(Event event) {
        return event.getShiftKey();
    }

    @Override
    public void start(EventHandler handler, Element parent, Object oldValue) {
        initAceEditor(parent, oldValue, colorThemeName);
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        stop(parent);
    }

    @Override
    public Object getValue(Element parent, Integer contextAction) {
        return getEditorValue(parent);
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext, Pair<Integer, Integer> renderedSize, Object oldValue) {

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
