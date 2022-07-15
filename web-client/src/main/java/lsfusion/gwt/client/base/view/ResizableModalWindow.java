package lsfusion.gwt.client.base.view;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.Dimension;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.form.view.ModalForm;

public class ResizableModalWindow extends SimplePanel {

    private final SimplePanel body;

    private final HeadingElement title;

    private String tooltip;

    public ResizableModalWindow() {
        super();
        setStyleName("modal");
        addStyleName("fade");

        SimplePanel dialog = new SimplePanel();
        dialog.setStyleName("modal-dialog");
        dialog.addStyleName("modal-fit-content");
        setWidget(dialog);

        ResizableComplexPanel header = new ResizableComplexPanel();
        header.setStyleName("modal-header");

        TooltipManager.registerWidget(header, new TooltipManager.TooltipHelper() {
            @Override
            public String getTooltip() {
                return tooltip;
            }

            @Override
            public boolean stillShowTooltip() {
                return header.isAttached() && header.isVisible();
            }
        });

        title = Document.get().createHElement(5);
        title.setClassName("modal-title");
        header.getElement().appendChild(title);

        ResizableComplexPanel content = new ResizableComplexPanel();
        content.setStyleName("modal-content");
        dialog.setWidget(content);
        content.add(header);

        body = new SimplePanel();
        body.setStyleName("modal-body");
        content.add(body);

        GwtClientUtils.draggable(getElement(), ".modal-header");

        modalBackDrop = new DivWidget();
        modalBackDrop.setStyleName("modal-backdrop");
        modalBackDrop.addStyleName("fade");
        modalBackDrop.addStyleName("show");
    }

    private final DivWidget modalBackDrop;

    //This scheme is necessary when one modal window is started before the second one, but is displayed later due to delays.
    //In this case, the order in which the windows are displayed must be maintained according to the order of request indexes.
    private FormRequestData formRequestData;

    public void show(FormRequestData formRequestData, Integer insertIndex) {
        addStyleName("show");

        this.formRequestData = formRequestData;
        show(insertIndex);
    }

    public Pair<ModalForm, Integer> getFormInsertIndex(FormRequestData formRequestData) {
        AbsolutePanel boundaryPanel = RootPanel.get();
        for (int i = 0; i < boundaryPanel.getWidgetCount(); i++) {
            Widget widget = boundaryPanel.getWidget(i);
            if (widget instanceof ResizableModalWindow) {
                FormRequestData widgetData = ((ResizableModalWindow) widget).formRequestData;
                if (widgetData.isBefore(formRequestData)) {
                    return new Pair(widgetData.modalForm, i);
                }
            }
        }
        return null;
    }

    public void hide() {
        RootPanel.get().remove(this);
        RootPanel.get().remove(modalBackDrop);
        formRequestData = null;
        removeStyleName("show");
    }

    private final static class ModalMask {
        private final PopupPanel popup;

        private ModalMask() {
            popup = new PopupPanel();
            popup.setGlassEnabled(true);
            popup.getElement().getStyle().setOpacity(0);
        }

        public void show() {
            popup.center();
        }

        public void hide() {
            popup.hide();
        }
    }

    public void setCaption(String caption) {
        title.setInnerText(caption);
    }

    private void show(Integer insertIndex) {
        // attaching
        if(insertIndex != null) {
            RootPanel.get().insert(this, insertIndex);
            RootPanel.get().insert(modalBackDrop, insertIndex);
        } else {
            RootPanel.get().add(modalBackDrop);
            RootPanel.get().add(this);
        }

        onShow(); // need it after attach to have actual sizes calculated
    }

    public void onShow() {
        RootPanel.get().setWidgetPosition(this,
                (Window.getClientWidth() - getOffsetWidth()) / 2,
                (Window.getClientHeight() - getOffsetHeight()) / 2);
    }

    private Widget innerContentWidget;

    public void setContentWidget(Widget innerContentWidget) {
        this.innerContentWidget = innerContentWidget;
        GwtClientUtils.resizable(innerContentWidget.getElement(), "e, s, se");
        body.setWidget(innerContentWidget);
    }

    public void setContentSize(Dimension size) {
        Element element = innerContentWidget.getElement();
        FlexPanel.setWidth(element, size.width);
        FlexPanel.setHeight(element, size.height);
    }

    public void focus() {
        Element element = RootPanel.get().getElement();
        int tabIndex = element.getTabIndex();
        if (tabIndex == -1) {
            element.setTabIndex(0);
        }
        element.focus();
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }
}
