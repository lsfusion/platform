package lsfusion.gwt.form.client.form;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ErrorHandlingCallback;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.form.client.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.form.client.form.ui.dialog.GResizableModalForm;
import lsfusion.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.form.shared.actions.GetForm;
import lsfusion.gwt.form.shared.actions.GetFormResult;
import lsfusion.gwt.form.shared.view.GFontMetrics;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.gwt.form.shared.view.window.GModalityType;

public abstract class DefaultFormsController extends SimpleLayoutPanel implements FormsController {
    private final TabLayoutPanel tabsPanel;

    public DefaultFormsController() {
        tabsPanel = new TabLayoutPanel(21, Style.Unit.PX);
        tabsPanel.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                ((FormDockable.ContentWidget) tabsPanel.getWidget(tabsPanel.getSelectedIndex())).setSelected(true);
            }
        });
        tabsPanel.addBeforeSelectionHandler(new BeforeSelectionHandler<Integer>() {
            @Override
            public void onBeforeSelection(BeforeSelectionEvent<Integer> event) {
                if (tabsPanel.getSelectedIndex() > -1) {
                    ((FormDockable.ContentWidget) tabsPanel.getWidget(tabsPanel.getSelectedIndex())).setSelected(false);
                }
            }
        });

        add(tabsPanel);

        quickOpenForm();
    }

    private void quickOpenForm() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                String formSID = GwtClientUtils.getPageParameter("formSID");
                if (formSID != null) {
                    openForm(formSID, GModalityType.DOCKED);
                }
            }
        });
    }

    public void openForm(final String formSID, final GModalityType modalityType) {
        openForm(formSID, modalityType, false);
    }

    public void openForm(final String formSID, final GModalityType modalityType, final boolean suppressErrorMessages) {
        final FormDockable dockable = modalityType.isDialog() ? null : addDockable(new FormDockable());

        NavigatorDispatchAsync.Instance.get().execute(new GetForm(formSID, modalityType.isModal(), null), new ErrorHandlingCallback<GetFormResult>() {
            @Override
            public void failure(Throwable caught) {
                if (dockable != null) {
                    removeDockable(dockable);
                }
                super.failure(caught);
            }

            @Override
            protected void showErrorMessage(Throwable caught) {
                if (!suppressErrorMessages) {
                    super.showErrorMessage(caught);
                }
            }

            @Override
            public void success(GetFormResult result) {
                openFormAfterFontsInitialization(dockable, result.form, modalityType, null);
            }
        });
    }

    public void openForm(GForm form, GModalityType modalityType, WindowHiddenHandler hiddenHandler) {
        openFormAfterFontsInitialization(null, form, modalityType, hiddenHandler);
    }

    private void openFormAfterFontsInitialization(final FormDockable dockable, final GForm form, final GModalityType modalityType, final WindowHiddenHandler hiddenHandler) {
        // перед открытием формы необходимо рассчитать размеры используемых шрифтов
        GFontMetrics.calculateFontMetrics(form.usedFonts, new GFontMetrics.MetricsCallback() {
            @Override
            public void metricsCalculated() {
                openForm(dockable, form, modalityType, hiddenHandler);
            }
        });
    }

    private void openForm(FormDockable dockable, GForm form, GModalityType modalityType, final WindowHiddenHandler hiddenHandler) {
        if (!GWT.isScript()) {
            form.caption += "(" + form.sID + ")";
        }
        if (modalityType.isDialog()) {
            showModalForm(form, modalityType.isFullScreen(), hiddenHandler);
        } else {
            if (dockable == null) {
                dockable = addDockable(new FormDockable(this, form));
            } else {
                dockable.initialize(this, form);
            }

            final FormDockable finalDockable = dockable;
            dockable.setHiddenHandler(new WindowHiddenHandler() {
                @Override
                public void onHidden() {
                    if (hiddenHandler != null) {
                        hiddenHandler.onHidden();
                    }
                    removeDockable(finalDockable);
                }
            });
        }
    }

    private FormDockable addDockable(FormDockable dockable) {
        tabsPanel.add(dockable.getContentWidget(), dockable.getTabWidget());
        tabsPanel.selectTab(dockable.getContentWidget());
        return dockable;
    }

    private void removeDockable(FormDockable dockable) {
        tabsPanel.remove(dockable.getContentWidget());
    }

    public void showModalForm(GForm form, boolean isFullScreen, final WindowHiddenHandler handler) {
        //todo: use isFullScreen
        GResizableModalForm.showForm(this, form, handler);
    }

    @Override
    public void select(Widget tabContent) {
        tabsPanel.selectTab(tabContent);
    }
}
