package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;

public class ResizableSystemModalWindow extends ResizableModalWindow {

    public ResizableSystemModalWindow(String caption) {
        super();
        setCaption(caption);
    }

    private WindowHiddenHandler hiddenHandler;

    private Element focusedElement;

    @Override
    public void show() {
        focusedElement = GwtClientUtils.getFocusedElement();

        super.show();
    }

    @Override
    public void hide() {
        super.hide();

        if (hiddenHandler != null)
            hiddenHandler.onHidden();

        if(focusedElement != null)
            focusedElement.focus();
    }

    public void setWindowHiddenHandler(WindowHiddenHandler hiddenHandler) {
        this.hiddenHandler = hiddenHandler;
    }
}
