package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.DialogBox;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.view.MainFrame;

// twin of a PopupDialogPanel
public class DialogModalBox extends DialogBox {

    public DialogModalBox() {
        super(false, true);
    }

    private Element focusedElement;

    @Override
    public void show() {
        focusedElement = GwtClientUtils.getFocusedElement();
        MainFrame.setModalPopup(true);

        super.show();
    }

    @Override
    public void hide() {
        super.hide();

        MainFrame.setModalPopup(false);
        if(focusedElement != null)
            focusedElement.focus();
    }
}
