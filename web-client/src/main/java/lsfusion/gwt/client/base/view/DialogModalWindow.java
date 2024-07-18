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
        this(caption, resizable, size, null);
    }

    public DialogModalWindow(String caption, boolean resizable, ModalWindowSize size, String backgroundClass) {
        super(resizable, size);

        getBody().getElement().addClassName("dialog-modal-body");

        if(backgroundClass !=null)
            body.addStyleName(backgroundClass);

        BaseImage.initImageText(getTitleWidget(), caption, null, ImageHtmlOrTextType.FORM);
    }

    private Element focusedElement;

    @Override
    public void show(Integer insertIndex, PopupOwner popupOwner) {
        MainFrame.closeNavigatorMenu();

        focusedElement = FocusUtils.getFocusedElement();
        MainFrame.setModalPopup(true);

        super.show(insertIndex, popupOwner);
    }

    @Override
    public void hide() {
        // for ResizableModalWindow this case is proceeded in ModalForm (using prevForm)
        if(focusedElement != null) {
            FocusUtils.focus(focusedElement, FocusUtils.Reason.RESTOREFOCUS);
            focusedElement = null; // just in case because sometimes hide is called without show (and the same DialogModalBox is used several time)
        }

        super.hide();

        MainFrame.setModalPopup(false);
    }
}
