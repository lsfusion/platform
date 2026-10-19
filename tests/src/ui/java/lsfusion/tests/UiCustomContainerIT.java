package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.Test;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertTrue;

// containers a React component or an HTML template draws (UiCustomContainer.lsf): the size of the window a form opens
// in when such a container is its content, two no-build .jsx views on one page, and the form's ENTER and ESCAPE in
// fields and blocks a view or a custom cell renderer draws itself, and in the platform's own editors
public class UiCustomContainerIT extends UiTest {

    private static final String FOLDER = "UiCustomContainer.uiCustomContainer";

    // a form opened as a window, its content a container a React component draws: the window takes the size of what the
    // component draws first, a block bigger than the window would be without it (#1817)
    @Test
    public void aWindowTakesTheSizeOfWhatAComponentDraws() throws Exception {
        windowFits("UiCustomContainer.uiCustomContainerReactSizedWindow", "UiCustomContainer.uiCustomContainerReactSized",
                ".ui-custom-container-block");
    }

    // ... and of what an HTML template draws, which has no child the platform draws (#1817)
    @Test
    public void aWindowTakesTheSizeOfWhatATemplateDraws() throws Exception {
        windowFits("UiCustomContainer.uiCustomContainerTemplateSizedWindow", "UiCustomContainer.uiCustomContainerTemplateSized",
                ".ui-custom-container-template");
    }

    private void windowFits(String action, String formName, String block) {
        open(FOLDER, action);
        Locator form = form(formName), drawn = form.locator(block);
        assertThat(drawn).isVisible();
        Locator window = page.locator(".modal-content").filter(new Locator.FilterOptions().setHas(form));
        BoundingBox inner = drawn.boundingBox(), outer = window.boundingBox();
        assertTrue("the block " + box(inner) + " is not inside its window " + box(outer),
                inner.x >= outer.x && inner.y >= outer.y
                        && inner.x + inner.width <= outer.x + outer.width && inner.y + inner.height <= outer.y + outer.height);
        close(form);
    }

    private static String box(BoundingBox box) {
        return Math.round(box.x) + "," + Math.round(box.y) + " " + Math.round(box.width) + "x" + Math.round(box.height);
    }

    // two .jsx files in web/init, each with a React view, both run on one page: what the transform adds to the classic
    // script of each file is named per file (#1805)
    @Test
    public void twoJsxViewsRunOnOnePage() throws Exception {
        Locator form = open(FOLDER, "UiCustomContainer.uiCustomContainerTwo");
        assertThat(form.locator(".ui-custom-container-first")).hasText("The first view");
        assertThat(form.locator(".ui-custom-container-second")).hasText("The second view");
        close(form);
    }

    // in a field a view says the user is editing in (controller.startEditing), ENTER does not move the focus to the
    // next component and ESCAPE does not close the form; in one it says nothing of, both do what they do anywhere else.
    // Once the field loses the focus, the view takes its word back (stopEditing), and ENTER - which steps aside while
    // the user is said to edit anywhere in the form, where ESCAPE steps aside only for a key pressed in the field
    // edited - moves the focus on again (#1819)
    @Test
    public void enterAndEscapeInAFieldTheViewSaysIsEdited() throws Exception {
        Locator form = open(FOLDER, "UiCustomContainer.uiCustomContainerKeys");
        Locator undeclared = form.locator(".ui-custom-container-undeclared"), declared = form.locator(".ui-custom-container-declared");
        undeclared.click();
        press("Enter");
        assertThat(declared).isFocused(); // the next component

        type("ab");
        press("Enter");
        type("c");
        assertThat(declared).hasValue("abc"); // the focus stayed
        press("Escape");
        type("d");
        assertThat(declared).hasValue("abcd"); // the form stayed open, the focus in the field

        undeclared.click();
        press("Enter");
        assertThat(declared).isFocused(); // nobody is said to edit any more

        undeclared.click();
        press("Escape");
        form.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    // ... and in a block that takes keys, which the browser does not take for a text control: what the view says
    // decides, not the kind of the element. ENTER moves the focus from a block the view says nothing of to the form's
    // first field, a block being none of the components ENTER steps through (#1819)
    @Test
    public void enterAndEscapeInABlockTheViewSaysIsEdited() throws Exception {
        Locator form = open(FOLDER, "UiCustomContainer.uiCustomContainerKeys");
        Locator undeclared = form.locator(".ui-custom-container-undeclared-block");
        Locator declared = form.locator(".ui-custom-container-declared-block");
        declared.click();
        press("Enter");
        type("x");
        assertThat(declared).hasText("x"); // the focus stayed
        press("Escape");
        type("y");
        assertThat(declared).hasText("xy"); // the form stayed open, the focus in the block

        undeclared.click();
        press("Enter");
        assertThat(form.locator(".ui-custom-container-undeclared")).isFocused();

        undeclared.click();
        press("Escape");
        form.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    // a field a custom cell renderer draws in its cell, of which it says nothing: ENTER moves the focus on to the next
    // component, and ESCAPE closes the form, as from a field of the form's own (#1819)
    @Test
    public void enterAndEscapeInAFieldOfACustomCell() throws Exception {
        Locator form = open(FOLDER, "UiCustomContainer.uiCustomContainerCell");
        Locator field = form.locator(".ui-custom-container-cell-field");
        field.click();
        press("Enter");
        assertThat(value(form, "uiCustomContainerAfter")).isFocused();

        field.click();
        press("Escape");
        form.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    // the platform's own editor of a property, in a form of its own: ENTER commits what is typed in it and ESCAPE
    // cancels it, the form staying open - the value the form holds, which its Record button saves after each key, is
    // the one committed -, and ESCAPE with no editor open closes the form, as before a view could say what is edited
    // (#1819)
    @Test
    public void enterAndEscapeInThePlatformsEditor() throws Exception {
        editThenClose(open(FOLDER, "UiCustomContainer.uiCustomContainerPlain"));
    }

    // ... and placed in a container a React view draws (<Platform name/>), the view saying nothing of what is edited:
    // the editor the platform draws there is the form's own, as anywhere else (#1819)
    @Test
    public void enterAndEscapeInThePlatformsEditorAViewPlaces() throws Exception {
        Locator form = open(FOLDER, "UiCustomContainer.uiCustomContainerPlaced");
        assertThat(form.locator(".ui-custom-container-placed [lsfusion-container='PROPERTY(uiCustomContainerText())']"))
                .isVisible();
        editThenClose(form);
    }

    // the text edited in the platform's editor: committed, then cancelled, then committed again - each by its key,
    // which ends the editing, the value the form holds saved after it -, then the form closed by ESCAPE
    private void editThenClose(Locator form) throws Exception {
        String run = "edit " + System.nanoTime();
        exec("UiCustomContainer.uiCustomContainerSeed", run);
        Locator text = value(form, "uiCustomContainerText");
        typeIn(text, "ab");
        endEditing(form, "Enter");
        assertThat(text).hasValue("ab");
        record(form, run, "ab");
        typeIn(text, "cd");
        endEditing(form, "Escape");
        assertThat(text).hasValue("ab");
        record(form, run, "ab,ab");
        typeIn(text, "ef");
        endEditing(form, "Enter");
        assertThat(text).hasValue("ef");
        record(form, run, "ab,ab,ef");

        press("Escape");
        form.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    // a key pressed in the platform's editor of the text, which is open before the key and is closed by it - not by the
    // click of the Record button after it, which would commit an editor still open
    private void endEditing(Locator form, String key) {
        Locator editing = form.locator("[lsfusion-container='PROPERTY(uiCustomContainerText())'] .is-editing");
        assertThat(editing).hasCount(1);
        press(key);
        assertThat(editing).hasCount(0);
    }

    // the value the form holds saved by its Record button, and what it saved for the run since the test seeded it
    private static void record(Locator form, String run, String expected) throws Exception {
        form.locator("button[lsfusion-container='PROPERTY(uiCustomContainerRecord())']").click();
        check("UiCustomContainer.uiCustomContainerCheckEdits", run, expected);
    }

    // the input the platform draws the value of a property in
    private static Locator value(Locator form, String property) {
        return form.locator("[lsfusion-container='PROPERTY(" + property + "())'] input");
    }

    // the editor of a property opened by a double click on its input, the text filled into it
    private void typeIn(Locator value, String text) {
        value.dblclick();
        value.fill(text);
    }

    // a real key press to the element that has the focus, once no request blocks the input, which the client drops
    // while one does
    private void press(String key) {
        idle();
        page.keyboard().press(key);
    }

    private void type(String text) {
        idle();
        page.keyboard().type(text);
    }

    private void idle() {
        page.locator(".lsf-blocking").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }
}
