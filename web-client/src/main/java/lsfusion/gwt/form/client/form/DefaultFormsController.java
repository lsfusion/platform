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
import lsfusion.gwt.base.client.GwtClientUtils;
import lsfusion.gwt.form.client.ErrorHandlingCallback;
import lsfusion.gwt.form.client.MainFrame;
import lsfusion.gwt.form.client.dispatch.NavigatorDispatchAsync;
import lsfusion.gwt.form.client.form.ui.GFormController;
import lsfusion.gwt.form.client.form.ui.dialog.GResizableModalForm;
import lsfusion.gwt.form.client.form.ui.dialog.WindowHiddenHandler;
import lsfusion.gwt.form.client.form.ui.layout.GAbstractContainerView;
import lsfusion.gwt.form.client.form.ui.layout.GFormLayout;
import lsfusion.gwt.form.client.form.ui.layout.TabbedContainerView;
import lsfusion.gwt.form.shared.actions.GetForm;
import lsfusion.gwt.form.shared.actions.GetFormResult;
import lsfusion.gwt.form.shared.view.GComponent;
import lsfusion.gwt.form.shared.view.GContainer;
import lsfusion.gwt.form.shared.view.GFontMetrics;
import lsfusion.gwt.form.shared.view.GForm;
import lsfusion.gwt.form.shared.view.grid.EditEvent;
import lsfusion.gwt.form.shared.view.window.GModalityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DefaultFormsController implements FormsController {
    private final TabLayoutPanel tabsPanel;
    private List<String> formsList = new ArrayList<>();
    private List<GFormController> gFormControllersList = new ArrayList<>();
    private Map<String, GFormController> gFormControllersMap = new HashMap<>();

    public DefaultFormsController() {
        tabsPanel = new TabLayoutPanel(21, Style.Unit.PX);
        tabsPanel.addSelectionHandler(new SelectionHandler<Integer>() {
            @Override
            public void onSelection(SelectionEvent<Integer> event) {
                int selected = tabsPanel.getSelectedIndex();
                ((FormDockable.ContentWidget) tabsPanel.getWidget(selected)).setSelected(true);
                if(gFormControllersList.size() > selected)
                    setCurForm(gFormControllersList.get(selected));
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

    public Widget openForm(GForm form, GModalityType modalityType, EditEvent initFilterEvent, WindowHiddenHandler hiddenHandler) {
        return openFormAfterFontsInitialization(null, form, modalityType, initFilterEvent, hiddenHandler);
    }

    private Widget openFormAfterFontsInitialization(final FormDockable dockable, final GForm form, final GModalityType modalityType, final EditEvent initFilterEvent, final WindowHiddenHandler hiddenHandler) {
        // перед открытием формы необходимо рассчитать размеры используемых шрифтов
        return GFontMetrics.calculateFontMetrics(form.usedFonts, new GFontMetrics.MetricsCallback() {
            @Override
            public Widget metricsCalculated() {
                return openForm(dockable, form, modalityType, initFilterEvent, hiddenHandler);
            }
        });
    }

    private Widget openForm(FormDockable dockable, final GForm form, GModalityType modalityType, EditEvent initFilterEvent, final WindowHiddenHandler hiddenHandler) {
        if (!GWT.isScript()) {
            form.caption += "(" + form.sID + ")";
        }
        if (modalityType.isModalWindow()) {
            GResizableModalForm modalForm = showModalForm(form, modalityType, initFilterEvent, hiddenHandler);
            setCurForm(modalForm.getForm());
        } else {
            if (dockable == null) {
                dockable = addDockable(new FormDockable(this, form), form.sID);
            } else {
                dockable.initialize(this, form);
            }
            setCurForm(dockable.getForm());

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
        return dockable == null ? null : dockable.getContentWidget();
    }

    public void selectTab(Widget widget) {
        tabsPanel.selectTab(widget);
    }

    private GResizableModalForm showModalForm(GForm form, GModalityType modality, EditEvent initFilterEvent, final WindowHiddenHandler handler) {
        assert modality.isModalWindow();

        return GResizableModalForm.showForm(this, form, modality.isDialog(), initFilterEvent, handler);
    }

    private FormDockable addDockable(FormDockable dockable, String formSID) {
        formsList.add(formSID);
        tabsPanel.add(dockable.getContentWidget(), dockable.getTabWidget());
        tabsPanel.selectTab(dockable.getContentWidget());
        return dockable;
    }

    private void removeDockable(FormDockable dockable, String formSID) {
        dropCurForm(dockable.getForm());
        formsList.remove(formSID);
        tabsPanel.remove(dockable.getContentWidget());
    }

    private void setCurForm(GFormController form) {
        if(!(gFormControllersList.contains(form))) {
            gFormControllersList.add(form);
            gFormControllersMap.put(form.getForm().sID, form);
            setCurrentForm(form.getForm().sID);
        }
    }

    @Override
    public void dropCurForm(GFormController form) {
        if(form != null) {
            gFormControllersList.remove(form);
            gFormControllersMap.remove(form.getForm().sID);
            if (gFormControllersList.isEmpty())
                setCurrentForm(null);
        }
    }

    @Override
    public void selectTab(String formID, String tabID) {
        FormDockable.ContentWidget selectedFormWidget = (FormDockable.ContentWidget) tabsPanel.getWidget(tabsPanel.getSelectedIndex());
        GFormLayout formLayout = ((GFormController) selectedFormWidget.getContent()).getFormLayout();
        Object[] parent = findContainerParent(formLayout, formLayout.getMainContainer(), tabID);
        if (parent != null) {
            GAbstractContainerView view = (GAbstractContainerView) parent[0];
            if (view instanceof TabbedContainerView) {
                ((TabbedContainerView) view).selectTab((Integer) parent[1]);
            }
        }
    }

    private Object[] findContainerParent(GFormLayout formLayout, GContainer parent, String tabID) {
        GAbstractContainerView view = formLayout.getFormContainer(parent);
        int c = 0;
        for (int i = 0; i < parent.children.size(); i++) {
            GComponent child = parent.children.get(i);
            if (child instanceof GContainer) {
                if (child.sID != null && child.sID.equals(tabID))
                    return new Object[]{view, c};
                else {
                    Object[] result = findContainerParent(formLayout, (GContainer) child, tabID);
                    if (result != null)
                        return result;
                }
            }
            if (view instanceof TabbedContainerView && ((TabbedContainerView) view).isTabVisible(i))
                c++;
        }
        return null;
    }

    public GFormController getForm(String sid) {
        return gFormControllersMap.get(sid);
    }

    @Override
    public void select(Widget tabContent) {
        tabsPanel.selectTab(tabContent);
    }
}
