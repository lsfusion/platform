package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.DialogBox;
import lsfusion.gwt.client.base.GwtClientUtils;

public class DialogModalBox extends DialogBox {

    public DialogModalBox() {
        super(false, true);
    }

    private Element focusedElement;

    @Override
    public void show() {
        focusedElement = GwtClientUtils.getFocusedElement();

        super.show();
    }

    @Override
    public void hide() {
        super.hide();

        if(focusedElement != null)
            focusedElement.focus();
    }
}
