package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.PopupPanel;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.view.MainFrame;

// twin of a WindowBox
// autohide or modal
public class PopupDialogPanel extends PopupPanel {

    public PopupDialogPanel() {
        this(true);
    }

    public PopupDialogPanel(boolean autoHide) {
        super(autoHide, false);

        setStyleName("popup-dialog");
        addCloseHandler(event -> onClose());
    }

    private Element focusedElement;

    @Override
    public void show() {
        onShow();

        super.show();
    }

    public void setFocusedElement(Element focusedElement) {
        this.focusedElement = focusedElement;
    }

    public void onShow() {
        setFocusedElement(GwtClientUtils.getFocusedElement());
        MainFrame.setModalPopup(true);
    }

    public void onClose() {
        MainFrame.setModalPopup(false);
        if(focusedElement != null)
            focusedElement.focus();
    }

    public boolean tooltipFocused = false;
}
