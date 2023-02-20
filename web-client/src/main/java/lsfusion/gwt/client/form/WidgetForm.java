package lsfusion.gwt.client.form;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.view.FormContainer;

public abstract class WidgetForm extends FormContainer {
    protected Widget captionWidget;

    protected final ContentWidget contentWidget;

    @Override
    public Element getFocusedElement() {
        return contentWidget.getElement();
    }

    public WidgetForm(FormsController formsController, boolean async, Event editEvent, Widget captionWidget) {
        super(formsController, async, editEvent);

        contentWidget = new ContentWidget();

        this.captionWidget = captionWidget;
    }

    @Override
    protected void setContent(Widget widget) {
        contentWidget.setContent(widget);
    }

    @Override
    public Widget getCaptionWidget() {
        return captionWidget;
    }

    public void block() {
        contentWidget.setBlocked(true);
    }

    public void unblock() {
        contentWidget.setBlocked(false);
    }


    protected abstract void onMaskClick();

    protected class ContentWidget extends SizedFlexPanel {
        private FocusPanel maskWrapper;
        private Widget content;

        // need this wrapper for paddings / margins (since content is )
        private ContentWidget() {
            super(true);

            initBlockedMask();

            // this is shrinked container and needs padding
//            addStyleName("form-shrink-padded-container");
        }

        public Widget getContent() {
            return content;
        }

        public void setContent(Widget widget) {
            if (content != null) {
                removeSized(content);
            }

            content = widget;
            // we want content to have it's content size minimum but 100%
            addFillShrinkSized(content);
        }

        private void initBlockedMask() {
            Widget mask = new SimpleLayoutPanel();
            mask.setStyleName("dockableBlockingMask");
            maskWrapper = new FocusPanel(mask);
            maskWrapper.setStyleName("dockableBlockingMask");
            maskWrapper.setVisible(false);
            add(maskWrapper);
            GwtClientUtils.setupFillParent(maskWrapper.getElement());
            maskWrapper.addClickHandler(clickEvent -> {
                onMaskClick();
            });
        }

//        private void addFullSizeChild(Widget child) {
//            add(child);
        // since table (and other elements) has zoom 1 by default, having not integer px leads to some undesirable extra lines (for example right part of any grid gets doubled line)
//            setWidgetLeftRight(child, 1, Style.Unit.PX, 1, Style.Unit.PX);
//            setWidgetTopBottom(child, 1, Style.Unit.PX, 1, Style.Unit.PX);
//        }

        public void setBlocked(boolean blocked) {
            if (blocked) {
                maskWrapper.setVisible(true);
            } else {
                maskWrapper.setVisible(false);
            }
        }
    }

    protected class CloseButton extends UnFocusableImageButton {
        public CloseButton() {
            super(null, null);

            addStyleName("btn-close");
            addStyleName("tab-close-button");

            addClickHandler(event -> {
                event.stopPropagation();
                event.preventDefault();
                closePressed();
            });
        }
    }
}
