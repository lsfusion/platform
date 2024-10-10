package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.design.view.GFormLayout;

public class ModalWindow extends ResizableComplexPanel {

    protected final SimplePanel modal;

    protected final SimplePanel dialog;

    protected ResizableComplexPanel header;

    private final ResizableComplexPanel content;

    protected final SizedFlexPanel body;

    private final Widget title;

    protected final DivWidget modalBackDrop;

    private final boolean resizable;

    public ModalWindow(boolean resizable, ModalWindowSize size) {
        super();

        this.resizable = resizable;

       GwtClientUtils.addClassName(this, "modal-form");

        modalBackDrop = new DivWidget();
        GwtClientUtils.addClassName(modalBackDrop, "modal-backdrop");
        GwtClientUtils.addClassName(modalBackDrop, "fade");
        GwtClientUtils.addClassName(modalBackDrop, "show");
        add(modalBackDrop);

        modal = new SimplePanel();
        GwtClientUtils.addClassName(modal, "modal");
        GwtClientUtils.addClassName(modal, "fade");
        add(modal);

        dialog = new SimplePanel();
        GwtClientUtils.addClassName(dialog, "modal-dialog");
        if (resizable)
            GwtClientUtils.addClassName(dialog, "modal-resizable");
        else
            GwtClientUtils.addClassName(dialog, "modal-dialog-centered");

        if (size != null) {
            switch (size) {
                case FIT_CONTENT:
                    GwtClientUtils.addClassName(dialog, "modal-fit-content");
                    break;
                case LARGE:
                    GwtClientUtils.addClassName(dialog, "modal-lg");
                    break;
                case EXTRA_LARGE:
                    GwtClientUtils.addClassName(dialog, "modal-xl");
                    break;
            }
        }

        modal.setWidget(dialog);

        header = new ResizableComplexPanel();
        GwtClientUtils.addClassName(header, "modal-header");

        title = GFormLayout.createModalWindowCaptionWidget();
        GwtClientUtils.addClassName(title, "modal-title");
        header.add(title);

        content = new ResizableComplexPanel();
        GwtClientUtils.addClassName(content, "modal-content");
        GwtClientUtils.addClassName(content, "bg-body-tertiary");  // it makes sense to be equal to the class for the forms-container in the BaseLogicsModule.initNavigators
        dialog.setWidget(content);
        content.add(header);

        body = new SizedFlexPanel(true);
        GwtClientUtils.addClassName(body, "modal-body");
        content.add(body);

        GwtClientUtils.draggable(dialog.getElement(), ".modal-header");
        if (resizable)
            GwtClientUtils.resizable(content.getElement(), "e, s, se", nativeEvent -> body.onResize());
    }

    public void makeShadowOnScroll() {
        FlexPanel.makeShadowOnScroll(content, header, body, false);
    }

    public void show(PopupOwner popupOwner) {
        show(null, popupOwner);
    }

    protected PopupOwner showPopupOwner = null;
    public void show(Integer insertIndex, PopupOwner popupOwner) {
        // attaching
        RootPanel parentWidget = RootPanel.get();
        if(insertIndex != null) {
            parentWidget.insert(this, insertIndex);
        } else {
            parentWidget.add(this);
        }

        showPopupOwner = popupOwner;
        if(popupOwner != PopupOwner.GLOBAL)
            GwtClientUtils.addPopupPartner(popupOwner, getElement());

        GwtClientUtils.addClassName(modal, "show");

        onShow(); // need it after attach to have actual sizes calculated
    }

    public void onShow() {
        if (resizable)
            center();
    }

    public void center() {
        dialog.getElement().getStyle().setLeft((modal.getOffsetWidth() - dialog.getOffsetWidth()) >> 1, Style.Unit.PX);
        dialog.getElement().getStyle().setTop((modal.getOffsetHeight() - dialog.getOffsetHeight()) >> 1, Style.Unit.PX);
    }

    public void hide() {
        GwtClientUtils.removeClassName(modal, "show");

        PopupOwner popupOwner = showPopupOwner;
        if(popupOwner != PopupOwner.GLOBAL)
            GwtClientUtils.removePopupPartner(popupOwner, getElement(), true); // we rely on DialogModalWindow.hide + ModalForm (prevForm.onFocus) to handle focuses
        showPopupOwner = null;

        RootPanel.get().remove(this);
    }

    public Widget getTitleWidget() {
        return title;
    }

    private Widget bodyWidget;
    public void setBodyWidget(Widget widget) {
        if(bodyWidget != null)
            body.removeSized(bodyWidget);

        bodyWidget = widget;
        body.addFillShrinkSized(bodyWidget); // we want bodywidget to overglow in the both direction to have a scroll in the body element (see form-shrink-tabbed-container)
//        widget.getElement().getStyle().setProperty("flex", "1 1 auto");
//        GwtClientUtils.setupPercentParent(widget.getElement());
    }

    public Widget getBody() {
        return body;
    }

    public void addContentWidget(Widget widget) {
        content.add(widget);
    }

    public Widget getContentWidget() { return content; }

    private ResizableComplexPanel footer;
    public void addFooterWidget(Widget widget) {
        if (footer == null) {
            footer = new ResizableComplexPanel();
            GwtClientUtils.addClassName(footer, "modal-footer");
            addContentWidget(footer);
        }

        footer.add(widget);
    }

    public Widget getFooterWidget() {
        return footer;
    }

    public enum ModalWindowSize {
        FIT_CONTENT, LARGE, EXTRA_LARGE
    }
}
