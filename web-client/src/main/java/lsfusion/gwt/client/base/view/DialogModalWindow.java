package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.DialogBox;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.view.MainFrame;

// twin of a PopupDialogPanel
public class DialogModalWindow extends ModalWindow {

    public DialogModalWindow() {
        super();
    }

    private Element focusedElement;

    @Override
    public void show(Integer insertIndex) {
        MainFrame.closeNavigatorMenu();

        focusedElement = GwtClientUtils.getFocusedElement();
        MainFrame.setModalPopup(true);

        super.show(insertIndex);
    }

    @Override
    public void hide() {
        super.hide();

        MainFrame.setModalPopup(false);
        if(focusedElement != null) {
            focusedElement.focus();
            focusedElement = null; // just in case because sometimes hide is called without show (and the same DialogModalBox is used several time)
        }
    }
}
