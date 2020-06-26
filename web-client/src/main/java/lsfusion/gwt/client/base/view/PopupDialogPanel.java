package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.PopupPanel;
import lsfusion.gwt.client.base.GwtClientUtils;

// autohide or modal
public class PopupDialogPanel extends PopupPanel {

    public PopupDialogPanel() {
        super(true, false);
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
