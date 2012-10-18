package platform.gwt.form2.client.form;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import platform.gwt.base.client.ErrorAsyncCallback;
import platform.gwt.base.client.GwtClientUtils;
import platform.gwt.form2.client.dispatch.NavigatorDispatchAsync;
import platform.gwt.form2.client.form.ui.dialog.GResizableModalForm;
import platform.gwt.form2.client.form.ui.dialog.WindowHiddenHandler;
import platform.gwt.form2.shared.actions.GetForm;
import platform.gwt.form2.shared.actions.GetFormResult;
import platform.gwt.form2.shared.view.GForm;
import platform.gwt.form2.shared.view.window.GModalityType;

public class DefaultFormsController extends SimpleLayoutPanel implements FormsController {
    private final TabLayoutPanel tabsPanel;

    public DefaultFormsController() {
        tabsPanel = new TabLayoutPanel(2, Style.Unit.EM);

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
        //todo: добавить loading-компонент перед вызовом и убрать его перед добавлением формы..
        NavigatorDispatchAsync.Instance.get().execute(new GetForm(formSID, modalityType.isModal(), null), new ErrorAsyncCallback<GetFormResult>() {
            @Override
            public void success(GetFormResult result) {
                if (!GWT.isScript() && formSID != null) {
                    result.form.caption += "(" + formSID + ")";
                }

                openForm(result.form, modalityType, null);
            }
        });
    }

    public void openForm(GForm form, GModalityType modalityType, final WindowHiddenHandler hiddenHandler) {
        if (modalityType.isDialog()) {
            showModalForm(form, modalityType.isFullScreen(), hiddenHandler);
        } else {
            final FormDockable formDockable = new FormDockable(this, form);

            formDockable.setHiddenHandler(new WindowHiddenHandler() {
                @Override
                public void onHidden() {
                    if (hiddenHandler != null) {
                        hiddenHandler.onHidden();
                    }
                    tabsPanel.remove(formDockable.getContentWidget());
                }
            });
            tabsPanel.add(formDockable.getContentWidget(), formDockable.getTabWidget());
            tabsPanel.selectTab(formDockable.getContentWidget());
        }
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
