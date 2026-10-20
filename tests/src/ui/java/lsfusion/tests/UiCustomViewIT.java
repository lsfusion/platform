package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// what a custom view's controller does (UiCustomView.lsf, its views in UiCustomView.js): the places of a React view
// that can hold no view - one outside the page, a second one of a row's editor -, a value a React view gives a member,
// the calls to the server a React view makes through its controller, a custom cell renderer's controller, and the
// controller an INTERNAL CLIENT action of a form and of the navigator gives the JS function it runs
public class UiCustomViewIT extends UiTest {

    private static final String FOLDER = "UiCustomView.uiCustomView";

    // the place a React view renders first for a child the platform draws is in a node it never puts in the page: once
    // the render is over, the place gives the child up, saying so on the console, and the place in the page takes it
    // (#1756)
    @Test
    public void aPlaceOutsideThePageGivesItsChildUp() throws Exception {
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewStray");
        Locator live = form.locator(".ui-custom-view-live");
        assertThat(live.locator("[lsfusion-container='PROPERTY(uiCustomViewNote())']")).isVisible();
        assertThat(live.locator(".lsf-view-error")).hasCount(0);
        close(form);
    }

    // two places of the platform's editor of one row: the first keeps it and the second says so, and once the view
    // takes the first one away the second gets the editor (#1754)
    @Test
    public void theSecondPlaceOfARowsEditorTakesItOnceTheFirstGoes() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewRows");
        Locator row = form.locator(".ui-custom-view-row").filter(new Locator.FilterOptions().setHasText(items[0]));
        Locator first = row.locator(".ui-custom-view-first"), second = row.locator(".ui-custom-view-second");
        assertThat(first).hasText("1");
        place("twice");
        assertThat(second.locator(".lsf-view-error"))
                .hasText("lsFusion: 'i.uiCustomViewQuantity' is already placed by another <Platform>");
        assertThat(first).hasText("1");

        place("second");
        assertThat(first).hasCount(0);
        assertThat(second.locator(".lsf-view-error")).hasCount(0);
        assertThat(second).hasText("1");
        close(form);
    }

    // a 0 a view gives the member of an integer property is 0, not NULL - a JS 0 held as an object reads as null in the
    // client's Java: the view is given 0 back once the change is answered, its cell no longer loading - the value the
    // change shows at once is the view's own -, and 0 is what the form saves
    @Test
    public void aZeroAViewGivesIsZero() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewQuantities");
        Locator row = form.locator(".ui-custom-view-quantity-row").filter(new Locator.FilterOptions().setHasText(items[0]));
        Locator quantity = row.locator(".ui-custom-view-quantity");
        assertThat(quantity).hasText("number 1");
        row.locator(".ui-custom-view-zero").click();
        assertThat(quantity).hasText("number 0");
        Locator save = form.locator("button[lsfusion-container='PROPERTY(formApply())']");
        assertThat(save).isEnabled(); // the server has the change
        save.click();
        check("UiCustomView.uiCustomViewCheckQuantity", items[0], "0");
        close(form);
    }

    // the calls a React view makes to the server through its controller resolve to what the server answers, of its
    // type - eval to the RETURN of its script, exec to the RETURN of the action, evalAction and change to nothing - and
    // run in the form's session, a change seen by the calls after it and by the form; a call that fails is rejected
    // with its error
    @Test
    public void aViewsCallsToTheServer() throws Exception {
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewCalls");
        Locator results = form.locator(".ui-custom-view-results li");
        call(form, "eval");
        assertThat(results).hasText(new String[] {"eval: number 42"});
        call(form, "evalAction");
        call(form, "read");
        assertThat(results).hasText(new String[] {"eval: number 42", "evalAction: undefined undefined", "read: number 5"});
        assertThat(value(form, "uiCustomViewCount")).hasText("5");
        call(form, "exec");
        call(form, "change");
        assertThat(results).hasText(new String[] {"eval: number 42", "evalAction: undefined undefined", "read: number 5",
                "exec: number 8", "change: undefined undefined"});
        assertThat(value(form, "uiCustomViewCount")).hasText("9");
        call(form, "broken");
        call(form, "missing");
        assertThat(results).hasText(new Pattern[] {Pattern.compile("^eval: number 42$"),
                Pattern.compile("^evalAction: undefined undefined$"), Pattern.compile("^read: number 5$"),
                Pattern.compile("^exec: number 8$"), Pattern.compile("^change: undefined undefined$"),
                Pattern.compile("^broken rejected: .+"), Pattern.compile("^missing rejected: .+")});
        close(form);
    }

    // a call made through the controller of a form that is closed throws nothing: the promise it returns is rejected -
    // Form is closed - and does not hang. The form is closed for its controller once the client has let go of it,
    // which follows its leaving the page, so the call is made until it is refused
    @Test
    public void aCallOfAClosedFormIsRejected() throws Exception {
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewCalls");
        call(form, "eval");
        assertThat(form.locator(".ui-custom-view-results li")).hasText(new String[] {"eval: number 42"});
        close(form);
        assertEquals("Form is closed", page.waitForFunction("() => window.uiCustomViewCalls.eval('run() { RETURN 1; }')"
                + ".then(() => false, error => error.message)", null,
                new Page.WaitForFunctionOptions().setTimeout(10_000)).jsonValue());
    }

    // the JS function an INTERNAL CLIENT action runs is given the action's parameters and the form's controller: what
    // it returns is the action's answer (TO), and a change it makes through the controller - one that declares the
    // callbacks, run asynchronously - is the form's, made in its session; the client function after it in the action
    // runs once the function says it is done, the change over, and it is given both callbacks
    @Test
    public void aClientActionIsGivenTheFormsController() throws Exception {
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewClient");
        Locator answer = value(form, "uiCustomViewAnswer");
        form.locator("button[lsfusion-container='PROPERTY(uiCustomViewOnClient())']").click();
        assertThat(answer).hasText("42, with a controller");
        form.locator("button[lsfusion-container='PROPERTY(uiCustomViewThroughServer())']").click();
        assertThat(answer).hasText("the server says 63");
        assertEquals("after the change, given function function", page.waitForFunction(
                "() => window.uiCustomViewAfterServerSaw", null, new Page.WaitForFunctionOptions().setTimeout(10_000)).jsonValue());
        close(form);
    }

    // the JS function an INTERNAL CLIENT action of the navigator runs is given the navigator's controller, which calls
    // the server as a form's does: a script it evaluates resolves to its RETURN, and an action it runs by name runs
    @Test
    public void aClientActionOfTheNavigatorIsGivenItsController() throws Exception {
        seed();
        open(FOLDER, "UiCustomView.uiCustomViewFromNavigator");
        check("UiCustomView.uiCustomViewCheckNavigatorMarks", "navigator number 42");
    }

    // a custom cell renderer changes its property through its controller - the server has the value, which the form
    // shows in a property of its own - and reaches the form's controller from there, whose call resolves to what the
    // server answers
    @Test
    public void aCustomCellRendererChangesItsPropertyAndCallsTheServer() throws Exception {
        Locator form = open(FOLDER, "UiCustomView.uiCustomViewCell");
        Locator field = form.locator(".ui-custom-view-cell-field");
        field.fill("typed");
        field.blur();
        assertThat(value(form, "uiCustomViewCellEcho")).hasText("typed");
        form.locator(".ui-custom-view-cell-exec").click();
        assertThat(form.locator(".ui-custom-view-cell-result")).hasText("exec: number 8");
        close(form);
    }

    // two items under names no other attempt has - what a failed attempt saved stays in the database for its rerun -,
    // of the quantities 1 and 2
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"First " + id, "Second " + id};
        exec("UiCustomView.uiCustomViewSeed", items);
        return items;
    }

    private static void call(Locator form, String name) {
        form.locator(".ui-custom-view-" + name).click();
    }

    private static Locator value(Locator form, String property) {
        String container = "[lsfusion-container='PROPERTY(" + property + "())']";
        return form.locator(container + ".panel-renderer-value, " + container + " .panel-renderer-value");
    }

    // what the first row of the rows view places from now on, see UiCustomView.js
    private void place(String what) {
        page.waitForFunction("() => !!window.uiCustomViewRows");
        page.evaluate("what => window.uiCustomViewRows.place(what)", what);
    }
}
