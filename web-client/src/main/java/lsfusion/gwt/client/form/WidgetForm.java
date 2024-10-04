package lsfusion.gwt.client.form;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.view.FormContainer;

public abstract class WidgetForm extends FormContainer {
    protected Widget captionWidget;

    protected final ContentWidget contentWidget;

    @Override
    public Element getContentElement() {
        return contentWidget.getElement();
    }

    public WidgetForm(FormsController formsController, GFormController contextForm, boolean async, Event editEvent, Widget captionWidget) {
        super(formsController, contextForm, async, editEvent);

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
            //GwtClientUtils.addXStyleName(this, "form-shrink-padded-container");
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
            GwtClientUtils.addClassName(mask, "dockable-blocking-mask", "dockableBlockingMask");
            maskWrapper = new FocusPanel(mask);
            GwtClientUtils.addClassName(maskWrapper, "dockable-blocking-mask", "dockableBlockingMask");
            maskWrapper.setVisible(false);
            addFill(maskWrapper);
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

           GwtClientUtils.addClassName(this, "btn-close");
           GwtClientUtils.addClassName(this, "tab-close-button");

            addClickHandler(event -> {
//                event.stopPropagation();
//                event.preventDefault();
                closePressed();
            });
        }
    }
}
