package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.ImageHtmlOrTextType;
import lsfusion.gwt.client.view.MainFrame;

// twin of a PopupDialogPanel
public class DialogModalWindow extends ModalWindow {

    public DialogModalWindow(String caption, boolean resizable, ModalWindowSize size) {
        super(resizable, size);

        BaseImage.initImageText(getTitleWidget(), caption, null, ImageHtmlOrTextType.FORM);
    }

    private Element focusedElement;

    @Override
    public void show(Integer insertIndex, Widget popupOwnerWidget) {
        MainFrame.closeNavigatorMenu();

        focusedElement = FocusUtils.getFocusedElement();
        MainFrame.setModalPopup(true);

        super.show(insertIndex, popupOwnerWidget);
    }

    @Override
    public void hide() {
        super.hide();

        MainFrame.setModalPopup(false);
        if(focusedElement != null) {
            FocusUtils.focus(focusedElement, FocusUtils.Reason.RESTOREFOCUS);
            focusedElement = null; // just in case because sometimes hide is called without show (and the same DialogModalBox is used several time)
        }
    }
}
