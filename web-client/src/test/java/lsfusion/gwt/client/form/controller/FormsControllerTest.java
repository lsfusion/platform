package lsfusion.gwt.client.form.controller;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.GForm;
import lsfusion.gwt.client.GFormChangesTest;
import lsfusion.gwt.client.action.GAction;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.controller.dispatch.GwtActionDispatcher;
import lsfusion.gwt.client.controller.remote.action.RequestCountingAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.ExecuteNavigatorAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorRequestAction;
import lsfusion.gwt.client.form.property.async.GAsyncOpenForm;
import lsfusion.gwt.client.form.view.FormContainer;
import lsfusion.gwt.client.form.view.FormDockable;
import lsfusion.gwt.client.navigator.controller.GAsyncFormController;
import lsfusion.gwt.client.navigator.controller.dispatch.GNavigatorActionDispatcher;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GDockedShowFormType;
import lsfusion.gwt.client.navigator.window.GModalityWindowFormType;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import lsfusion.gwt.client.view.MainFrame;
import net.customware.gwt.dispatch.shared.Result;

import java.util.Collections;
import java.util.List;

// the forms the forms controller holds: a predicted open puts a placeholder up before the answer comes, and the form
// the answer brings arrives in it - or, when none comes for it, the placeholder goes. No server: an open's request is
// played by the test, and System.forms is never on the page, so a form arriving there takes no keyboard and sends nothing
public class FormsControllerTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    // a placeholder takes the keyboard as it is shown and hands it back as it is hidden, so it starts from none
    private FormContainer currentForm;

    @Override protected void gwtSetUp() {
        JsniTestSupport.installMapDelete();
        JsniTestSupport.installResizeObserver();
        JsniTestSupport.installPageSetup(); // the edit modes' and the loading placeholder's images
        GFormControllerTest.installPageHelpers(); // the toolbar's buttons, and jQuery UI for a floating window
        currentForm = MainFrame.getAssertCurrentForm();
        MainFrame.setCurrentForm(null);
    }
    @Override protected void gwtTearDown() {
        MainFrame.setCurrentForm(currentForm);
    }

    // an open predicted to float and answered docked - the action took another branch: the placeholder is hidden, the
    // form gets a container of its own, and that one alone is held
    public void testAPlaceholderOfAnotherWindowTypeIsHeldNoMore() {
        FormsController formsController = formsController();
        Request request = new Request();
        formsController.asyncOpenForm(request, floatingOpen(), null, null, null, null);
        FormContainer placeholder = request.asyncForm;
        assertEquals(Collections.singletonList(placeholder), held(formsController));

        GForm form = GFormControllerTest.form("docked");
        form.initialFormChanges = GFormChangesTest.noChanges();
        FormContainer arrived = formsController.openForm(request, form, new GDockedShowFormType("System.forms", false), null, false, null,
                new FormsController.OpenContext(null, null, null, null), false, failure -> { });
        assertNotSame(placeholder, arrived);
        assertEquals(Collections.singletonList(arrived), held(formsController));
    }

    // ... and one the answer brings no form for: the placeholder is hidden, and nothing is held
    public void testAPlaceholderAnsweredWithNoFormIsHeldNoMore() {
        FormsController formsController = formsController();
        Request request = new Request();
        formsController.asyncOpenForm(request, floatingOpen(), null, null, null, null);
        assertEquals(Collections.singletonList(request.asyncForm), held(formsController));

        formsController.onServerInvocationResponse(new ServerResponseResult(new GAction[0], request.getEditRequestIndex(), false), request);
        assertEquals(Collections.emptyList(), held(formsController));
    }

    // every form the controller holds: what an address that names nothing finds. No statement writes one - an empty
    // address does not compile -, and none that names something finds a floating placeholder, which has no form yet
    private static List<FormContainer> held(FormsController formsController) {
        return formsController.findForms(null, null, null);
    }

    // a floating form waited for, as the server predicts the open
    private static GAsyncOpenForm floatingOpen() {
        return new GAsyncOpenForm("FormsControllerTest.floating", "Floating", null, null, null, true, GModalityWindowFormType.FLOAT);
    }

    // with System.forms, drawn as tabs and never put on the page; nothing it sends goes anywhere
    private static FormsController formsController() {
        FormsController formsController = new FormsController(new WindowsController() {
            public Widget getWindowView(GAbstractWindow window) { return null; }
        }) {
            public <T extends Result> long syncDispatch(NavigatorRequestAction<T> action, RequestCountingAsyncCallback<T> callback) { return -1; }
            public long asyncDispatch(ExecuteNavigatorAction action, RequestCountingAsyncCallback<ServerResponseResult> callback) { return -1; }
            public GNavigatorActionDispatcher getDispatcher() { return null; }
        };
        GAbstractWindow forms = new GAbstractWindow();
        forms.canonicalName = "System.forms";
        formsController.initWindow(forms);
        return formsController;
    }

    // the open of one request, as the dispatcher that sent it keeps it (GwtActionDispatcher.getAsyncFormController),
    // its placeholder put up at once - the answer late
    private static class Request implements GAsyncFormController {
        FormContainer asyncForm;

        public FormContainer removeAsyncForm() {
            FormContainer removed = asyncForm;
            asyncForm = null;
            return removed;
        }
        public void putAsyncForm(FormContainer container) { asyncForm = container; }
        public Pair<FormDockable, Integer> removeAsyncClosedForm() { return null; }
        public void putAsyncClosedForm(Pair<FormDockable, Integer> container) { }
        public boolean onServerInvocationOpenResponse() { return asyncForm != null; }
        public boolean onServerInvocationCloseResponse() { return false; }
        public boolean canShowDockedModal() { return false; }
        public long getEditRequestIndex() { return 1; }
        public GwtActionDispatcher getDispatcher() { return null; }
        public void scheduleOpen(Scheduler.ScheduledCommand command) { command.execute(); }
        public void cancelScheduledOpening() { }
    }
}
