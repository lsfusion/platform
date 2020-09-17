package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.DialogBox;
import lsfusion.gwt.client.base.GwtClientUtils;

public class DialogWindowBox extends DialogBox {

    public DialogWindowBox(Caption captionWidget) {
        super(false, false, captionWidget);
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