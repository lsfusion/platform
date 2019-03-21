package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.logical.shared.BeforeSelectionEvent;
import com.google.gwt.event.logical.shared.BeforeSelectionHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.ui.TabLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.MainFrame;
import lsfusion.gwt.client.form.FormDockable;
import lsfusion.gwt.client.form.ModalForm;
import lsfusion.gwt.client.base.ui.WindowHiddenHandler;
import lsfusion.gwt.shared.form.GForm;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;
import lsfusion.gwt.shared.navigator.window.GModalityType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class DefaultFormsController implements FormsController {
    private final TabLayoutPanel tabsPanel;
    private List<String> formsList = new ArrayList<>();
    private List<GFormController> gFormControllersList = new ArrayList<>();
    private Map<String, GFormController> gFormControllersMap = new HashMap<>();
    private final String tabSID;

    public DefaultFormsController(String tabSID) {
        this.tabSID = tabSID;
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
    }

    public Widget getView() {
        return tabsPanel;
    }

    public Widget openForm(GForm form, GModalityType modalityType, boolean forbidDuplicate, EditEvent initFilterEvent, WindowHiddenHandler hiddenHandler) {
        if(forbidDuplicate && MainFrame.forbidDuplicateForms && formsList.contains(form.sID)) {
            tabsPanel.selectTab(formsList.indexOf(form.sID));
            return null;
        } else {
            return openFormAfterFontsInitialization(null, form, modalityType, initFilterEvent, hiddenHandler);
        }
    }

    private Widget openFormAfterFontsInitialization(final FormDockable dockable, final GForm form, final GModalityType modalityType, final EditEvent initFilterEvent, final WindowHiddenHandler hiddenHandler) {
        // перед открытием формы необходимо рассчитать размеры используемых шрифтов
        return openForm(dockable, form, modalityType, initFilterEvent, hiddenHandler);
//        return GFontMetrics.calculateFontMetrics(form.usedFonts, new GFontMetrics.MetricsCallback() {
//            @Override
//            public Widget metricsCalculated() {
//                return openForm(dockable, form, modalityType, initFilterEvent, hiddenHandler);
//            }
//        });
    }

    private Widget openForm(FormDockable dockable, final GForm form, GModalityType modalityType, EditEvent initFilterEvent, final WindowHiddenHandler hiddenHandler) {
        if (!GWT.isScript()) {
            form.caption += "(" + form.sID + ")";
        }
        if (modalityType.isModalWindow()) {
            ModalForm modalForm = showModalForm(form, modalityType, initFilterEvent, hiddenHandler);
            setCurForm(modalForm.getForm());
        } else {
            if (dockable == null) {
                dockable = addDockable(new FormDockable(), form.sID);
            }
            dockable.initialize(this, form); // initialize should be after addDockable, otherwise offsetTop and other sizes are not recalculated in preAfterUpdateTableData, and it breaks scrolling (for example LAST option at form opening)
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

    public void selectTab(String formCanonicalName) {
        tabsPanel.selectTab(formsList.indexOf(formCanonicalName));
    }

    private ModalForm showModalForm(GForm form, GModalityType modality, EditEvent initFilterEvent, final WindowHiddenHandler handler) {
        assert modality.isModalWindow();

        return ModalForm.showForm(this, form, modality.isDialog(), initFilterEvent, handler);
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

    public GFormController getForm(String sid) {
        return gFormControllersMap.get(sid);
    }

    @Override
    public void select(Widget tabContent) {
        tabsPanel.selectTab(tabContent);
    }
}
