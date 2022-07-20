package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;

public class ModalWindow extends ResizableComplexPanel {

    protected final SimplePanel modal;

    protected ResizableComplexPanel header;

    private final ResizableComplexPanel content;

    private final SimplePanel body;

    private final HeadingElement title;

    protected final DivWidget modalBackDrop;

    public ModalWindow() {
        super();
        setStyleName("modal-form");

        modalBackDrop = new DivWidget();
        modalBackDrop.setStyleName("modal-backdrop");
        modalBackDrop.addStyleName("fade");
        modalBackDrop.addStyleName("show");
        add(modalBackDrop);

        modal = new SimplePanel();
        modal.setStyleName("modal");
        modal.addStyleName("fade");
        add(modal);

        SimplePanel dialog = new SimplePanel();
        dialog.setStyleName("modal-dialog");
        dialog.addStyleName("modal-dialog-centered");
        dialog.addStyleName("modal-fit-content");
        modal.setWidget(dialog);

        header = new ResizableComplexPanel();
        header.setStyleName("modal-header");

        title = Document.get().createHElement(5);
        title.setClassName("modal-title");
        header.getElement().appendChild(title);

        content = new ResizableComplexPanel();
        content.setStyleName("modal-content");
        dialog.setWidget(content);
        content.add(header);

        body = new SimplePanel();
        body.setStyleName("modal-body");
        content.add(body);

        GwtClientUtils.draggable(modal.getElement(), ".modal-header");
    }

    public void show() {
        show(null);
    }

    public void show(Integer insertIndex) {
        // attaching
        if(insertIndex != null) {
            RootPanel.get().insert(this, insertIndex);
        } else {
            RootPanel.get().add(this);
        }

        modal.addStyleName("show");

        onShow(); // need it after attach to have actual sizes calculated
    }

    public void onShow() {
    }

    public void hide() {
        modal.removeStyleName("show");
        RootPanel.get().remove(this);
    }

    public void setCaption(String caption) {
        title.setInnerText(caption);
    }

    public void setBodyWidget(Widget widget) {
        body.setWidget(widget);
    }

    public Widget getBodyWidget() {
        return body.getWidget();
    }

    public void addContentWidget(Widget widget) {
        content.add(widget);
    }
}
