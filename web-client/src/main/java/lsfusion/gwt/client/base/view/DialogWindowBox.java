package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.DialogBox;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.view.MainFrame;
import org.vectomatic.dom.svg.events.FocusInEvent;
import org.vectomatic.dom.svg.events.FocusOutEvent;

public class DialogWindowBox extends DialogBox {

    public DialogWindowBox(Caption captionWidget) {
        super(false, false, captionWidget);
        addDomHandler(event -> MainFrame.setModalPopup(true), FocusInEvent.getType());
        addDomHandler(event -> MainFrame.setModalPopup(false), FocusOutEvent.getType());
    }

    private Element focusedElement;

    @Override
    public void show() {
        focusedElement = GwtClientUtils.getFocusedElement();
        super.show();
    }

    public void close() {
        if(focusedElement != null) {
            focusedElement.focus();
            focusedElement = null;
        }
        super.hide();
    }

    public void restoreDialog() {
        super.show();
    }
}