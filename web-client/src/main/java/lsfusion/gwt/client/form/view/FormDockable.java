package lsfusion.gwt.client.form.view;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.design.view.GFormLayout;
import lsfusion.gwt.client.form.property.cell.controller.EndReason;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.window.GModalityWindowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;

public final class FormDockable extends FormContainer {
    private String canonicalName;
    private TabWidget tabWidget;
    private FormDockable blockingForm; //GFormController

    protected final FormDockable.ContentWidget contentWidget;

    @Override
    public GWindowFormType getWindowType() {
        return GModalityWindowFormType.DOCKED;
    }

    @Override
    protected Element getFocusedElement() {
        return contentWidget.getElement();
    }

    public FormDockable(FormsController formsController, String canonicalName, String caption, boolean async, Event editEvent) {
        super(formsController, async, editEvent);
        this.canonicalName = canonicalName;

        contentWidget = new ContentWidget();

        tabWidget = new TabWidget(caption);
        tabWidget.setBlocked(false);
        formsController.addContextMenuHandler(this);
    }

    @Override
    protected void setContent(Widget widget) {
        contentWidget.setContent(widget);
    }

    public void setCaption(String caption, String tooltip) {
        tabWidget.setTitle(caption);
        tabWidget.setTooltip(tooltip);
    }

    @Override
    public void show(GAsyncFormController asyncFormController) {
        showDockable(null);
    }

    public void showDockable(Integer index) {
        formsController.addDockable(this, index);
    }

    @Override
    public void hide(EndReason editFormCloseReason) {
        formsController.removeDockable(this);
    }

    public void block() {
        tabWidget.setBlocked(true);
        contentWidget.setBlocked(true);
    }

    public void setBlockingForm(FormDockable blocking) {
        blockingForm = blocking;
    }

    public void unblock() {
        tabWidget.setBlocked(false);
        contentWidget.setBlocked(false);
    }

    public Widget getTabWidget() {
        return tabWidget;
    }

    public Widget getContentWidget() {
        return contentWidget;
    }

    public String getCanonicalName() {
        return canonicalName;
    }

    public class ContentWidget extends SizedFlexPanel {
        private FocusPanel maskWrapper;
        private Widget content;

        // need this wrapper for paddings / margins (since content is )
        private ContentWidget() {
            super(true);

            initBlockedMask();

            // this is shrinked container and needs padding
            addStyleName("form-shrink-padded-container");
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
            addFillNoShrink(content);
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
                if (content instanceof GFormLayout && blockingForm != null) {
                    ((GFormLayout) content).getFormsController().selectTab(blockingForm);
                }
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

    private class TabWidget extends FlexPanel {
        private Label label;
        private FormButton closeButton;

        private String tooltip;

        public TabWidget(String title) {
            setTitle(title);

            TooltipManager.registerWidget(this, new TooltipManager.TooltipHelper() {
                @Override
                public String getTooltip() {
                    return tooltip;
                }

                @Override
                public boolean stillShowTooltip() {
                    return TabWidget.this.isAttached() && TabWidget.this.isVisible();
                }
            });
        }

        public void setBlocked(boolean blocked) {
            closeButton.setEnabled(!blocked);
        }

        private void addCloseButton () {
            closeButton = new FormButton() {
                @Override
                protected void onAttach() {
                    super.onAttach();
                    setTabIndex(-1);
                }
            };
            closeButton.addStyleName("btn-close");
            closeButton.addStyleName("tab-close-button");

            add(closeButton, GFlexAlignment.CENTER); //, 0, false, closeTabButtonWidth);

            closeButton.addClickHandler(event -> {
                event.stopPropagation();
                event.preventDefault();
                closePressed();
            });
        }

        public void setTitle(String title) {
            getElement().setInnerText(title);
            addCloseButton();
        }
        public void setTooltip(String tooltip) {
            this.tooltip = tooltip;
        }
    }

    private class LoadingWidget extends SimplePanel {
        private LoadingWidget() {
            setWidget(new Label("Loading...."));
        }
    }
}
