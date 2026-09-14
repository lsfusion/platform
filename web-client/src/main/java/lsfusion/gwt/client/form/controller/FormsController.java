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
import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.action.GDestroyFormAction;
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
import lsfusion.gwt.client.form.event.*;
import lsfusion.gwt.client.form.object.table.grid.user.toolbar.view.GToolbarButton;
import lsfusion.gwt.client.form.object.table.view.GToolbarView;
import lsfusion.gwt.client.form.property.async.GAsyncExecutor;
import lsfusion.gwt.client.form.property.async.GAsyncOpenForm;
import lsfusion.gwt.client.form.property.async.GPushAsyncClose;
import lsfusion.gwt.client.form.property.cell.controller.CancelReason;
import lsfusion.gwt.client.form.property.cell.controller.EditContext;
import lsfusion.gwt.client.form.property.cell.controller.ExecContext;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.form.view.ModalForm;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.controller.GNavigatorController;
import lsfusion.gwt.client.navigator.controller.dispatch.GNavigatorActionDispatcher;
import lsfusion.gwt.client.navigator.view.BSMobileNavigatorView;
import lsfusion.gwt.client.navigator.window.GContainerWindowFormType;
import lsfusion.gwt.client.navigator.window.GDockedWindowFormType;
import lsfusion.gwt.client.navigator.window.GModalityShowFormType;
import lsfusion.gwt.client.navigator.window.GShowFormType;
import lsfusion.gwt.client.navigator.window.GWindowFormType;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;

import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static lsfusion.gwt.client.base.GwtClientUtils.*;
import static lsfusion.gwt.client.form.event.GKeyStroke.isAltEvent;
import static lsfusion.gwt.client.form.event.GKeyStroke.isTabEvent;

public abstract class FormsController {
    private final ClientMessages messages = ClientMessages.Instance.get();

    // System.forms, and every WINDOW ... FORMS the application declared, by canonical name: each holds its own
    // forms and draws them its own way. Filled in initWindow, once the navigator has been read - System.forms FIRST,
    // which is the order a search over all of them goes in
    private FormsWindowController main;
    private final Map<String, FormsWindowController> formsWindows = new LinkedHashMap<>();
    // the tabbed view's extra tab widget, built here with its buttons - and built even when a component draws the
    // window and it is never attached: the buttons are written from here as the state behind them changes (a held
    // modifier's force mode, ALT+F11, a server maximize), and those writes cannot become one FormsView notification -
    // a component is told about the chosen edit mode and the full screen mode, but not about a force mode
    private final GToolbarView toolbarView;

    private final List<FormContainer> formContainers = new ArrayList<>();

    private final WindowsController windowsController;

    private int prevModeButton;
    private GToolbarButton editModeButton;

    private GToolbarButton fullScreenButton;
    private boolean fullScreenMode = false;
    
    private GToolbarButton mobileMenuButton;

    public WindowsController getWindowsController() {
        return windowsController;
    }

    public FormsController(WindowsController windowsController) {
        this.windowsController = windowsController;

        toolbarView = new GToolbarView();

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
                                    setEditMode(buttons[index]);
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

        initEditModeTimer();
    }

    private GNavigatorController navigatorController;
    public GNavigatorController getNavigatorController() {
        return navigatorController;
    }
    public void setNavigatorController(GNavigatorController navigatorController) {
        this.navigatorController = navigatorController;
    }

    private final ArrayList<GBindingEvent> bindingEvents = new ArrayList<>();
    private final ArrayList<GFormController.Binding> bindings = new ArrayList<>();

    public void addBindings(GNavigatorElement element, ArrayList<GInputBindingEvent> inputBindingEvents) {
        for(GInputBindingEvent inputBindingEvent : inputBindingEvents) {
            bindingEvents.add(new GBindingEvent(event -> inputBindingEvent.inputEvent.isEvent(event), new GBindingEnv()));
            bindings.add(new GFormController.Binding(null) {
                @Override
                public boolean showing() {
                    return true;
                }
                @Override
                public void exec(Event event) {
                    executeNavigatorAction(element.canonicalName, event, true);
                }
            });
        }
    }

    public void processBinding(EventHandler handler) {
        ProcessBinding.processBinding(handler, false, false, false, bindingEvents, bindings,
                eventTarget -> null,
                (binding, preview) -> true, binding -> true, binding -> true, (bindingEvent, groupObject, equalGroup) -> true,
                (binding, event) -> true, (binding, showing) -> true, (binding, isMouse, panel) -> true,
                (binding, isMouse, isCell) -> true, () -> {});
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
            if (onlyShift && !isGroupChangeMode())
                setForceEditMode(EditMode.GROUPCHANGE);
            if (!onlyShift && isForceGroupChangeMode())
                removeForceEditMode();
        }
        if (altKey != null) {
            boolean onlyAlt = altKey && (ctrlKey == null || !ctrlKey) && (shiftKey == null || !shiftKey);
            pressedAlt = true;
            if (onlyAlt && !isDialogMode())
                setForceEditMode(EditMode.DIALOG);
            if (!onlyAlt && isForceDialogMode())
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

    // the mode the user chose and the one stored between sessions - what the toolbar button's popup sets, and what a
    // component's own chooser sets through controller.setEditMode. The force modes go through selectEditMode instead:
    // they last while a modifier is held, so the view is not told about them
    public void setEditMode(EditMode mode) {
        selectEditMode(mode.getIndex());
        requestViewUpdate();
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
                        // the key-up was lost (a focus change, another window), so the force mode is dropped here.
                        // removeForceEditMode restores the CHOSEN mode, image and title together - resetting to DEFAULT
                        // after it would leave the button showing the chosen mode while editing behaved as default
                        if (isForceLinkMode() || isForceDialogMode() || isForceGroupChangeMode())
                            removeForceEditMode();
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

    public boolean isFullScreenMode() {
        return fullScreenMode;
    }

    public void setFullScreenMode(boolean fullScreenMode) {
        if (fullScreenMode != this.fullScreenMode) {
            windowsController.setFullScreenMode(fullScreenMode);
            this.fullScreenMode = fullScreenMode;
            updateFullScreenButton();
            requestViewUpdate();
        }
    }

    // the windows are only known once the navigator has been read, which is after this controller is built - and no
    // form can be open before that, so the views are made here rather than in the constructor. System.forms is the
    // main one and carries the platform's toolbar; an application's own window is drawn where its POSITION puts it.
    // Returns what draws the window
    public Widget initWindow(GAbstractWindow window) {
        boolean main = window.isSystemForms();
        FormsWindowController formsWindow = new FormsWindowController(this, window, main, main ? toolbarView : null);
        formsWindows.put(window.canonicalName, formsWindow);
        if (main)
            this.main = formsWindow;
        return formsWindow.getView();
    }

    // the window a form goes to: the one the open names, or System.forms. There are two ways to reach System.forms
    // and only one of them is a fallback: a type that is not docked names no window at all - a float, an editor - and
    // a NAME no window answers to, which is the mobile web layout and only it, the one client that does not build the
    // application's windows. HIDE WINDOW is not one of the two: it stops a window being drawn and leaves it here
    public FormsWindowController getFormsWindow(GWindowFormType windowType) {
        if (windowType instanceof GDockedWindowFormType) {
            FormsWindowController formsWindow = formsWindows.get(((GDockedWindowFormType) windowType).window);
            if (formsWindow != null)
                return formsWindow;
        }
        return main;
    }

    public FormsWindowController getFormsWindow(FormDockable dockable) {
        return getFormsWindow(dockable.getWindowType());
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
        return main.getView();
    }

    public static class OpenContext {
        public final Event editEvent;
        public final EditContext editContext;
        public final GFormController formController;
        public final FormDockable contextFormDockable;

        public OpenContext(Event editEvent, EditContext editContext, GFormController formController, FormDockable contextFormDockable) {
            this.editEvent = editEvent;
            this.editContext = editContext;
            this.formController = formController;
            this.contextFormDockable = contextFormDockable;
        }
    }

    public FormContainer openForm(GAsyncFormController asyncFormController, GForm form, GShowFormType showFormType, boolean forbidDuplicate, boolean syncType, String formId, OpenContext context, boolean canShowDockedModal, Consumer<Throwable> onResult) {
        if (showFormType.isDockedModal() && !canShowDockedModal) {
            showFormType = GModalityShowFormType.MODAL;
        }

        FormContainer formContainer = asyncFormController.removeAsyncForm();
        boolean asyncOpened = formContainer != null;

        FormDockable contextFormDockable = context.contextFormDockable;
        GFormController formController = context.formController;

        if(!asyncOpened) {
            FormDockable duplicateForm = getDuplicateForm(showFormType.getWindowType(), form.canonicalName, forbidDuplicate);
            if (duplicateForm != null) {
                setCurrentForm(duplicateForm);
                return null;
            }
        }

        // if form is async opened with different type - close it
        GWindowFormType windowType = showFormType.getWindowType();
        if(asyncOpened && !nullEquals(formContainer.getWindowType(), windowType) && !formContainer.isAsyncHidden()) { // by value: a docked type names its window
            formContainer.hide(CancelReason.HIDE);
            asyncOpened = false;
        }

        if (!asyncOpened) {
            asyncFormController.cancelScheduledOpening();
            formContainer = createFormContainer(windowType, false, syncType, -1, form.canonicalName, context.editEvent, context.editContext, formController);
        }

        int dispatchPriority = (formController != null ? formController.getDispatchPriority() : 0);
        if(showFormType.isWindow()) {
            assert showFormType.isModal();
            // we'll increase the priority for the cascade window forms, because they can block ui (and more important)
            // and we'll prioritize less requestIndexes calls (see ModalForm show order)
            dispatchPriority += RemoteDispatchAsync.windowDeepStep -
                    (int)asyncFormController.getEditRequestIndex() * RemoteDispatchAsync.requestIndexDeepStep;
        }

        if (contextFormDockable != null) {
            contextFormDockable.block();
            contextFormDockable.setBlockingForm((FormDockable) formContainer);
            getFormsWindow(contextFormDockable).formsChanged(); // the mask is a projected state, so a component view has to be told the transition happened
        }

        boolean isDialog = showFormType.isDialog();

        FormContainer fFormContainer = formContainer; GShowFormType fShowFormType = showFormType; int fDispatchPriority = dispatchPriority;
        Result<WindowHiddenHandler> recursionHiddenHandler = new Result<>();
        WindowHiddenHandler hiddenHandler = (lookAhead, hideAsyncFormController, editFormCloseReason) -> {
            // checking if there is other form opening -> using the same container
            GForm recreateForm = null;
            GAction action;
            while((action = lookAhead.next()) != null) {
                if(action instanceof GDestroyFormAction)
                    continue;
                if(action instanceof GFormAction) {
                    GFormAction formAction = (GFormAction) action;
                    GShowFormType newShowFormType = formAction.showFormType;

                    if (newShowFormType.isDockedModal() && !canShowDockedModal) {
                        newShowFormType = GModalityShowFormType.MODAL;
                    }

                    // the same form open type
                    if(newShowFormType.equals(fShowFormType) && formAction.syncType == syncType && GwtClientUtils.nullEquals(formAction.formId, formId)) {
                        recreateForm = formAction.form;
                        lookAhead.drop();
                    }
                }

                break;
            }
            if(recreateForm != null) {
                fFormContainer.initForm(FormsController.this, recursionHiddenHandler.result, recreateForm, isDialog, fDispatchPriority, formId); // the container is reused, but a NEW form arrived in it

                if(fFormContainer instanceof ModalForm) // it's a hack but for now it's the best place
                    ((ModalForm)fFormContainer).initPreferredSize();
                return;
            }

            // closing container
            Pair<FormDockable, Integer> asyncClosedForm = hideAsyncFormController.removeAsyncClosedForm();
            if(asyncClosedForm == null)
                fFormContainer.queryHide(editFormCloseReason);
            removeFormContainer(fFormContainer);

            // the form is gone for good, so a docked-modal child of ITS OWN stops blocking an opener there no longer
            // is - the flag is what keeps an arrival from displacing that child, and nothing else would ever clear it,
            // since the pair is unwound by the CHILD's handler below. Only here, where the server has confirmed the
            // close: an optimistic hide can still be refused, and the same opener comes back
            if (fFormContainer instanceof FormDockable)
                ((FormDockable) fFormContainer).setBlockingForm(null);

            if (contextFormDockable != null) {
                contextFormDockable.setBlockingForm(null);
                contextFormDockable.unblock();
                getFormsWindow(contextFormDockable).formsChanged();
            }

            // back to the form this one was opened from - if its window still holds it. CLOSE FORM can close a form
            // while a docked-modal child of its own is open, and then there is nobody to go back to; an optimistic
            // close takes one out of its window too, until the server answers - and it comes back, masked, if the
            // close is refused. Either way the window picks what it draws instead. The opener is unblocked above
            // in every case: the flag is about the pair, not about where the form is
            if (contextFormDockable != null && getFormsWindow(contextFormDockable).indexOf(contextFormDockable) >= 0)
                setCurrentForm(contextFormDockable);
            else if (fFormContainer instanceof FormDockable)
                getFormsWindow((FormDockable) fFormContainer).ensureCurrentForm();

            onResult.accept(null);
        };
        recursionHiddenHandler.set(hiddenHandler);

        formContainer.initForm(this, hiddenHandler, form, isDialog, dispatchPriority, formId);

        if(asyncOpened)
            formContainer.onAsyncInitialized();
        else
            formContainer.show(asyncFormController);

        return formContainer;
    }

    private FormContainer createFormContainer(GWindowFormType windowType, boolean async, boolean syncType, long editRequestIndex, String formCanonicalName, Event editEvent, EditContext editContext, GFormController formController) {
        FormContainer formContainer;
        if(windowType instanceof GContainerWindowFormType) {
            formContainer = new ContainerForm(this, formController, async, editEvent, ((GContainerWindowFormType) windowType));
        } else if(windowType.isFloat()) {
            formContainer =  new ModalForm(this, formController, async, syncType, editEvent, editContext != null ? editContext.getPopupOwner() : (formController != null ? formController.getPopupOwner() : PopupOwner.GLOBAL));
        } else if(windowType.isDocked()) {
            formContainer =  new FormDockable(this, formController, formCanonicalName, async, editEvent, windowType);
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
        GWindowFormType windowType = openForm.getWindowType(asyncFormController.canShowDockedModal());
        FormDockable duplicateForm = getDuplicateForm(windowType, openForm.canonicalName, openForm.forbidDuplicate);
        if (duplicateForm == null) {
            Scheduler.ScheduledCommand runOpenForm = () -> {
                FormContainer formContainer = createFormContainer(windowType, true, true, asyncFormController.getEditRequestIndex(), openForm.canonicalName, editEvent, editContext, formController);

                formContainer.requestedCaption = openForm.caption;

                Widget captionWidget = formContainer.getCaptionWidget();
                if(captionWidget != null)
                    BaseImage.initImageText(captionWidget, openForm.caption, openForm.appImage, ImageHtmlOrTextType.FORM);

                formContainer.setContentLoading(asyncFormController);
                formContainer.show(asyncFormController);
                asyncFormController.putAsyncForm(formContainer);
            };
            // this types because for them size is unknown, so there'll be blinking
            if(isAutoSized(execContext, windowType)) // execContext (always non-null); editContext is null on the controller path
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
            asyncFormController.putAsyncClosedForm(new Pair<>((FormDockable) formContainer, getFormsWindow((FormDockable) formContainer).indexOf((FormDockable) formContainer)));
            formContainer.queryHide(CancelReason.HIDE);
        }
    }

    private boolean isAutoSized(ExecContext execContext, GWindowFormType windowType) {
        return (windowType.isEmbedded() && execContext.getProperty().hasAutoSize())  || windowType instanceof GContainerWindowFormType || windowType.isPopup() || windowType.isFloat();
    }

    // a duplicate is looked for in the window the form would open in - after the fallback, so that on the mobile layout,
    // where every docked form goes to System.forms, it is that window's forms that are checked
    private FormDockable getDuplicateForm(GWindowFormType windowType, String canonicalName, boolean forbidDuplicate) {
        if(forbidDuplicate && MainFrame.forbidDuplicateForms) {
            return getFormsWindow(windowType).findForm(canonicalName);
        }
        return null;
    }

    // something the forms view shows from its projection changed - a caption, an image, the blocked state.
    // Applying one set of remote changes can touch several of them, and a component view rebuilds its whole
    // projection each time, so the requests are collapsed into one pass
    private boolean viewUpdateRequested;
    public void requestViewUpdate() {
        if (viewUpdateRequested)
            return;

        viewUpdateRequested = true;
        Scheduler.get().scheduleFinally(() -> {
            viewUpdateRequested = false;
            for (FormsWindowController window : formsWindows.values())
                window.formsChanged();
        });
    }

    public void setCurrentForm(FormDockable dockable) {
        getFormsWindow(dockable).setCurrentForm(dockable);
    }

    public void setCurrentForm(String formCanonicalName) {
        FormDockable form = findForm(formCanonicalName);
        if(form != null)
            setCurrentForm(form);
    }

    // CLOSE FORM: every form with that id, each through the ordinary close - which is a request, so a form with unsaved
    // changes asks the user and may stay. That is why nothing is dropped from formContainers here: a container leaves
    // that list when the form is actually hidden, and dropping it on the request alone put a form that refused to close
    // out of reach of the next CLOSE FORM. Over a copy, since a close may take a container out of the list at once
    public void closeForm(String formId) {
        for (FormContainer formContainer : new ArrayList<>(formContainers)) {
            GFormController form = formContainer.getForm();
            if(form != null && formId.equals(form.formId))
                formContainer.closePressed();
        }
    }

    // in any window, System.forms first: ACTIVATE names a form, not a window
    public FormDockable findForm(String formCanonicalName) {
        for (FormsWindowController window : formsWindows.values()) {
            FormDockable form = window.findForm(formCanonicalName);
            if (form != null)
                return form;
        }
        return null;
    }

    public void addDockable(FormDockable dockable, Integer index) {
        getFormsWindow(dockable).addDockable(dockable, index);
    }

    public void removeDockable(FormDockable dockable) {
        getFormsWindow(dockable).removeDockable(dockable);
    }

    // the forms in System.forms: what the startup's "is anything open" check means
    public int getFormsCount() {
        return main.getFormsCount();
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
    // there is no event when the element is activated from code rather than by a click, and then no modifier is held
    public long executeNavigatorAction(String actionSID, final NativeEvent event, boolean sync) {
        return executeNavigatorAction(actionSID, event != null && event.getCtrlKey(), sync, 1, null);
    }
    public void executeNotificationAction(Integer id, String result, Runnable onRequestFinished) {
        FormContainer currentForm = onRequestFinished == null ? MainFrame.getCurrentForm() : null;
        GFormController form = currentForm != null ? currentForm.getForm() : null;
        String notification = id + (result != null ? ";" + result : ""); // should match RemoteNavigator.runNotification
        if (form != null) {
            try {
                form.executeNotificationAction(notification);
            } catch (IOException e) {
                GWT.log(e.getMessage());
            }
            executeVoidAction(-1);
        } else
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
        syncDispatch(new VoidNavigatorAction(waitRequestIndex), new ServerResponseCallback(false));
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
