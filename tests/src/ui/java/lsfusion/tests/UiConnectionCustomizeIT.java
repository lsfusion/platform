package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.After;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// The customization of a connection (SystemEvents.dataExtendCode[]): its code is compiled as a connection is made, its
// NAVIGATOR statements over a copy of the navigator, so a page loaded after the code is saved draws the navigator the
// code made, and the navigator of the logics is back once the code is dropped (UiConnectionCustomize.lsf).
// The code is written in the dialog a form's code is written in (SystemEvents.customizeForm).
// What the code declares, a script evaluated in that client sees.
// And a call by its name, made in that client, finds.
public class UiConnectionCustomizeIT extends UiTest {

    private static final String FOLDER = "UiConnectionCustomize.uiConnectionCustomize";
    private static final String FORM = "UiConnectionCustomize.uiCcForm";
    private static final String OTHER = "UiConnectionCustomize.uiCcOther";

    // the codes are for all users, so that it does not matter which user the browser is : a test leaves none of them to
    // the next one, whose navigator would be made of them too
    @After
    public void dropTheCodes() throws Exception {
        exec("UiConnectionCustomize.uiCcReset");
    }

    // a folder of the logics renamed, an element of it put first, a folder added with an element for a form of the
    // logics and one for a form the code itself declares
    private static final String CODE =
            "FORM uiCcOwn 'Own form';\n" +
            "NAVIGATOR {\n" +
            "    " + FOLDER + " 'Customized folder' {\n" +
            "        " + OTHER + " FIRST;\n" +
            "        NEW FOLDER uiCcAdded 'Added folder' {\n" +
            "            NEW FORM uiCcNew 'New entry' = " + FORM + ";\n" +
            "            NEW uiCcOwn;\n" +
            "        }\n" +
            "    }\n" +
            "}";

    @Test
    public void aCodeChangesTheNavigator() throws Exception {
        assertThat(element(FOLDER)).containsText("UI connection customize");

        exec("UiConnectionCustomize.uiCcSet", CODE);
        restart();

        assertThat(element(FOLDER)).containsText("Customized folder");
        element(FOLDER).click();
        assertThat(added("uiCcAdded")).containsText("Added folder");
        assertTrue("the element the code put first is not before the other one", (Boolean) page.evaluate(
                "([first, second]) => !!(document.querySelector(first).compareDocumentPosition(document.querySelector(second)) & Node.DOCUMENT_POSITION_FOLLOWING)",
                new String[]{selector(OTHER), selector(FORM)}));

        // an element of the logics the code moved still opens its form
        close(open(FOLDER, OTHER));

        // an element the code added for a form of the logics
        open(FOLDER, added("uiCcNew"));
        assertThat(form(FORM)).isVisible();
        close(form(FORM));

        // and one for a form of the code's own, which is of the module the code was compiled into
        open(FOLDER, added("uiCcOwn"));
        Locator own = page.locator("[lsfusion-form$='.uiCcOwn']");
        assertThat(own).isVisible();
        close(own);

        exec("UiConnectionCustomize.uiCcReset");
        restart();
        assertThat(element(FOLDER)).containsText("UI connection customize");
        assertThat(added("uiCcAdded")).hasCount(0);
    }

    // a window of the code's own with a section drawn in it, a window that holds forms with an action that opens a form
    // into it, a window of the logics hidden, and the window the forms are opened in turned to one form at a time
    private static final String WINDOWS =
            "WINDOW uiCcWindow 'Own window' HORIZONTAL BOTTOM HIDETITLE;\n" +
            "WINDOW uiCcForms 'Own forms' FORMS AUTOSIZE POSITION(20, 6, 80, 2) HIDETITLE CLASS 'ui-cc-forms';\n" +
            "HIDE WINDOW System.logo;\n" +
            "EXTEND WINDOW System.forms FORMS NOTABBED;\n" +
            "uiCcIntoOwn 'Into the own window' () { SHOW " + OTHER + " WINDOW uiCcForms NOWAIT; }\n" +
            "NAVIGATOR {\n" +
            "    NEW FOLDER uiCcSection 'Own section' WINDOW uiCcWindow PARENT {\n" +
            "        NEW FORM uiCcFormInWindow 'Form' = " + FORM + ";\n" +
            "        NEW FORM uiCcOtherInWindow 'Other' = " + OTHER + ";\n" +
            "        NEW uiCcIntoOwn;\n" +
            "    }\n" +
            "}";

    // the windows are the navigator's as its elements are : the code declares, hides and turns over the windows of the
    // copy it changes, and the windows of the logics are back once the code is dropped
    @Test
    public void aCodeChangesTheWindows() throws Exception {
        assertThat(element("System.logo")).isVisible();

        exec("UiConnectionCustomize.uiCcSet", WINDOWS);
        restart();

        assertThat(element("System.logo")).isHidden();

        // a section of the code is drawn in a window of the code, and its element opens its form
        Locator own = added("uiCcWindow");
        own.locator(addedSelector("uiCcFormInWindow")).click();
        assertThat(form(FORM)).isVisible();

        // the window the forms are opened in draws one at a time now : the one a second form displaces is closed
        own.locator(addedSelector("uiCcOtherInWindow")).click();
        assertThat(form(OTHER)).isVisible();
        assertThat(form(FORM)).hasCount(0);
        close(form(OTHER));

        // and a form an action of the code opens into the code's window is drawn there
        own.locator(addedSelector("uiCcIntoOwn")).click();
        Locator inOwn = page.locator(".ui-cc-forms").locator("[lsfusion-form='" + OTHER + "']");
        assertThat(inOwn).isVisible();
        close(inOwn);

        exec("UiConnectionCustomize.uiCcReset");
        restart();
        assertThat(element("System.logo")).isVisible();
        assertThat(added("uiCcWindow")).hasCount(0);
    }

    // what draws a window stays the logics' : the standard windows are styled by it
    @Test
    public void aCodeCannotSayWhatDrawsAWindow() throws Exception {
        for (String code : new String[]{"WINDOW uiCcDrawn 'Drawn' HORIZONTAL BOTTOM CUSTOM 'UiCcDrawn';", "EXTEND WINDOW System.toolbar CUSTOM 'UiCcDrawn';"}) {
            exec("UiConnectionCustomize.uiCcSetBroken", code);
            restart();

            closeDialog("CUSTOM option of a WINDOW statement cannot be used in EVAL module");
        }
    }

    // a client cannot start without a navigator, so a code that cannot be compiled leaves the navigator of the logics,
    // and says so
    @Test
    public void aBrokenCodeLeavesTheNavigator() throws Exception {
        exec("UiConnectionCustomize.uiCcSetBroken", "NAVIGATOR {\n    uiCcNoSuchElement 'Renamed';\n}");
        restart();

        closeDialog("Application customization code is broken");
        assertThat(element(FOLDER)).containsText("UI connection customize");
        close(open(FOLDER, FORM));
    }

    // the editor of the code is the dialog a form's code is written in, opened for the connection's code from the administration.
    // The code written there for the user is saved and the client reloaded, so the navigator is the code's at once
    @Test
    public void theEditorSavesTheCodeAndReloads() {
        open("System.administration", element("SystemEvents.customize"));
        Locator editor = form("SystemEvents.customizeForm");
        type(editor, "NAVIGATOR {\n    " + FOLDER + " 'Folder of the editor';\n}");
        action(editor, "formOk").click();

        assertThat(element(FOLDER)).containsText("Folder of the editor", new LocatorAssertions.ContainsTextOptions().setTimeout(60_000));
    }

    // and it customizes a form from the form's toolbar : the code written for the user extends the form, which is opened
    // anew with it
    @Test
    public void theEditorCustomizesAForm() {
        action(open(FOLDER, FORM), "formCustomize").click();
        Locator editor = form("SystemEvents.customizeForm");
        type(editor, "EXTEND FORM uiCcForm PROPERTIES() uiCcAdded;");
        action(editor, "formOk").click();

        Locator customized = form(FORM);
        assertThat(customized.locator("[lsfusion-container='PROPERTY(uiCcAdded())']")).isVisible();
        close(customized);
    }

    // and what is written in it and then left by closing the dialog - which asks first - is dropped : the editor opened
    // again keeps nothing of it, and its OK opens the form anew without it
    @Test
    public void closingTheEditorDropsWhatWasWritten() {
        Locator form = open(FOLDER, FORM);
        action(form, "formCustomize").click();
        Locator editor = form("SystemEvents.customizeForm");
        type(editor, "EXTEND FORM uiCcForm PROPERTIES() uiCcAdded;");
        closeDropping(editor);

        action(form, "formCustomize").click();
        Locator reopened = form("SystemEvents.customizeForm");
        answered(0, () -> action(reopened, "formOk").click());

        assertThat(form).isVisible();
        assertThat(form.locator("[lsfusion-container='PROPERTY(uiCcAdded())']")).isHidden();
        close(form);
    }

    // its Save saves the code and leaves the editor open, with its editors and what is written in them : the editor
    // opened again has the code
    @Test
    public void theEditorsSaveKeepsItOpen() {
        Locator form = open(FOLDER, FORM);
        action(form, "formCustomize").click();
        Locator editor = form("SystemEvents.customizeForm");
        String code = "EXTEND FORM uiCcForm PROPERTIES() uiCcAdded;";
        type(editor, code);
        answered(0, () -> action(editor, "formApply").click());
        assertThat(personalEditor(editor)).isVisible();
        assertEquals(code, personalEditor(editor).evaluate("ace => ace.parentElement.aceEditor.getValue()"));
        close(editor);

        answered(0, () -> action(form, "formCustomize").click());
        Locator reopened = form("SystemEvents.customizeForm");
        assertEquals(code, personalEditor(reopened).evaluate("ace => ace.parentElement.currentValue"));
        close(reopened);
        close(form);
    }

    // a code that does not compile is refused on OK : the editor shows the compiler's error and stays open, and the code
    // is not saved - the editor opened again has none of it. For a form's code
    @Test
    public void theEditorRefusesBrokenFormCode() {
        Locator form = open(FOLDER, FORM);
        action(form, "formCustomize").click();
        Locator editor = form("SystemEvents.customizeForm");
        refuseCode(editor, "EXTEND FORM uiCcForm PROPERTIES() uiCcNoSuchProperty;", "uiCcNoSuchProperty");

        answered(0, () -> action(form, "formCustomize").click());
        assertEquals("", personalEditor(editor).evaluate("ace => ace.parentElement.currentValue"));
        close(editor);
        close(form);
    }

    // and for the application's
    @Test
    public void theEditorRefusesBrokenApplicationCode() {
        open("System.administration", element("SystemEvents.customize"));
        Locator editor = form("SystemEvents.customizeForm");
        refuseCode(editor, "NAVIGATOR { NEW uiCcNoSuchForm; }", "uiCcNoSuchForm");

        answered(0, () -> open("System.administration", element("SystemEvents.customize")));
        assertEquals("", personalEditor(editor).evaluate("ace => ace.parentElement.currentValue"));
        close(editor);
    }

    private void refuseCode(Locator editor, String code, String missingName) {
        type(editor, code);
        action(editor, "formOk").click();
        String refusal = "The code does not compile and is not saved:";
        assertThat(dialog(refusal)).containsText(missingName);
        closeDialog(refusal);
        assertThat(editor).isVisible();
        assertEquals(code, personalEditor(editor).evaluate("ace => ace.parentElement.currentValue"));
        closeDropping(editor);
    }

    // the editor edits the codes in a session of its own, so closing it with what is written asks whether to drop that
    private void closeDropping(Locator editor) {
        editor.locator("button[lsfusion-container='PROPERTY(formClose())']").click();
        Locator question = page.locator(".modal-dialog").filter(new Locator.FilterOptions().setHasText("Do you really want close the form"));
        question.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Yes").setExact(true)).click();
        editor.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    private Locator personalEditor(Locator editor) {
        return editor.locator("[lsfusion-container='PROPERTY(extendCodeCurrentUser)'] .ace_editor");
    }

    // the code written into the editor's "For me" : the editor sends its text a moment after it changes, and the dialog
    // is closed once the server has it
    private void type(Locator editor, String code) {
        Locator ace = personalEditor(editor);
        answered(0, () -> {
            ace.evaluate("(ace, code) => ace.parentElement.aceEditor.setValue(code, 1)", code);
            page.waitForCondition(() -> code.equals(ace.evaluate("ace => ace.parentElement.currentValue")));
        });
    }

    // a form of another test, whose view keeps its controller for a page's code to call (UiCustomView.js)
    private static final String CALLS = "UiCustomView.uiCustomViewCalls";

    // a property and an action the code declares, for the scripts evaluated in the client - one of them an action of the
    // code's own, an element of the navigator
    private static String declarations(int answer) {
        return "uiCcAnswer 'Answer of the code' () = " + answer + ";\n" +
               "uiCcSay() { MESSAGE 'Said by the code'; }\n" +
               "uiCcAsk 'Evaluate in the navigator' () { EVAL ACTION 'uiCcSay();'; }\n" +
               "NAVIGATOR {\n" +
               "    " + FOLDER + " {\n" +
               "        NEW uiCcAsk;\n" +
               "    }\n" +
               "}";
    }

    // a script evaluated anywhere in a client sees what the code of its connection declares
    @Test
    public void aScriptEvaluatedInTheClientSeesTheCode() throws Exception {
        exec("UiConnectionCustomize.uiCcSet", declarations(42));
        exec("UiConnectionCustomize.uiCcSetForm", FORM, "EXTEND FORM uiCcForm PROPERTIES() uiCcAnswer;");
        exec("UiConnectionCustomize.uiCcSetForm", CALLS, "uiCcFormAnswer() = 7;");
        restart();

        // one an action of the navigator evaluates
        open(FOLDER, added("uiCcAsk"));
        closeDialog("Said by the code");

        // one an action of a form opened in the client does
        Locator other = open(FOLDER, OTHER);
        action(other, "uiCcEvaluate").click();
        closeDialog("Said by the code");
        close(other);

        // the code that customizes a form
        assertAnswerOnTheForm("42");

        // one the code of a page sends : evaluated on a customized form, it sees what that form's code declares as well
        assertEquals("49", openCalls().answer("controller.eval('run() { RETURN uiCcAnswer() + uiCcFormAnswer(); }')"));
        closeCalls();

        // the code of the form is the same, the connection's is another : the form is compiled for the connection it is
        // opened in
        exec("UiConnectionCustomize.uiCcSet", declarations(43));
        restart();
        assertAnswerOnTheForm("43");
    }

    // the property of the connection's code the form's code put on the form
    private void assertAnswerOnTheForm(String answer) {
        Locator customized = open(FOLDER, FORM);
        String container = "[lsfusion-container='PROPERTY(uiCcAnswer())']";
        assertThat(customized.locator(container + ".panel-renderer-value, " + container + " .panel-renderer-value")).hasText(answer);
        close(customized);
    }

    // the form whose view keeps its controller, customized by a test or not, and that controller
    private View openCalls() {
        open("UiCustomView.uiCustomView", element(CALLS));
        return controller("window.uiCustomViewCalls && window.uiCustomViewCalls.form");
    }

    private void closeCalls() {
        close(page.locator("[lsfusion-form$='uiCustomViewCalls']"));
    }

    // a controller the code of a page keeps, for a test to call as that code would
    private View controller(String kept) {
        return new View("const controller = " + kept + ";", "!!controller", "", "null").drawn();
    }

    // and what the code of a connection, or of a form, declares is called by its name : the code of a page runs an
    // action and changes a property of the code through the controller - the form's, or the navigator's, which an action
    // of the connection's code hands to a function of the page. A name is looked for among what the logics and the code
    // declare, in every namespace alike : one the logics, or the other code, declares as well is nobody's until a
    // namespace says whose - and a form's code is that form's only, the navigator's controller does not see it
    @Test
    public void aCallByNameFindsWhatTheCodeDeclares() throws Exception {
        exec("UiConnectionCustomize.uiCcSet",
                "uiCcTwice(INTEGER a) { RETURN a * 2; }\n" +
                "uiCcNote = DATA LOCAL STRING[20] ();\n" +
                "uiCcLogics(INTEGER a) { RETURN a + 100; }\n" +
                "uiCcLogicsNote = DATA LOCAL STRING[20] ();\n" +
                "uiCcBoth(INTEGER a) { RETURN a; }\n" +
                "uiCcKeep 'Keep the controller' () { INTERNAL CLIENT 'uiCcKeepController'; }\n" +
                "NAVIGATOR {\n" +
                "    " + FOLDER + " {\n" +
                "        NEW uiCcKeep;\n" +
                "    }\n" +
                "}");
        exec("UiConnectionCustomize.uiCcSetForm", CALLS,
                "uiCcFormTwice(INTEGER a) { RETURN a * 2 + 1; }\n" +
                "uiCcBoth(INTEGER a) { }");
        restart();

        page.evaluate("() => { window.uiCcKeepController = controller => { window.uiCcController = controller; }; }");
        open(FOLDER, added("uiCcKeep"));
        View connection = controller("window.uiCcController");
        assertEquals("42", connection.answer("controller.exec('uiCcTwice', 21)"));
        assertAmbiguous(connection, "exec('uiCcLogics', 1)", "uiCcLogics");
        assertEquals("7", connection.answer("controller.exec('uiCcBoth', 7)"));

        View calls = openCalls();
        assertEquals("42", calls.answer("controller.exec('uiCcTwice', 21)"));
        assertEquals("42", calls.answer("controller.exec('uiCcTwice[INTEGER]', 21)"));
        assertEquals("43", calls.answer("controller.exec('uiCcFormTwice', 21)"));
        assertAmbiguous(calls, "exec('uiCcLogics', 1)", "uiCcLogics");
        assertAmbiguous(calls, "exec('uiCcBoth', 1)", "uiCcBoth");
        assertAmbiguous(calls, "change('uiCcLogicsNote', 'noted')", "uiCcLogicsNote");
        assertEquals("1", calls.answer("controller.exec('UiConnectionCustomize.uiCcLogics', 1)"));
        assertEquals("noted", calls.answer("controller.change('uiCcNote', 'noted').then(() => controller.eval('run() { RETURN uiCcNote(); }'))"));
        closeCalls();
        assertEquals("7", connection.answer("controller.exec('uiCcBoth', 7)"));
    }

    private static void assertAmbiguous(View controller, String call, String name) {
        String rejection = controller.rejection("controller." + call);
        assertTrue("a name declared twice was answered with " + rejection, rejection.startsWith("Ambiguous element '" + name + "'"));
    }

    // an entry of a form's list of what a page may call names an action the form's own code declares : by the entry's
    // name, and by the action's own
    @Test
    public void aFormsCodeListsItsOwnActionForThePage() throws Exception {
        exec("UiConnectionCustomize.uiCcSetForm", CALLS,
                "uiCcListed(INTEGER a) { RETURN a * 3; }\n" +
                "EXTEND FORM uiCustomViewCalls CUSTOMS uiCcTriple = uiCcListed;");

        View calls = openCalls();
        assertEquals("42", calls.answer("controller.exec('uiCcTriple', 14)"));
        assertEquals("42", calls.answer("controller.exec('uiCcListed', 14)"));
        closeCalls();
    }

    // a client makes its navigator as its page is loaded
    private void restart() {
        page.reload();
        element("System.root").waitFor();
    }

    private static String selector(String element) {
        return "[lsfusion-container='" + element + "']";
    }

    private Locator element(String element) {
        return page.locator(selector(element));
    }

    // an element or a window the code added: its namespace is the one of the module the code is compiled into, made
    // anew with it
    private Locator added(String name) {
        return page.locator(addedSelector(name));
    }

    private static String addedSelector(String name) {
        return "[lsfusion-container$='." + name + "']";
    }
}
