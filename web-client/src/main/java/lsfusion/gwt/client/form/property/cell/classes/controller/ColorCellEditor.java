package lsfusion.gwt.client.form.property.cell.classes.controller;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.base.jsni.Function;
import lsfusion.gwt.client.form.property.GPropertyDraw;
import lsfusion.gwt.client.form.property.PValue;
import lsfusion.gwt.client.form.property.cell.classes.ColorDTO;
import lsfusion.gwt.client.form.property.cell.controller.EditManager;

import static lsfusion.gwt.client.base.GwtSharedUtils.nullEmpty;

public class ColorCellEditor extends TextBasedPopupCellEditor {
    private static final ClientMessages messages = ClientMessages.Instance.get();

    public ColorCellEditor(EditManager editManager, GPropertyDraw property) {
        super(editManager, property);
    }

    Function eventListener = null;
    @Override
    protected Widget createPopupComponent(Element parent, PValue oldValue) {

        if (oldValue != null) {
            try {
                String defaultColor = PValue.getColorValue(oldValue).value;
                if(defaultColor != null) {
                    editBox.setAttribute("value", "#" + defaultColor);
                }
            } catch (Exception e) {
                throw new IllegalStateException("can't convert string value to color");
            }
        }

        editBox.setAttribute("data-coloris", "true");
        editBox.setAttribute("autocomplete", "off");

        if(eventListener == null) {
            eventListener = startColoris(RootPanel.get().getElement(), editBox, messages.ok(), messages.cancel(), messages.reset());
        }

        popup.setVisible(false);

        editBox.click();

        return new SimplePanel();
    }

//    @Override
//    public void enterPressed(Element parent) {
//        super.enterPressed(parent);
//        validateAndCommit(parent, false, CommitReason.ENTERPRESSED);
//    }

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
        super.stop(parent, cancel, blurred);
    }

    protected native void stopColoris(Element root, Element input, Function fn)/*-{
        root.getElementsByClassName('clr-cancel')[0].remove();
        input.removeEventListener('close', fn);
    }-*/;

}
