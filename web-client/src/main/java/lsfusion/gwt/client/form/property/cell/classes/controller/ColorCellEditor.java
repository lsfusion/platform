package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
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
            eventListener = startColoris(RootPanel.get().getElement(), parent, messages.ok(), messages.cancel(), messages.reset());
        }
    }

    protected native Function startColoris(Element root, Element input, String okText, String cancelText, String resetText)/*-{
        $wnd.Coloris({
            closeButton: true,
            closeLabel: okText,
            clearButton: true,
            clearLabel: resetText,
            alpha: false
        });
        $wnd.Coloris.ready(function() {
            var picker = root.getElementsByClassName('clr-picker')[0];
            var button = document.createElement('button');

            picker.appendChild(button);
            button.setAttribute('type', 'button');
            button.classList.add('clr-cancel');
            button.textContent = cancelText;
            button.addEventListener('click', function() {
                $wnd.Coloris.close(true);
            });
    });

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

    @Override
    public void stop(Element parent, boolean cancel, boolean blurred) {
        stopColoris(RootPanel.get().getElement(), parent, eventListener);
        eventListener = null;
    }

    protected native void stopColoris(Element root, Element input, Function fn)/*-{
        root.getElementsByClassName('clr-cancel')[0].remove();
        input.removeEventListener('close', fn);
    }-*/;

}
