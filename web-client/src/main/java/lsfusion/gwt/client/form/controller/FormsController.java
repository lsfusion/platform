package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.ClientMessages;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.RemoteDispatchAsync;
import lsfusion.gwt.client.action.GFormAction;
import lsfusion.gwt.client.action.GHideFormAction;
import lsfusion.gwt.client.base.*;
import lsfusion.gwt.client.base.result.VoidResult;
import lsfusion.gwt.client.base.view.*;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.action.RequestCountingAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.ExecuteNavigatorAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorRequestAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorRequestCountingAction;
import lsfusion.gwt.client.controller.remote.action.navigator.VoidNavigatorAction;
import lsfusion.gwt.client.form.ContainerForm;
import lsfusion.gwt.client.form.EmbeddedForm;
import lsfusion.gwt.client.form.PopupForm;
import lsfusion.gwt.client.form.design.view.flex.FlexTabbedPanel;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;
import lsfusion.gwt.client.form.property.async.GAsyncExecutor;
import lsfusion.gwt.client.form.property.async.GAsyncOpenForm;
import lsfusion.gwt.client.form.property.async.GPushAsyncClose;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.ExecContext;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.form.view.ModalForm;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.controller.dispatch.GNavigatorActionDispatcher;
import lsfusion.gwt.client.navigator.view.BSMobileNavigatorView;
import lsfusion.gwt.client.navigator.window.GContainerWindowFormType;
import lsfusion.gwt.client.navigator.window.GShowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Supplier;

import static lsfusion.gwt.client.base.GwtClientUtils.findInList;
import static lsfusion.gwt.client.form.event.GKeyStroke.isAltEvent;
import static lsfusion.gwt.client.form.event.GKeyStroke.isTabEvent;

public abstract class FormsController {
    private final ClientMessages messages = ClientMessages.Instance.get();

    private final ResizableSimplePanel container; // used for / in the setFullScreenMode, and it is assumed that it is returned in getView

    private final Panel tabsPanel;

    private final List<FormContainer> formContainers = new ArrayList<>();

    private final List<FormDockable> forms = new ArrayList<>();
    private final List<Integer> formFocusOrder = new ArrayList<>();
    private int focusOrderCount;

    private final WindowsController windowsController;

    private int prevModeButton;
    private GToolbarButton editModeButton;

    private GToolbarButton fullScreenButton;
    private boolean fullScreenMode = false;
    
    private GToolbarButton mobileMenuButton;

    public static class Panel extends FlexTabbedPanel {
        public Panel(Widget extraTabWidget, boolean end) {
            super(extraTabWidget, end);
        }
    }

    public FormsController(WindowsController windowsController) {
        this.windowsController = windowsController;

        GToolbarView toolbarView = new GToolbarView();

        int editMode = windowsController.restoreEditMode();
        editModeButton = new GToolbarButton(EditMode.getImage(editMode)) {
            @Override
            public ClickHandler getClickHandler() {
                return event -> {
                    final Result<JavaScriptObject> popup = new Result<>();
                    FlexPanel panel = new FlexPanel(true);
                    GwtClientUtils.addClassName(panel.getElement(), "btn-toolbar");
                    EditMode[] buttons = EditMode.values();
                    for(int i = 0; i < buttons.length; i++) {
                        int index = i;
                        panel.add(new GToolbarButton(EditMode.getImage(buttons[i].getIndex())) {
                            @Override
                            public ClickHandler getClickHandler() {
                                return event -> {
                                    selectEditMode(index);
                                    GwtClientUtils.hideAndDestroyTippyPopup(popup.result);
                                };
                            }
                        });
                    }

                    popup.result = GwtClientUtils.showTippyPopup(editModeButton, panel);
                };
            }
        };
        updateEditMode(EditMode.getMode(editMode), null);

        toolbarView.addComponent(editModeButton);

        if (!MainFrame.mobile) {
            fullScreenButton = new GToolbarButton(StaticImage.MAXIMIZE) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> switchFullScreenMode();
                }
            };
            toolbarView.addComponent(fullScreenButton);
            updateFullScreenButton();
        } else {
            mobileMenuButton = new GToolbarButton(StaticImage.HAMBURGER) {
                @Override
                public ClickHandler getClickHandler() {
                    return event -> MainFrame.openNavigatorMenu();
                }

                @Override
                protected boolean ignoreFocusLastBlurredElement() {
                    //there is some bug with focusLastBlurredElement in bootstrap theme
                    //after showing menu resize event is called, this event causes hiding menu
                    //so we disable focusLastBlurredElement for bootstrap mobile menu button
                    return MainFrame.useBootstrap;
                }
            };
            mobileMenuButton.getElement().setAttribute("data-bs-toggle", "offcanvas");
            mobileMenuButton.getElement().setAttribute("data-bs-target", "#" + BSMobileNavigatorView.OFFCANVAS_ID);
            toolbarView.addComponent(mobileMenuButton);
        }

        tabsPanel = new Panel(toolbarView, MainFrame.mobile);

        // unselected (but not removed)
        tabsPanel.setBeforeSelectionHandler(index -> {
            int selectedTab = tabsPanel.getSelectedTab();
            if(selectedTab >= 0) {
                forms.get(selectedTab).onBlur(isRemoving);
                isRemoving = false;
            }
        });
        tabsPanel.setSelectionHandler(index -> {
            if(index >= 0) {
                forms.get(index).onFocus(isAdding);
                formFocusOrder.set(index, focusOrderCount++);
                isAdding = false;
            }
        });

        GwtClientUtils.addClassName(tabsPanel, "forms-container");

        container = new ResizableSimplePanel();
        container.setPercentMain(tabsPanel);
        GwtClientUtils.addClassName(container, "forms-container-window");

        initEditModeTimer();
    }

    public void onServerInvocationResponse(ServerResponseResult response, GAsyncFormController asyncFormController) {
        onServerInvocation(asyncFormController,
                () -> Arrays.stream(response.actions).noneMatch(a -> a instanceof GFormAction),
                () -> Arrays.stream(response.actions).noneMatch(a -> a instanceof GHideFormAction));
    }

    public void onServerInvocationFailed(GAsyncFormController asyncFormController) {
        onServerInvocation(asyncFormController, () -> true, () -> true);
    }

    public void onServerInvocation(GAsyncFormController asyncFormController,
                                   Supplier<Boolean> checkOpenForm, Supplier<Boolean> checkHideForm) {
        if (asyncFormController.onServerInvocationOpenResponse()) {
            if (checkOpenForm.get()) {
                asyncFormController.removeAsyncForm().queryHide(CancelReason.HIDE);
            }
        }
        if (asyncFormController.onServerInvocationCloseResponse()) {
            if (checkHideForm.get()) {
                Pair<FormDockable, Integer> asyncClosedForm = asyncFormController.removeAsyncClosedForm();
                asyncClosedForm.first.showDockable(asyncClosedForm.second);
            }
        }
    }

    private boolean isRemoving = false;
    private boolean isAdding = false;

    public void checkEditModeEvents(NativeEvent event) {
        Boolean ctrlKey = eventGetCtrlKey(event);
        Boolean shiftKey = eventGetShiftKey(event);
        Boolean altKey = eventGetAltKey(event);
        boolean tab = isTabEvent(event);
        if(ctrlKey != null) {
            boolean onlyCtrl = ctrlKey && (shiftKey == null || !shiftKey) && (altKey == null || !altKey);
            pressedCtrl = onlyCtrl;
            if (onlyCtrl && !isLinkMode())
                setForceEditMode(EditMode.LINK);
            if (!onlyCtrl && isForceLinkMode())
                removeForceEditMode();
        }
        if(shiftKey != null) {
            boolean onlyShift = shiftKey && (ctrlKey == null || !ctrlKey) && (altKey == null || !altKey) && !tab;
            pressedShift = onlyShift;
            if (onlyShift && !isDialogMode())
                setForceEditMode(EditMode.DIALOG);
            if (!onlyShift && isForceDialogMode())
                removeForceEditMode();
        }
        if (altKey != null) {
            boolean onlyAlt = altKey && (ctrlKey == null || !ctrlKey) && (shiftKey == null || !shiftKey);
            pressedAlt = true;
            if (onlyAlt && !isGroupChangeMode())
                setForceEditMode(EditMode.GROUPCHANGE);
            if (!onlyAlt && isForceGroupChangeMode())
                removeForceEditMode();

            if(isAltEvent(event)) // we want to prevent moving focus in the browser to the menu bar
                event.preventDefault();
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

    public static boolean isForceGroupChangeMode() {
        return forceEditMode == EditMode.GROUPCHANGE;
    }
    public static boolean isGroupChangeMode() {
        return editMode == EditMode.GROUPCHANGE;
    }

    public static int getEditModeIndex() {
        return forceEditMode != null ? prevEditMode.getIndex() : editMode.getIndex();
    }

    private int selectEditModeButton(int i) {
        int prevModeButton = editMode.getIndex();
        selectEditMode(i);
        return prevModeButton;
    }

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
        editModeButton.changeImage(EditMode.getImage(mode));
        updateEditMode(EditMode.getMode(mode), null);
    }

    private void updateEditMode(EditMode mode, EditMode forceMode) {
        updateForceLinkModeStyles(mode, forceMode == EditMode.LINK);
        editMode = mode;
        forceEditMode = forceMode;
        editModeButton.setTitle(mode.getTitle(messages));
    }

    private void updateForceLinkModeStyles(EditMode editMode, boolean forceLinkMode) {
        boolean linkMode = editMode == EditMode.LINK;
        if(!isForceLinkMode() && forceLinkMode) {
            scheduleLinkModeStylesTimer(() -> setLinkModeClassName(linkMode));
        } else {
            cancelLinkModeStylesTimer();
            setLinkModeClassName(linkMode);
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

    private void setLinkModeClassName(boolean linkMode) {
        GwtClientUtils.setGlobalClassName(linkMode, "linkMode");
    }

    private void updateFormsNotEmptyClassName() {
        GwtClientUtils.setGlobalClassName(!forms.isEmpty(), "forms-container-not-empty");
    }

    private static Timer editModeTimer;
    public static boolean pressedCtrl = false;
    public static boolean pressedShift = false;
    public static boolean pressedAlt = false;
    private void initEditModeTimer() {
        if(editModeTimer == null) {
            editModeTimer = new Timer() {
                @Override
                public void run() {
                    if (pressedCtrl) {
                        pressedCtrl = false;
                    } else if(pressedShift) {
                        pressedShift = false;
                    } else if(pressedAlt) {
                        pressedAlt = false;
                    } else {
                        if (isForceLinkMode() || isForceDialogMode() || isForceGroupChangeMode()) {
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
        fullScreenButton.changeImage(fullScreenMode ? StaticImage.MINIMIZE : StaticImage.MAXIMIZE);
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

    public void initRoot() {
        GFormController.initKeyEventHandler(RootPanel.get(), this, () -> {
            FormContainer currentForm = MainFrame.getCurrentForm();
            if(currentForm != null)
                return currentForm.getForm();
            return null;
        });
    }

    public Widget getView() {
        return container;
    }

    public FormContainer openForm(GAsyncFormController asyncFormController, GForm form, GShowFormType showFormType, boolean forbidDuplicate, Event editEvent, EditContext editContext, GFormController formController, WindowHiddenHandler hiddenHandler, String formId) {
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
        GWindowFormType windowType = showFormType.getWindowType();
        if(asyncOpened && formContainer.getWindowType() != windowType && !formContainer.isAsyncHidden()) {
            formContainer.hide(CancelReason.HIDE);
            asyncOpened = false;
        }

        if (!asyncOpened) {
            asyncFormController.cancelScheduledOpening();
            formContainer = createFormContainer(windowType, false, -1, form.canonicalName, editEvent, editContext, formController);
        }

        int dispatchPriority = (formController != null ? formController.getDispatchPriority() : 0);
        if(showFormType.isWindow()) {
            assert showFormType.isModal();
            // we'll increase the priority for the cascade window forms, because they can block ui (and more important)
            // and we'll prioritize less requestIndexes calls (see ModalForm show order)
            dispatchPriority += RemoteDispatchAsync.windowDeepStep -
                    (int)asyncFormController.getEditRequestIndex() * RemoteDispatchAsync.requestIndexDeepStep;
        }

        initForm(formContainer, form, hiddenHandler, showFormType.isDialog(), dispatchPriority, formId);

        if(asyncOpened)
            formContainer.onAsyncInitialized();
        else
            formContainer.show(asyncFormController);

        return formContainer;
    }

    private FormContainer createFormContainer(GWindowFormType windowType, boolean async, long editRequestIndex, String formCanonicalName, Event editEvent, EditContext editContext, GFormController formController) {
        FormContainer formContainer;
        if(windowType instanceof GContainerWindowFormType) {
            formContainer = new ContainerForm(this, formController, async, editEvent, ((GContainerWindowFormType) windowType));
        } else if(windowType.isFloat()) {
            formContainer =  new ModalForm(this, formController, async, editEvent, editContext != null ? editContext.getPopupOwner() : (formController != null ? formController.getPopupOwner() : PopupOwner.GLOBAL));
        } else if(windowType.isDocked()) {
            formContainer =  new FormDockable(this, formController, formCanonicalName, async, editEvent);
        } else if(windowType.isEmbedded()) {
            formContainer =  new EmbeddedForm(this, formController, editRequestIndex, async, editEvent, editContext);
        } else if(windowType.isPopup()) {
            formContainer =  new PopupForm(this, formController, editRequestIndex, async, editEvent, editContext);
        } else {
            throw new UnsupportedOperationException();
        }

        formContainers.add(formContainer);

        return formContainer;
    }

    public void asyncOpenForm(GAsyncFormController asyncFormController, GAsyncOpenForm openForm, Event editEvent, EditContext editContext, ExecContext execContext, GFormController formController) {
        FormDockable duplicateForm = getDuplicateForm(openForm.canonicalName, openForm.forbidDuplicate);
        if (duplicateForm == null) {
            GWindowFormType windowType = openForm.getWindowType(asyncFormController.canShowDockedModal());
            Scheduler.ScheduledCommand runOpenForm = () -> {
                FormContainer formContainer = createFormContainer(windowType, true, asyncFormController.getEditRequestIndex(), openForm.canonicalName, editEvent, editContext, formController);

                Widget captionWidget = formContainer.getCaptionWidget();
                if(captionWidget != null)
                    BaseImage.initImageText(captionWidget, openForm.caption, openForm.appImage, ImageHtmlOrTextType.FORM);

                formContainer.setContentLoading(asyncFormController.getEditRequestIndex());
                formContainer.show(asyncFormController);
                asyncFormController.putAsyncForm(formContainer);
            };
            // this types because for them size is unknown, so there'll be blinking
            if(isAutoSized(editContext, windowType))
                asyncFormController.scheduleOpen(() -> {
                    if(formController == null || formController.isVisible()) // form can be hidden before the task will be executed
                        runOpenForm.execute();
                });
            else
                runOpenForm.execute();
        }
    }

    public void asyncCloseForm(GAsyncExecutor asyncExecutor, FormContainer formContainer) {
        asyncCloseForm(asyncExecutor.execute(new GPushAsyncClose()), formContainer);
    }

    public void asyncCloseForm(GAsyncFormController asyncFormController, FormContainer formContainer) {
        if(formContainer instanceof FormDockable) {
            asyncFormController.putAsyncClosedForm(new Pair<>((FormDockable) formContainer, forms.indexOf(formContainer)));
            formContainer.queryHide(CancelReason.HIDE);
        }
    }

    private boolean isAutoSized(ExecContext execContext, GWindowFormType windowType) {
        return (windowType.isEmbedded() && execContext.getProperty().hasAutoSize())  || windowType instanceof GContainerWindowFormType || windowType.isPopup() || windowType.isFloat();
    }

    private FormDockable getDuplicateForm(String canonicalName, boolean forbidDuplicate) {
        if(forbidDuplicate && MainFrame.forbidDuplicateForms) {
            return findForm(canonicalName);
        }
        return null;
    }

    public void initForm(FormContainer formContainer, GForm form, WindowHiddenHandler hiddenHandler, boolean dialog, int dispatchPriority, String formId) {
        formContainer.initForm(this, form, (asyncFormController, editFormCloseReason) -> {
            Pair<FormDockable, Integer> asyncClosedForm = asyncFormController.removeAsyncClosedForm();
            if(asyncClosedForm == null) {
                formContainer.queryHide(editFormCloseReason);
            }
            hiddenHandler.onHidden();
        }, dialog, dispatchPriority, formId);
    }

    public void selectTab(FormDockable dockable) {
        tabsPanel.selectTab(forms.indexOf(dockable));
    }

    public void selectTab(String formCanonicalName) {
        FormDockable form = findForm(formCanonicalName);
        if(form != null)
            selectTab(form);
    }

    public void closeForm(String formId) {
        ListIterator<FormContainer> iterator = formContainers.listIterator();
        while(iterator.hasNext()){
            FormContainer formContainer = iterator.next();
            if(formId.equals(formContainer.formId)) {
                formContainer.closePressed();
                iterator.remove();
            }
        }
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

        updateFormsNotEmptyClassName();

        FlexPanel contentWidget = dockable.getContentWidget();

        FlexPanel header = new FlexPanel();
        header.addFillShrink(dockable.getTabWidget());
        header.add(dockable.getCloseButton(), GFlexAlignment.CENTER);

        tabsPanel.addTab(contentWidget, index, header);

        FlexPanel.makeShadowOnScroll(tabsPanel, tabsPanel.getTabBar(), contentWidget, tabsPanel.tabEnd);

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

        updateFormsNotEmptyClassName();
        
        if (forms.isEmpty()) {
            MainFrame.openNavigatorMenu();
        }

        ensureTabSelected();
    }
    
    public int getFormsCount() {
        return forms.size();
    }

    public void closeAllTabs() {
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

    public abstract <T extends net.customware.gwt.dispatch.shared.Result> long syncDispatch(final NavigatorRequestAction<T> action, RequestCountingAsyncCallback<T> callback);

    public abstract long asyncDispatch(final ExecuteNavigatorAction action, RequestCountingAsyncCallback<ServerResponseResult> callback);

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

    public void executeAction(String actionSID, Runnable onRequestFinished) {
        executeNavigatorAction(actionSID, false, true, 0, onRequestFinished);
    }
    public long executeNavigatorAction(String actionSID, final NativeEvent event, boolean sync) {
        return executeNavigatorAction(actionSID, event.getCtrlKey(), sync, 1, null);
    }
    public void executeNotificationAction(Integer id, String result, Runnable onRequestFinished) {
        FormContainer currentForm = onRequestFinished == null ? MainFrame.getCurrentForm() : null;
        GFormController form = currentForm != null ? currentForm.getForm() : null;
        String notification = id + (result != null ? ";" + result : ""); // should match RemoteNavigator.runNotification
        if (form != null)
            try {
                form.executeNotificationAction(notification);
            } catch (IOException e) {
                GWT.log(e.getMessage());
            }
        else
            executeNavigatorAction(notification, false, true, 2, null);
    }
    public long executeNavigatorAction(String actionSID, boolean disableForbidDuplicate, boolean sync, int type, Runnable onRequestFinished) {
        ExecuteNavigatorAction navigatorAction = new ExecuteNavigatorAction(actionSID, type);
        ServerResponseCallback callback = new ServerResponseCallback(disableForbidDuplicate) {
            @Override
            protected Runnable getOnRequestFinished() {
                if(onRequestFinished != null)
                    return onRequestFinished;

                return super.getOnRequestFinished();
            }
        };
        if(sync)
            return syncDispatch(navigatorAction, callback);
        else
            return asyncDispatch(navigatorAction, callback);
    }

    public void executeVoidAction(long waitRequestIndex) {
        executeSystemAction(new VoidNavigatorAction(waitRequestIndex));
    }
    public void executeSystemAction(NavigatorRequestCountingAction<VoidResult> systemAction) {
        syncDispatch(systemAction, new SimpleRequestCallback<VoidResult>() {
            protected void onSuccess(VoidResult result) {
            }

            @Override
            public PopupOwner getPopupOwner() {
                return PopupOwner.GLOBAL;
            }
        });
    }

    public void removeFormContainer(FormContainer formContainer) {
        formContainers.remove(formContainer);
    }
}
