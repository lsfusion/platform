package platform.gwt.form.client.form;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.form.client.ErrorHandlingCallback;
import platform.gwt.form.client.dispatch.NavigatorDispatchAsync;
import platform.gwt.form.client.form.ui.dialog.GResizableModalForm;
import platform.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import platform.gwt.form.shared.actions.GetForm;
import platform.gwt.form.shared.actions.GetFormResult;
import platform.gwt.form.shared.view.GForm;
import platform.gwt.form.shared.view.window.GModalityType;

public abstract class DefaultFormsController extends SimpleLayoutPanel implements FormsController {
    private final TabLayoutPanel tabsPanel;

    public DefaultFormsController() {
        tabsPanel = new TabLayoutPanel(21, Style.Unit.PX);

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
            public void success(GetFormResult result) {
                if (!GWT.isScript() && formSID != null) {
                    result.form.caption += "(" + formSID + ")";
                }

                openForm(dockable, result.form, modalityType, null);
            }
        });
    }

    public void openForm(GForm form, GModalityType modalityType, final WindowHiddenHandler hiddenHandler) {
        openForm(null, form, modalityType, hiddenHandler);
    }

    private void openForm(FormDockable dockable, GForm form, GModalityType modalityType, final WindowHiddenHandler hiddenHandler) {
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
