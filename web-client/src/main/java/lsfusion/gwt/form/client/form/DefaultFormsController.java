package lsfusion.gwt.form.client.form;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.base.client.ErrorHandlingCallback;
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.form.client.MainFrame;
import lsfusion.gwt.form.client.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.form.client.form.ui.dialog.GResizableModalForm;
import lsfusion.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.form.shared.actions.GetForm;
import lsfusion.gwt.form.shared.actions.GetFormResult;
import lsfusion.gwt.form.shared.view.GFontMetrics;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.window.GModalityType;

import java.util.ArrayList;
import java.util.List;

public abstract class DefaultFormsController implements FormsController {
    private final TabLayoutPanel tabsPanel;
    private List<String> formsList = new ArrayList<>();

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

        quickOpenForm();
    }

    public Widget getView() {
        return tabsPanel;
    }

    private void quickOpenForm() {
        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
            @Override
            public void execute() {
                String formSIDs = GwtClientUtils.getPageParameter("formSID");
                if (formSIDs != null) {
                    for (String formSID : formSIDs.split(",")) {
                        openForm(formSID, formSID, GModalityType.DOCKED, null);
                    }
                }
            }
        });
    }

    public void openForm(final String canonicalName, final String formSID, final GModalityType modalityType, NativeEvent nativeEvent) {
        openForm(canonicalName, formSID, modalityType, false, nativeEvent);
    }

    public void openForm(final String canonicalName, final String formSID, final GModalityType modalityType, final boolean suppressErrorMessages) {
        openForm(canonicalName, formSID, modalityType, suppressErrorMessages, null);
    }

    public void openForm(final String canonicalName, final String formSID, final GModalityType modalityType, final boolean suppressErrorMessages, NativeEvent nativeEvent) {
        if(MainFrame.forbidDuplicateForms && formsList.contains(formSID) && (nativeEvent == null || !nativeEvent.getCtrlKey())) {
            tabsPanel.selectTab(formsList.indexOf(formSID));
        } else {

            final FormDockable dockable = modalityType.isModalWindow() ? null : addDockable(new FormDockable(), formSID);

            NavigatorDispatchAsync.Instance.get().execute(new GetForm(canonicalName, formSID, modalityType.isModal(), null), new ErrorHandlingCallback<GetFormResult>() {
                @Override
                public void failure(Throwable caught) {
                    if (dockable != null) {
                        removeDockable(dockable, formSID);
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
                    openFormAfterFontsInitialization(dockable, result.form, modalityType, null, null);
                }
            });
        }
    }

    public void openForm(GForm form, GModalityType modalityType, EditEvent initFilterEvent, WindowHiddenHandler hiddenHandler) {
        openFormAfterFontsInitialization(null, form, modalityType, initFilterEvent, hiddenHandler);
    }

    private void openFormAfterFontsInitialization(final FormDockable dockable, final GForm form, final GModalityType modalityType, final EditEvent initFilterEvent, final WindowHiddenHandler hiddenHandler) {
        // перед открытием формы необходимо рассчитать размеры используемых шрифтов
        GFontMetrics.calculateFontMetrics(form.usedFonts, new GFontMetrics.MetricsCallback() {
            @Override
            public void metricsCalculated() {
                openForm(dockable, form, modalityType, initFilterEvent, hiddenHandler);
            }
        });
    }

    private void openForm(FormDockable dockable, final GForm form, GModalityType modalityType, EditEvent initFilterEvent, final WindowHiddenHandler hiddenHandler) {
        if (!GWT.isScript()) {
            form.caption += "(" + form.sID + ")";
        }
        if (modalityType.isModalWindow()) {
            showModalForm(form, modalityType, initFilterEvent, hiddenHandler);
        } else {
            if (dockable == null) {
                dockable = addDockable(new FormDockable(this, form), form.sID);
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
                    removeDockable(finalDockable, form.sID);
                }
            });
        }
    }

    private void showModalForm(GForm form, GModalityType modality, EditEvent initFilterEvent, final WindowHiddenHandler handler) {
        assert modality.isModalWindow();

        GResizableModalForm.showForm(this, form, modality.isDialog(), initFilterEvent, handler);
    }

    private FormDockable addDockable(FormDockable dockable, String formSID) {
        formsList.add(formSID);
        tabsPanel.add(dockable.getContentWidget(), dockable.getTabWidget());
        tabsPanel.selectTab(dockable.getContentWidget());
        return dockable;
    }

    private void removeDockable(FormDockable dockable, String formSID) {
        formsList.remove(formSID);
        tabsPanel.remove(dockable.getContentWidget());
    }

    @Override
    public void select(Widget tabContent) {
        tabsPanel.selectTab(tabContent);
    }
}
