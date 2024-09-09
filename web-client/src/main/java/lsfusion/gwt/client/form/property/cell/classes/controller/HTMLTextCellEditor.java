package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;
import lsfusion.gwt.client.form.property.cell.view.RenderContext;
import lsfusion.gwt.client.view.MainFrame;

public class HTMLTextCellEditor extends RequestReplaceValueCellEditor {
    private final String colorThemeName;
    private final boolean autoSizedY;

    public HTMLTextCellEditor(EditManager editManager, boolean autoSizedY) {
        super(editManager);
        this.autoSizedY = autoSizedY;
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
    public void start(EventHandler handler, Element parent, RenderContext renderContext, boolean notFocusable, PValue oldValue) {
        initAceEditor(parent, PValue.getStringValue(oldValue), colorThemeName, autoSizedY);
    }

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        stop(parent);
    }

    @Override
    public PValue getCommitValue(Element parent, Integer contextAction) {
        return PValue.getPValue(getEditorValue(parent));
    }

    @Override
    public void render(Element cellParent, RenderContext renderContext, PValue oldValue, Integer renderedWidth, Integer renderedHeight) {

    }

    protected native void stop(Element element)/*-{
        var aceEditor = element.aceEditor;
        if (aceEditor != null) {
            aceEditor.destroy();
            element.aceEditor = null;
        }
    }-*/;

    protected native void initAceEditor(Element element, String oldValue, String colorTheme, boolean autoSizedY)/*-{
        var aceEditor = $wnd.ace.edit(element, {
            enableLiveAutocompletion: true,
            mode: 'ace/mode/html',
            theme: colorTheme === 'LIGHT' ? 'ace/theme/chrome' : 'ace/theme/ambiance',
            showPrintMargin: false
        });
        if (autoSizedY) {
            aceEditor.setOptions({
                // https://stackoverflow.com/questions/11584061/automatically-adjust-height-to-contents-in-ace-cloud-9-editor
                maxLines: Infinity
            });
        }
        element.aceEditor = aceEditor;
        aceEditor.setValue(oldValue != null ? oldValue : '');
        aceEditor.focus();
    }-*/;
}
