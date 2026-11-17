package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.After;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertTrue;

// The customization of a connection (SystemEvents.dataExtendCode[]): its code is compiled as a connection is made, its
// NAVIGATOR statements over a copy of the navigator, so a page loaded after the code is saved draws the navigator the
// code made, and the navigator of the logics is back once the code is dropped (UiConnectionCustomize.lsf).
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
            exec("UiConnectionCustomize.uiCcSet", code);
            restart();

            closeDialog("CUSTOM option of a WINDOW statement cannot be used in EVAL module");
        }
    }

    // a client cannot start without a navigator, so a code that cannot be compiled leaves the navigator of the logics,
    // and says so
    @Test
    public void aBrokenCodeLeavesTheNavigator() throws Exception {
        exec("UiConnectionCustomize.uiCcSet", "NAVIGATOR {\n    uiCcNoSuchElement 'Renamed';\n}");
        restart();

        closeDialog("Application customization code is broken");
        assertThat(element(FOLDER)).containsText("UI connection customize");
        close(open(FOLDER, FORM));
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
