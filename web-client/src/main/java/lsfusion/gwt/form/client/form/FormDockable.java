package lsfusion.gwt.form.client.form;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.base.client.EscapeUtils;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.form.shared.view.GForm;

final class FormDockable {
    private TabWidget tabWidget;
    private ContentWidget contentWidget;

    private GFormController form;

    private WindowHiddenHandler hiddenHandler;

    private boolean initialized = false;

    public FormDockable() {
        tabWidget = new TabWidget("(loading...)");
        tabWidget.setBlocked(true);

        contentWidget = new ContentWidget(new LoadingWidget());
    }

    public FormDockable(final FormsController formsController, GForm gForm) {
        tabWidget = new TabWidget("");
        contentWidget = new ContentWidget(null);

        initialize(formsController,  gForm);
    }

    public void initialize(final FormsController formsController, final GForm gForm) {
        if (initialized) {
            throw new IllegalStateException("Form dockable has already been initialized");
        }
        initialized = true;

        tabWidget.setTitle(gForm.caption);
        tabWidget.setBlocked(false);

        form = new GFormController(formsController, gForm) {
            @Override
            public void hideForm() {
                super.hideForm();
                if (hiddenHandler != null) {
                    hiddenHandler.onHidden();
                }
            }

            @Override
            public void block() {
                tabWidget.setBlocked(true);
                contentWidget.setBlocked(true);
            }

            @Override
            public void unblock() {
                tabWidget.setBlocked(false);
                contentWidget.setBlocked(false);
                formsController.select(contentWidget);
            }

            @Override
            protected void onInitialFormChangesReceived() {
                contentWidget.setContent(this);
                super.onInitialFormChangesReceived();
            }
        };
    }

    public void setHiddenHandler(WindowHiddenHandler hiddenHandler) {
        this.hiddenHandler = hiddenHandler;
    }

    private void closePressed() {
        form.closePressed();
    }

    public Widget getTabWidget() {
        return tabWidget;
    }

    public Widget getContentWidget() {
        return contentWidget;
    }


    public static class ContentWidget extends LayoutPanel {
        private final Widget mask;
        private Widget content;

        private ContentWidget(Widget content) {
            mask = new SimpleLayoutPanel();
            mask.setStyleName("dockableBlockingMask");

            setContent(content);
        }

        public void setContent(Widget icontent) {
            if (content != null) {
                remove(content);
            }

            content = icontent;
            if (content != null) {
                addFullSizeChild(content);
            }
        }

        private void addFullSizeChild(Widget child) {
            add(child);
            setWidgetLeftRight(child, 0, Style.Unit.PX, 0, Style.Unit.PX);
        }

        public void setBlocked(boolean blocked) {
            if (blocked) {
                addFullSizeChild(mask);
            } else {
                remove(mask);
            }

            if (content instanceof GFormController) {
                ((GFormController) content).setBlocked(blocked);
            }
        }

        public void setSelected(boolean selected) {
            if (content instanceof GFormController) {
                ((GFormController) content).setSelected(selected);
            }
        }
    }

    private class TabWidget extends HorizontalPanel {
        private Label label;
        private Button closeButton;

        public TabWidget(String title) {
            label = new Label(title);

            closeButton = new Button() {
                @Override
                protected void onAttach() {
                    super.onAttach();
                    setTabIndex(-1);
                }
            };
            closeButton.setText(EscapeUtils.UNICODE_CROSS);
            closeButton.setStyleName("closeTabButton");

            add(label);
            add(closeButton);

            closeButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    event.stopPropagation();
                    event.preventDefault();
                    closePressed();
                }
            });
        }

        public void setBlocked(boolean blocked) {
            closeButton.setEnabled(!blocked);
        }

        public void setTitle(String title) {
            label.setText(title);
        }
    }

    private class LoadingWidget extends SimplePanel {
        private LoadingWidget() {
            setWidget(new Label("Loading...."));
        }
    }
}
