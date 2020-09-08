package lsfusion.gwt.client.form.controller;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.storage.client.Storage;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.base.view.ResizableSimplePanel;
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.form.view.ModalForm;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.window.GModalityType;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.StyleDefaults;

import java.util.ArrayList;
import java.util.List;

import static lsfusion.gwt.client.base.GwtClientUtils.findInList;

public abstract class FormsController {
    private final ClientMessages messages = ClientMessages.Instance.get();

    private final FlexTabbedPanel tabsPanel;

    private final List<FormDockable> forms = new ArrayList<>();
    private final List<Integer> formFocusOrder = new ArrayList<>();
    private int focusOrderCount;

    private final WindowsController windowsController;
    private final ResizableSimplePanel formsContainer;

    private final GToolbarButton linkEditButton;

    private final GToolbarButton fullScreenButton;
    private Boolean fullScreenMode = null;

    public FormsController(WindowsController windowsController) {
        this.windowsController = windowsController;

        GToolbarView toolbarView = new GToolbarView();

        linkEditButton = new GToolbarButton("linkEditMode.png", messages.linkEditModeEnable()) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> updateLinkEditMode(!GFormController.isLinkEditMode(), false);
            }
        };
        setCompactSize(linkEditButton);
        toolbarView.addComponent(linkEditButton);

        fullScreenButton = new GToolbarButton(null) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> switchFullScreenMode();
            }
        };
        setCompactSize(fullScreenButton);
        toolbarView.addComponent(fullScreenButton);

        formsContainer = new ResizableSimplePanel();

        tabsPanel = new FlexTabbedPanel("formsTabBar", toolbarView);

        // unselected (but not removed)
        tabsPanel.setBeforeSelectionHandler(index -> {
            int selectedTab = tabsPanel.getSelectedTab();
            if(selectedTab >= 0) {
                forms.get(selectedTab).onBlur(isRemoving);
                isRemoving = false;
            }
        });
        tabsPanel.setSelectionHandler(index -> {
            forms.get(index).onFocus(isAdding);
            formFocusOrder.set(index, focusOrderCount++);
            isAdding = false;
        });
    }

    private void setCompactSize(GToolbarButton button) {
        button.setSize(StyleDefaults.VALUE_HEIGHT_STRING, StyleDefaults.VALUE_HEIGHT_STRING);
    }

    private boolean isRemoving = false;
    private boolean isAdding = false;

    public void updateLinkEditMode(boolean linkEditMode, boolean linkEditModeWithCtrl) {
        GFormController.setLinkEditMode(linkEditMode, linkEditModeWithCtrl);
        linkEditButton.showBackground(linkEditMode);
        linkEditButton.setTitle(linkEditMode ? messages.linkEditModeDisable() : messages.linkEditModeEnable() + " (CTRL)");
    }

    public void storeFullScreen(){
        Storage storage = Storage.getLocalStorageIfSupported();
        if (this.fullScreenMode) {
            storage.setItem("full_screen", "");
        } else {
            storage.removeItem("full_screen");
        }
    }

    public void updateFullScreenButton(){
        fullScreenButton.setTitle(fullScreenMode ? messages.fullScreenModeDisable() : messages.fullScreenModeEnable() + " (ALT+ENTER)");
        fullScreenButton.setModuleImagePath(fullScreenMode ? "minimize.png" : "maximize.png");
    }

    public void switchFullScreenMode() {
        setFullScreenMode(!fullScreenMode);
    }

    public void setFullScreenMode(boolean fullScreenMode) {
        if (this.fullScreenMode == null || fullScreenMode != this.fullScreenMode) {
            if (fullScreenMode) {
                maximizeTabsPanel();
            } else {
                normalizeTabsPanel();
            }
            this.fullScreenMode = fullScreenMode;
            updateFullScreenButton();
        }
    }

    public void restoreFullScreen() {
        Storage storage = Storage.getLocalStorageIfSupported();
        setFullScreenMode(storage != null && storage.getItem("full_screen") != null);
    }

    public void initRoot(FormsController formsController) {
        GFormController.initKeyEventHandler(RootPanel.get(), formsController, () -> {
            FormContainer currentForm = MainFrame.getCurrentForm();
            if(currentForm != null)
                return currentForm.getForm();
            return null;
        });

        restoreFullScreen();
    }
    public void updateRoot(Widget widget) {
        RootLayoutPanel.get().clear();
        RootLayoutPanel.get().add(widget);
    }

    public void maximizeTabsPanel(){
        updateRoot(tabsPanel);
    }

    public void normalizeTabsPanel() {
        formsContainer.setFillWidget(tabsPanel);
        updateRoot(windowsController.getRootView());
    }

    public Widget getView() {
        return formsContainer;
    }

    public FormContainer openForm(GForm form, GModalityType modalityType, boolean forbidDuplicate, Event initFilterEvent, WindowHiddenHandler hiddenHandler) {
        FormDockable duplForm;
        if(forbidDuplicate && MainFrame.forbidDuplicateForms && (duplForm = findForm(form.sID)) != null) {
            selectTab(duplForm);
            return null;
        }

        FormContainer formContainer = modalityType.isModalWindow() ? new ModalForm(this) : new FormDockable(this);

        initForm(formContainer, form, hiddenHandler, modalityType.isDialog(), initFilterEvent);

        formContainer.show();

        return formContainer;
    }

    public void initForm(FormContainer<?> formContainer, GForm form, WindowHiddenHandler hiddenHandler, boolean dialog, Event initFilterEvent) {
        formContainer.initForm(this, form, () -> {
            formContainer.hide();

            hiddenHandler.onHidden();
        }, dialog, initFilterEvent);
    }

    public void selectTab(FormDockable dockable) {
        tabsPanel.selectTab(forms.indexOf(dockable));
    }

    public void selectTab(String formCanonicalName) {
        FormDockable form = findForm(formCanonicalName);
        if(form != null)
            selectTab(form);
    }

    public FormDockable findForm(String formCanonicalName) {
        return findInList(forms, dockable -> dockable.getForm().getForm().sID.equals(formCanonicalName));
    }

    public void addDockable(FormDockable dockable) {
        forms.add(dockable);
        formFocusOrder.add(null);

        tabsPanel.add(dockable.getContentWidget(), dockable.getTabWidget());
        assert !isAdding;
        isAdding = true;
        selectTab(dockable);
        assert !isAdding;
    }

    public void removeDockable(FormDockable dockable) {
        int index = forms.indexOf(dockable);

        assert !isRemoving;
        isRemoving = true;
        tabsPanel.remove(index);
        assert !isRemoving;

        forms.remove(index);
        formFocusOrder.remove(index);
    }

    public void ensureTabSelected() {
        int size;
        if(tabsPanel.getSelectedTab() < 0 && (size = forms.size()) > 0) {
            FormDockable lastFocusedForm = null;
            int maxOrder = 0;
            int formOrder;
            for(int i=0;i<size;i++) {
                formOrder = formFocusOrder.get(i);
                if (lastFocusedForm == null || formOrder > maxOrder) {
                    lastFocusedForm = forms.get(i);
                    maxOrder = formOrder;
                }
            }
            selectTab(lastFocusedForm);
        }
    }

    public abstract void executeNavigatorAction(GNavigatorAction action, NativeEvent nativeEvent);

    public abstract void executeNotificationAction(String actionSID, int type);

}
