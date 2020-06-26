package lsfusion.gwt.client.form.view;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.EscapeUtils;
import lsfusion.gwt.client.base.TooltipManager;
import lsfusion.gwt.client.base.view.FlexPanel;
import lsfusion.gwt.client.base.view.GFlexAlignment;
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.client.form.controller.DefaultFormsController;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.GFormController;

import static lsfusion.gwt.client.view.StyleDefaults.VALUE_HEIGHT;

public final class FormDockable {
    private TabWidget tabWidget;
    private ContentWidget contentWidget;
    private FormDockable blockingForm; //GFormController

    private GFormController form;

    private WindowHiddenHandler hiddenHandler;

    public FormDockable(GForm form) {
        tabWidget = new TabWidget("");
        tabWidget.setBlocked(false);

        contentWidget = new ContentWidget(null);
    }

    public void setCaption(String caption, String tooltip) {
        tabWidget.setTitle(caption);
        tabWidget.setTooltip(tooltip);
    }

    public void initialize(final FormsController formsController, final GForm gForm) {
        form = new GFormController(formsController, gForm, false, false) {
            @Override
            public void onFormHidden(int closeDelay) {
                super.onFormHidden(closeDelay);
                if (hiddenHandler != null) {
                    hiddenHandler.onHidden();
                }
            }

            @Override
            public void setFormCaption(String caption, String tooltip) {
                setCaption(caption, tooltip);
            }

            @Override
            public void block() {
                tabWidget.setBlocked(true);
                contentWidget.setBlocked(true);
            }

            @Override
            public void setBlockingForm(FormDockable blocking) {
                blockingForm = blocking;
            }

            @Override
            public void unblock() {
                tabWidget.setBlocked(false);
                contentWidget.setBlocked(false);
                if(contentWidget.isAttached())
                    getFormsController().selectTab(FormDockable.this);
            }

            @Override
            protected void onInitialFormChangesReceived() {
                contentWidget.setContent(this);
                super.onInitialFormChangesReceived();
            }
        };
    }

    public GFormController getForm() {
        return form;
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


    public class ContentWidget extends LayoutPanel {
        private final Widget mask;
        private FocusPanel maskWrapper;
        private Widget content;

        private ContentWidget(Widget content) {
            mask = new SimpleLayoutPanel();
            mask.setStyleName("dockableBlockingMask");
            maskWrapper = new FocusPanel(mask);
            maskWrapper.setStyleName("dockableBlockingMask");
            setContent(content);
        }

        public Widget getContent() {
            return content;
        }

        public void setContent(Widget icontent) {
            if (content != null) {
                remove(content);
            }

            content = icontent;
            if (content != null) {
                addFullSizeChild(content);
            }

            maskWrapper.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent clickEvent) {
                    if(content instanceof GFormController) {
                        FormsController formsController = ((GFormController) content).getFormsController();
                        if(formsController instanceof DefaultFormsController && blockingForm != null) {
                            formsController.selectTab(blockingForm);
                        }
                    }
                }

            });
        }

        private void addFullSizeChild(Widget child) {
            add(child);
            // since table (and other elements) has zoom 1 by default, having not integer px leads to some undesirable extra lines (for example right part of any grid gets doubled line)
            setWidgetLeftRight(child, 1, Style.Unit.PX, 1, Style.Unit.PX);
            setWidgetTopBottom(child, 1, Style.Unit.PX, 1, Style.Unit.PX);
        }

        public void setBlocked(boolean blocked) {
            if (blocked) {
                addFullSizeChild(maskWrapper);
            } else {
                remove(maskWrapper);
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

    private class TabWidget extends FlexPanel {
        private Label label;
        private Button closeButton;

        private String tooltip;

        public TabWidget(String title) {
            addStyleName("tabLayoutPanelTabWidget");
            
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
            closeButton.setSize(VALUE_HEIGHT - 2 + "px", VALUE_HEIGHT - 2 + "px");

            FlexPanel labelWrapper = new FlexPanel();
            labelWrapper.getElement().addClassName("tabLayoutPanelTabTitleWrapper");
            labelWrapper.add(label);
            add(labelWrapper, GFlexAlignment.CENTER);
            
            add(closeButton, GFlexAlignment.CENTER);

            closeButton.addClickHandler(event -> {
                event.stopPropagation();
                event.preventDefault();
                closePressed();
            });

            TooltipManager.registerWidget(this, new TooltipManager.TooltipHelper() {
                @Override
                public String getTooltip() {
                    return tooltip != null ? tooltip : null;
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

        public void setTitle(String title) {
            label.setText(title);
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
