package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.RootPanel;
import lsfusion.gwt.client.base.BaseImage;
import lsfusion.gwt.client.base.FocusUtils;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.ImageHtmlOrTextType;
import lsfusion.gwt.client.form.event.GKeyStroke;
import lsfusion.gwt.client.view.MainFrame;

// twin of a PopupDialogPanel
public class DialogModalWindow extends ModalWindow {

    private static HandlerRegistration previewRegistration;

    public DialogModalWindow(String caption, boolean resizable, ModalWindowSize size) {
        this(caption, resizable, size, null);
    }

    public DialogModalWindow(String caption, boolean resizable, ModalWindowSize size, String backgroundClass) {
        super(resizable, true, size);

        GwtClientUtils.addClassName(getBody().getElement(), "dialog-modal-body");

        if(backgroundClass !=null)
            GwtClientUtils.addClassName(body, backgroundClass);

        BaseImage.initImageText(getTitleWidget(), caption, null, ImageHtmlOrTextType.FORM);
    }

    private Element focusedElement;

    @Override
    public void show(Integer insertIndex, PopupOwner popupOwner) {
        MainFrame.closeNavigatorMenu();

        focusedElement = FocusUtils.getFocusedElement();
        MainFrame.setModalPopup(true);

        super.show(insertIndex, popupOwner);

        if (previewRegistration == null)
            previewRegistration = Event.addNativePreviewHandler(DialogModalWindow::previewNativeEvent);
    }

    private static boolean hasShownDialog() {
        RootPanel rootPanel = RootPanel.get();
        for (int i = 0, size = rootPanel.getWidgetCount(); i < size; i++)
            if (rootPanel.getWidget(i) instanceof DialogModalWindow)
                return true;
        return false;
    }

    private static void previewNativeEvent(Event.NativePreviewEvent event) {
        if (Event.ONKEYDOWN != event.getTypeInt() || event.isConsumed())
            return;

        ModalWindow topmostModal = getTopmostModal();
        if (!(topmostModal instanceof DialogModalWindow))
            return;
        DialogModalWindow topmost = (DialogModalWindow) topmostModal;

        NativeEvent nativeEvent = event.getNativeEvent();
        if (GKeyStroke.isEscapeKeyEvent(nativeEvent)) {
            GwtClientUtils.stopPropagation(nativeEvent);
            topmost.closeOnEscape();
        }
    }

    protected void closeOnEscape() {
    }

    @Override
    public void hide() {
        // for ResizableModalWindow this case is proceeded in ModalForm (using prevForm)
        if(focusedElement != null) {
            FocusUtils.focus(focusedElement, FocusUtils.Reason.RESTOREFOCUS);
            focusedElement = null; // just in case because sometimes hide is called without show (and the same DialogModalBox is used several time)
        }

        super.hide();

        boolean anyShown = hasShownDialog();
        if (!anyShown && previewRegistration != null) {
            previewRegistration.removeHandler();
            previewRegistration = null;
        }
        MainFrame.setModalPopup(anyShown);
    }
}
