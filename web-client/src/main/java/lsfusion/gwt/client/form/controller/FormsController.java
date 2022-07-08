package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.ContextMenuEvent;
import com.google.gwt.event.dom.client.ContextMenuHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.action.GFormAction;
import lsfusion.gwt.client.action.GHideFormAction;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.view.PopupDialogPanel;
import lsfusion.gwt.client.base.view.WindowHiddenHandler;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.action.RequestCountingAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.ExecuteNavigatorAction;
import lsfusion.gwt.client.form.EmbeddedForm;
import lsfusion.gwt.client.form.InnerForm;
import lsfusion.gwt.client.form.PopupForm;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;
import lsfusion.gwt.client.form.property.async.GAsyncOpenForm;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.ExecContext;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.form.view.ModalForm;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.controller.dispatch.GNavigatorActionDispatcher;
import lsfusion.gwt.client.navigator.window.GShowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.ColorThemeChangeListener;
import lsfusion.gwt.client.view.MainFrame;
import lsfusion.gwt.client.view.StyleDefaults;
import net.customware.gwt.dispatch.shared.Result;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static lsfusion.gwt.client.base.GwtClientUtils.findInList;

public abstract class FormsController {
    private final ClientMessages messages = ClientMessages.Instance.get();

    private final FlexTabbedPanel tabsPanel;

    private final List<FormDockable> forms = new ArrayList<>();
    private final List<Integer> formFocusOrder = new ArrayList<>();
    private int focusOrderCount;

    private final WindowsController windowsController;

    private int prevModeButton;
    private final EditModeButton editModeButton;

    private GToolbarButton fullScreenButton;
    private boolean fullScreenMode = false;
    
    private GToolbarButton mobileMenuButton;

    public FormsController(WindowsController windowsController) {
        this.windowsController = windowsController;

        GToolbarView toolbarView = new GToolbarView();

        editModeButton = new EditModeButton();
        toolbarView.addComponent(editModeButton);

        setupEditModeButton();

        if (!MainFrame.mobile) {
            fullScreenButton = new GToolbarButton(null) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> switchFullScreenMode();
                }
            };
            setCompactSize(fullScreenButton);
            toolbarView.addComponent(fullScreenButton);
            updateFullScreenButton();
        } else {
            mobileMenuButton = new GToolbarButton("hamburger.png") {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> MainFrame.openNavigatorMenu();
                }
            };
            setCompactSize(mobileMenuButton);
            toolbarView.addComponent(mobileMenuButton);
        }

        tabsPanel = new FlexTabbedPanel("formsTabBar", toolbarView);
        if (MainFrame.mobile) {
            tabsPanel.getElement().addClassName("mobileFormsTabPanel");
            tabsPanel.getElement().getStyle().setProperty("flexDirection", "column-reverse");
        }

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

        initEditModeTimer();
    }

    public void onServerInvocationResponse(ServerResponseResult response, GAsyncFormController asyncFormController) {
        if (asyncFormController.onServerInvocationOpenResponse()) {
            if (Arrays.stream(response.actions).noneMatch(a -> a instanceof GFormAction)) {
                asyncFormController.removeAsyncForm().queryHide(CancelReason.OTHER);
            }
        }
        if (asyncFormController.onServerInvocationCloseResponse()) {
            if (Arrays.stream(response.actions).noneMatch(a -> a instanceof GHideFormAction)) {
                Pair<FormContainer, Integer> asyncClosedForm = asyncFormController.removeAsyncClosedForm();
                asyncClosedForm.first.show(asyncFormController, asyncClosedForm.second);
            }
        }
    }

    private void setCompactSize(GToolbarButton button) {
        button.setSize(StyleDefaults.VALUE_HEIGHT_STRING, StyleDefaults.VALUE_HEIGHT_STRING);
    }

    private boolean isRemoving = false;
    private boolean isAdding = false;

    public void checkEditModeEvents(NativeEvent event) {
        Boolean ctrlKey = eventGetCtrlKey(event);
        Boolean shiftKey = eventGetShiftKey(event);
        Boolean altKey = eventGetAltKey(event);
        if(ctrlKey != null) {
            boolean onlyCtrl = ctrlKey && (shiftKey == null || !shiftKey) && (altKey == null || !altKey);
            pressedCtrl = onlyCtrl;
            if (onlyCtrl && !isLinkMode())
                setForceEditMode(EditMode.LINK);
            if (!onlyCtrl && isForceLinkMode())
                removeForceEditMode();
        }
        if(shiftKey != null) {
            boolean onlyShift = shiftKey && (ctrlKey == null || !ctrlKey) && (altKey == null || !altKey);
            pressedShift = onlyShift;
            if (onlyShift && !isDialogMode())
                setForceEditMode(EditMode.DIALOG);
            if (!onlyShift && isForceDialogMode())
                removeForceEditMode();
        }
    }

    // we need native method and not getCtrlKey, since some events (for example focus) have ctrlKey undefined and in this case we want to ignore them
    private native Boolean eventGetCtrlKey(NativeEvent evt) /*-{
        return evt.ctrlKey;
    }-*/;

    private native Boolean eventGetShiftKey(NativeEvent evt) /*-{
        return evt.shiftKey;
    }-*/;

    private native Boolean eventGetAltKey(NativeEvent evt) /*-{
        return evt.altKey;
    }-*/;

    private static EditMode editMode;
    private static EditMode prevEditMode;
    private static EditMode forceEditMode;

    private static boolean isForceLinkMode() {
        return forceEditMode == EditMode.LINK;
    }
    public static boolean isLinkMode() {
        return editMode == EditMode.LINK;
    }

    private static boolean isForceDialogMode() {
        return forceEditMode == EditMode.DIALOG;
    }
    public static boolean isDialogMode() {
        return editMode == EditMode.DIALOG;
    }

    public static int getEditModeIndex() {
        return forceEditMode != null ? prevEditMode.getIndex() : editMode.getIndex();
    }

    private void setupEditModeButton() {
        final String[] images = new String[3];
        GwtClientUtils.setThemeImage("defaultMode.png", image -> images[0] = image);
        GwtClientUtils.setThemeImage("linkMode.png", image -> images[1] = image);
        GwtClientUtils.setThemeImage("dialogMode.png", image -> images[2] = image);

        setupEditModeButton(editModeButton.getElement(), images, windowsController.restoreEditMode());
    }

    private native void setupEditModeButton(Element element, String[] images, int defaultIndex) /*-{
        var instance = this;
        var ddData = [{imageSrc: images[0], title: 'Default Mode'}, {imageSrc: images[1], title: 'Link Mode (CTRL)'}, {imageSrc: images[2], title: 'Dialog Mode (SHIFT)'}];

        $wnd.$(element).ddslick({
            data:ddData,
            defaultSelectedIndex:defaultIndex,
            width:16,
            onSelected: function(selectedData){
                instance.@FormsController::selectEditMode(*)(selectedData.selectedIndex);
            }
        });
    }-*/;

    private native int selectEditModeButton(int i) /*-{
        var modeButton = $wnd.$('#modeButton');
        var prevModeButton = modeButton.data('ddslick').selectedIndex;
        modeButton.ddslick('select', {index: i });
        return prevModeButton;

    }-*/;

    private native void destroyEditModeButton() /*-{
        var modeButton = $wnd.$('#modeButton');
        modeButton.ddslick('destroy');
    }-*/;

    private void setForceEditMode(EditMode mode) {
        prevEditMode = editMode;
        prevModeButton = selectEditModeButton(mode.getIndex());
        updateEditMode(mode, mode);
    }

    private void removeForceEditMode() {
        selectEditModeButton(prevModeButton);
        updateEditMode(EditMode.getMode(prevModeButton), null);
    }

    private void selectEditMode(int mode) {
        updateEditMode(EditMode.getMode(mode), null);
    }

    private void updateEditMode(EditMode mode, EditMode forceMode) {
        updateForceLinkModeStyles(mode, forceMode == EditMode.LINK);
        editMode = mode;
        forceEditMode = forceMode;
    }

    private void updateForceLinkModeStyles(EditMode editMode, boolean forceLinkMode) {
        boolean linkMode = editMode == EditMode.LINK;
        if(!isForceLinkMode() && forceLinkMode) {
            scheduleLinkModeStylesTimer(() -> setLinkModeStyles(linkMode));
        } else {
            cancelLinkModeStylesTimer();
            setLinkModeStyles(linkMode);
        }
    }

    private static Timer linkModeStylesTimer;
    private static void scheduleLinkModeStylesTimer(Runnable setLinkModeStyles) {
        if(linkModeStylesTimer == null) {
            linkModeStylesTimer = new Timer() {
                @Override
                public void run() {
                    setLinkModeStyles.run();
                    linkModeStylesTimer = null;
                }
            };
            linkModeStylesTimer.schedule(250);
        }
    }
    private static void cancelLinkModeStylesTimer() {
        if (linkModeStylesTimer != null) {
            linkModeStylesTimer.cancel();
            linkModeStylesTimer = null;
        }
    }

    private void setLinkModeStyles(boolean linkMode) {
        Element globalElement = RootPanel.get().getElement();
        if(linkMode)
            globalElement.addClassName("linkMode");
        else
            globalElement.removeClassName("linkMode");
    }

    private static Timer editModeTimer;
    public static boolean pressedCtrl = false;
    public static boolean pressedShift = false;
    private void initEditModeTimer() {
        if(editModeTimer == null) {
            editModeTimer = new Timer() {
                @Override
                public void run() {
                    if (pressedCtrl) {
                        pressedCtrl = false;
                    } else if(pressedShift) {
                        pressedShift = false;
                    } else {
                        if (isForceLinkMode() || isForceDialogMode()) {
                            removeForceEditMode();
                            updateEditMode(EditMode.DEFAULT, null);
                        }
                    }
                }
            };
            editModeTimer.scheduleRepeating(500); //delta between first and second events ~500ms, between next ~30ms
        }
    }

    public void updateFullScreenButton(){
        fullScreenButton.setTitle(fullScreenMode ? messages.fullScreenModeDisable() : messages.fullScreenModeEnable() + " (ALT+F11)");
        fullScreenButton.setModuleImagePath(fullScreenMode ? "minimize.png" : "maximize.png");
    }

    public void switchFullScreenMode() {
        setFullScreenMode(!fullScreenMode);
    }

    public void setFullScreenMode(boolean fullScreenMode) {
        if (fullScreenMode != this.fullScreenMode) {
            windowsController.setFullScreenMode(fullScreenMode);
            this.fullScreenMode = fullScreenMode;
            updateFullScreenButton();
        }
    }

    public void initRoot(FormsController formsController) {
        GFormController.initKeyEventHandler(RootPanel.get(), formsController, () -> {
            FormContainer currentForm = MainFrame.getCurrentForm();
            if(currentForm != null)
                return currentForm.getForm();
            return null;
        });
    }

    public Widget getView() {
        return tabsPanel;
    }

    public FormContainer openForm(GAsyncFormController asyncFormController, GForm form, GShowFormType showFormType, boolean forbidDuplicate, Event editEvent, EditContext editContext, GFormController formController, WindowHiddenHandler hiddenHandler) {
        GWindowFormType windowType = showFormType.getWindowType();
        FormContainer formContainer = asyncFormController.removeAsyncForm();
        boolean asyncOpened = formContainer != null;

        if(!asyncOpened) {
            FormDockable duplicateForm = getDuplicateForm(form.canonicalName, forbidDuplicate);
            if (duplicateForm != null) {
                selectTab(duplicateForm);
                return null;
            }
        }

        // if form is async opened with different type - close it
        if(asyncOpened && formContainer.getWindowType() != windowType && !formContainer.isAsyncHidden()) {
            formContainer.hide(CancelReason.OTHER);
            asyncOpened = false;
        }

        if (!asyncOpened) {
            asyncFormController.cancelScheduledOpening();
            formContainer = createFormContainer(windowType, showFormType, false, -1, form.canonicalName, form.getCaption(), editEvent, editContext, formController);
        }
        initForm(formContainer, form, hiddenHandler, showFormType.isDialog(), isAutoSized(editContext, windowType));
        if(asyncOpened)
            formContainer.onAsyncInitialized();
        else
            formContainer.show(asyncFormController);

        return formContainer;
    }

    private FormContainer createFormContainer(GWindowFormType windowType, GShowFormType showFormType, boolean async, long editRequestIndex, String formCanonicalName, String formCaption, Event editEvent, EditContext editContext, GFormController formController) {
        switch (windowType) {
            case FLOAT:
                return new ModalForm(this, formCaption, async, editEvent);
            case DOCKED:
                return new FormDockable(this, formCanonicalName, formCaption, async, editEvent);
            case EMBEDDED:
                return new EmbeddedForm(this, editRequestIndex, async, editEvent, editContext, formController);
            case POPUP:
                return new PopupForm(this, editRequestIndex, async, editEvent, editContext, formController);
            case INNER:
                return new InnerForm(this, async, editEvent, showFormType.getInContainerId());
        }
        throw new UnsupportedOperationException();
    }

    public void asyncOpenForm(GAsyncFormController asyncFormController, GAsyncOpenForm openForm, Event editEvent, EditContext editContext, ExecContext execContext, GFormController formController) {
        FormDockable duplicateForm = getDuplicateForm(openForm.canonicalName, openForm.forbidDuplicate);
        if (duplicateForm == null) {
            GWindowFormType windowType = openForm.getWindowType(asyncFormController.canShowDockedModal());
            Scheduler.ScheduledCommand runOpenForm = () -> {
                if(windowType != GWindowFormType.INNER) {
                    FormContainer formContainer = createFormContainer(windowType, null, true, asyncFormController.getEditRequestIndex(), openForm.canonicalName, openForm.caption, editEvent, editContext, formController);
                    formContainer.setContentLoading();
                    formContainer.show(asyncFormController);
                    asyncFormController.putAsyncForm(formContainer);
                }
            };
            // this types because for them size is unknown, so there'll be blinking
            if(isCalculatedSized(editContext, windowType))
                asyncFormController.scheduleOpen(runOpenForm);
            else
                runOpenForm.execute();
        }
    }

    public void asyncCloseForm(GAsyncFormController asyncFormController) {
        FormContainer formContainer = MainFrame.getCurrentForm();
        if(formContainer instanceof FormDockable) {
            asyncFormController.putAsyncClosedForm(new Pair<>(formContainer, forms.indexOf(formContainer)));
            formContainer.queryHide(null);
        }
    }

    private boolean isCalculatedSized(ExecContext execContext, GWindowFormType windowType) {
        return isPreferredSize(execContext, windowType) || isAutoSized(execContext, windowType);
    }

    private boolean isAutoSized(ExecContext execContext, GWindowFormType windowType) {
        return (windowType.isEmbedded() && execContext.getProperty().autoSize) || windowType.isPopup();
    }

    private boolean isPreferredSize(ExecContext execContext, GWindowFormType windowType) {
        return windowType == GWindowFormType.FLOAT;
    }

    private FormDockable getDuplicateForm(String canonicalName, boolean forbidDuplicate) {
        if(forbidDuplicate && MainFrame.forbidDuplicateForms) {
            return findForm(canonicalName);
        }
        return null;
    }

    public void addContextMenuHandler(FormDockable formContainer) {
        formContainer.getTabWidget().addDomHandler(new ContextMenuHandler() {
            @Override
            public void onContextMenu(ContextMenuEvent event) {
                GwtClientUtils.stopPropagation(event);

                PopupDialogPanel popup = new PopupDialogPanel();
                final MenuBar menuBar = new MenuBar(true);
                menuBar.addItem(new MenuItem(messages.closeAllTabs(), () -> {
                    popup.hide();
                    closeAllTabs();
                }));
                GwtClientUtils.showPopupInWindow(popup, menuBar, event.getNativeEvent().getClientX(), event.getNativeEvent().getClientY());
            }
        }, ContextMenuEvent.getType());
    }

    public void initForm(FormContainer formContainer, GForm form, WindowHiddenHandler hiddenHandler, boolean dialog, boolean autoSize) {
        formContainer.initForm(this, form, (asyncFormController, editFormCloseReason) -> {
            Pair<FormContainer, Integer> asyncClosedForm = asyncFormController.removeAsyncClosedForm();
            if(asyncClosedForm == null) {
                formContainer.queryHide(editFormCloseReason);
            }
            hiddenHandler.onHidden();
        }, dialog, autoSize);
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
        return findInList(forms, dockable -> dockable.getCanonicalName() != null && dockable.getCanonicalName().equals(formCanonicalName));
    }

    public void addDockable(FormDockable dockable, Integer index) {
        if(index != null) {
            forms.add(index, dockable);
            formFocusOrder.add(index, null);
        } else {
            forms.add(dockable);
            formFocusOrder.add(null);
        }

        tabsPanel.addTab(dockable.getContentWidget(), index, dockable.getTabWidget());
        assert !isAdding;
        isAdding = true;
        selectTab(dockable);
        assert !isAdding;
    }

    public void removeDockable(FormDockable dockable) {
        int index = forms.indexOf(dockable);
        boolean isTabSelected = forms.get(tabsPanel.getSelectedTab()).equals(dockable);

        if (isTabSelected) {
            assert !isRemoving;
            isRemoving = true;
        }

        tabsPanel.removeTab(index);
        assert !isRemoving; // checking that the active tab is closing

        forms.remove(index);
        formFocusOrder.remove(index);

        ensureTabSelected();
    }
    
    public int getFormsCount() {
        return forms.size();
    }

    private void closeAllTabs() {
        Scheduler.get().scheduleFixedDelay(new Scheduler.RepeatingCommand() {
            private int size = forms.size();
            @Override
            public boolean execute() {
                if (MainFrame.isModalPopup())
                    return true;

                if (size > 0 && size <= forms.size()) { // check if some tab not closed by user while running "closeAllTabs"
                    FormDockable lastTab = forms.get(size - 1);
                    selectTab(lastTab);
                    lastTab.closePressed();
                    size--;
                    return true;
                }

                return false;
            }
        }, 20);
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

    public void resetWindowsLayout() {
        setFullScreenMode(false);
        windowsController.resetLayout();
    }

    public abstract <T extends Result> long syncDispatch(final ExecuteNavigatorAction action, RequestCountingAsyncCallback<ServerResponseResult> callback);

    public abstract <T extends Result> long asyncDispatch(final ExecuteNavigatorAction action, RequestCountingAsyncCallback<ServerResponseResult> callback);

    public abstract GNavigatorActionDispatcher getDispatcher();

    public class ServerResponseCallback extends GwtActionDispatcher.ServerResponseCallback {

        @Override
        protected GwtActionDispatcher getDispatcher() {
            return FormsController.this.getDispatcher();
        }

        public ServerResponseCallback(boolean disableForbidDuplicate) {
            super(disableForbidDuplicate);
        }
    }

    public long executeNavigatorAction(String actionSID, final NativeEvent event, boolean sync) {
        ExecuteNavigatorAction navigatorAction = new ExecuteNavigatorAction(actionSID, 1);
        ServerResponseCallback callback = new ServerResponseCallback(event.getCtrlKey());
        if(sync)
            return syncDispatch(navigatorAction, callback);
        else
            return asyncDispatch(navigatorAction, callback);
    }

    public void executeNotificationAction(String actionSID, int type) {
        executeNotificationAction(actionSID, type, new ServerResponseCallback(false));
    }
    
    public void executeNotificationAction(String actionSID, int type, RequestCountingAsyncCallback<ServerResponseResult> callback) {
        syncDispatch(new ExecuteNavigatorAction(actionSID, type), callback);
    }

    private class EditModeButton extends SimplePanel implements ColorThemeChangeListener {

        public EditModeButton() {
            getElement().setId("modeButton");
            MainFrame.addColorThemeChangeListener(this);
        }

        Timer timer = null;

        @Override
        public void colorThemeChanged() {

            timer = new Timer() {
                @Override
                public void run() {
                    if(editModeButton.isVisible()) {
                        destroyEditModeButton();
                        setupEditModeButton();
                        timer.cancel();
                    }
                }
            };
            timer.scheduleRepeating(100);
        }
    }

}
