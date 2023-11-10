package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.jsni.Function;
import lsfusion.gwt.client.base.view.EventHandler;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.property.cell.controller.CommitReason;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import static lsfusion.gwt.client.base.GwtSharedUtils.nullEmpty;

public class ColorCellEditor extends PopupValueCellEditor {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    public ColorCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    @Override
    protected Widget createPopupComponent(Element parent) {
        throw new IllegalStateException("shouldn't be called: start method of ColorCellEditor doesn't call super");
    }

    @Override
    public void enterPressed(Element parent) {
        super.enterPressed(parent);
        validateAndCommit(parent, false, CommitReason.ENTERPRESSED);
    }

    @Override
    public PValue getCommitValue(Element parent, Integer contextAction) {
        String background = nullEmpty(parent.getPropertyString("hexValue"));
        return background != null ? PValue.getPValue(new ColorDTO(background)) : null;
    }

    public void reset(Element parent) {
        commitValue(parent, (PValue) null);
    }

    private void finishEditing(Element parent) {
        commitValue(parent, getCommitValue(parent, null));
    }

    Function eventListener = null;
    @Override
    public void start(EventHandler handler, Element parent, PValue oldValue) {
        if (oldValue != null) {
            try {
                String defaultColor = PValue.getColorValue(oldValue).value;
                if(defaultColor != null) {
                    parent.setAttribute("value", "#" + defaultColor);
                }
            } catch (Exception e) {
                throw new IllegalStateException("can't convert string value to color");
            }
        }

        if(eventListener == null) {
            initColoris(parent, messages.ok(), messages.reset());
            eventListener = addEventListener(parent);
        }
    }

    protected native void initColoris(Element input, String okText, String resetText)/*-{
        $wnd.Coloris({
            closeButton: true,
            closeLabel: okText,
            clearButton: true,
            clearLabel: resetText,
            alpha: false
        });
    }-*/;

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        removeEventListener(parent, eventListener);
        eventListener = null;
    }

    protected native Function addEventListener(Element input)/*-{
        var instance = this;
        fn = function(event) {
            var color = event.currentTarget.value;
            event.target.style.backgroundColor =  color ? color : null;
            event.target.hexValue = color ? color.substring(1) : null;
            instance.@ColorCellEditor::finishEditing(*)(input);
        };

        input.addEventListener('close', fn);
        return fn;
    }-*/;

    protected native void removeEventListener(Element input, Function fn)/*-{
        input.removeEventListener('close', fn);
    }-*/;

}
