package lsfusion.gwt.client.form.controller;

import com.google.gwt.dom.client.Style;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.form.property.cell.controller.EditEvent;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.form.view.ModalForm;
import lsfusion.gwt.client.navigator.window.GModalityType;
import lsfusion.gwt.client.navigator.window.view.TabLayoutPanel;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.ArrayList;
import java.util.List;

public abstract class DefaultFormsController implements FormsController {
    private final TabLayoutPanel tabsPanel;
    private final List<String> formsList = new ArrayList<>();
    private final List<GFormController> gFormControllersList = new ArrayList<>(); // have no idea why it is a list and not a field
    private final WindowsController windowsController;
    private final ImageButton imageButton = new ImageButton("");
    private final ResizableSimplePanel resizableSimplePanel;
    // flag to update view
    private boolean originalView;
    private boolean fullScreenMode;

    public DefaultFormsController(WindowsController windowsController) {
        this.windowsController = windowsController;
        tabsPanel = new TabLayoutPanel(StyleDefaults.VALUE_HEIGHT + 1, Style.Unit.PX, updateViewButton()); // 1px for one side border
        resizableSimplePanel = new ResizableSimplePanel();
        resizableSimplePanel.setFillWidget(tabsPanel);
        tabsPanel.addSelectionHandler(event -> {
            int selected = tabsPanel.getSelectedIndex();
            ((FormDockable.ContentWidget) tabsPanel.getWidget(selected)).setSelected(true);
            if(gFormControllersList.size() > selected) {
                GFormController form = gFormControllersList.get(selected);
                form.gainedFocus();
                setCurrentForm(form);
            }
        });
        tabsPanel.addBeforeSelectionHandler(event -> {
            if (tabsPanel.getSelectedIndex() > -1) {
                ((FormDockable.ContentWidget) tabsPanel.getWidget(tabsPanel.getSelectedIndex())).setSelected(false);
            }
        });
    }

    public void updateLocalStorage(){
        Storage storage = Storage.getLocalStorageIfSupported();
        if (this.fullScreenMode) {
            storage.setItem("full_screen", "");
        } else {
            storage.removeItem("full_screen");
        }
    }

    public void updateButtonImage(){
        if (!originalView){
            imageButton.setModuleImagePath("minimize.png");
        } else {
            imageButton.setModuleImagePath("maximize.png");
        }
    }

    public void setFullScreenMode(boolean fullScreenMode) {
        updateView(fullScreenMode);
    }

    public void updateView(boolean fullScreen) {
        if (fullScreen) {
            maximizeTabsPanel();
            originalView = false;
        } else {
            normalizeTabsPanel();
            originalView = true;
        }
        this.fullScreenMode = fullScreen;
        updateButtonImage();
    }

    public void checkFullscreenOnStartup() {
        Storage storage = Storage.getLocalStorageIfSupported();
        setFullScreenMode(storage != null && storage.getItem("full_screen") != null);
    }

    public void maximizeTabsPanel(){
        RootLayoutPanel.get().clear();
        RootLayoutPanel.get().add(tabsPanel);
    }

    public void normalizeTabsPanel() {
        RootLayoutPanel.get().clear();
        resizableSimplePanel.setFillWidget(tabsPanel);
        RootLayoutPanel.get().add(windowsController.getRootView());
    }

    public Widget updateViewButton() {
        imageButton.setSize("20px", "20px");
        imageButton.addClickHandler(event -> {
            updateView(originalView);
        });
        return imageButton;
    }

    public  Widget getView() {
        return resizableSimplePanel;
    }

    public Widget openForm(GForm form, GModalityType modalityType, boolean forbidDuplicate, EditEvent initFilterEvent, WindowHiddenHandler hiddenHandler) {
        if(forbidDuplicate && MainFrame.forbidDuplicateForms && formsList.contains(form.sID)) {
            tabsPanel.selectTab(formsList.indexOf(form.sID));
            return null;
        }
        if (modalityType.isModalWindow()) {
            ModalForm modalForm = showModalForm(form, modalityType, initFilterEvent, hiddenHandler);
            registerForm(modalForm.getForm());
            return null;
        } else {
            FormDockable dockable = addDockable(new FormDockable(form), form.sID);
            dockable.initialize(this, form); // initialize should be after addDockable, otherwise offsetTop and other sizes are not recalculated in preAfterUpdateTableData, and it breaks scrolling (for example LAST option at form opening)
            registerForm(dockable.getForm());

            dockable.setHiddenHandler(() -> {
                if (hiddenHandler != null) {
                    hiddenHandler.onHidden();
                }
                removeDockable(dockable, form.sID);
            });
            return dockable.getContentWidget();
        }
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
        unregisterForm(dockable.getForm());
        formsList.remove(formSID);
        tabsPanel.remove(dockable.getContentWidget());
    }

    public void registerForm(GFormController form) {
        gFormControllersList.add(form);
        setCurrentForm(form);
    }

    @Override
    public void unregisterForm(GFormController form) {
        if(form != null) {
            gFormControllersList.remove(form);
            dropCurrentForm(form);
        }
    }

    public abstract void setCurrentForm(GFormController form);
    
    public abstract void dropCurrentForm(GFormController form);

    @Override
    public void select(Widget tabContent) {
        tabsPanel.selectTab(tabContent);
    }
}
