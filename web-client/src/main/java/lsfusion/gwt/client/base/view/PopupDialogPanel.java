package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.user.client.ui.PopupPanel;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.view.MainFrame;

// twin of a WindowBox
// autohide or modal
public class PopupDialogPanel extends PopupPanel {

    public PopupDialogPanel() {
        super(true, false);

        addCloseHandler(event -> onClose());
    }

    private Element focusedElement;

    @Override
    public void show() {
        onShow();

        super.show();
    }

    public void onShow() {
        focusedElement = GwtClientUtils.getFocusedElement();
        MainFrame.setModalPopup(true);
    }

    public void onClose() {
        MainFrame.setModalPopup(false);
        if(focusedElement != null)
            focusedElement.focus();
    }

    public boolean tooltipFocused = false;

    @Override
    protected void onAttach() {
        addDomHandler(ev -> tooltipFocused = true, MouseOverEvent.getType());

        addDomHandler(ev -> {
            tooltipFocused = false;
            hide();
        }, MouseOutEvent.getType());

        super.onAttach();
    }
}
