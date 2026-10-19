package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.assertions.LocatorAssertions;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.KeyboardModifier;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// the FORMS windows of an application (UiFormsWindow.lsf): what a window that draws one form at a time does with the
// form it displaces - closes it, asks about its changes, keeps it -, docked-modal pairs in it, the address ACTIVATE
// FORM and CLOSE FORM name a form by, an open that activates the form it finds, a value a React view supplies, and a
// window a React component draws. The forms are opened from the buttons of a launcher in System.forms, or from the
// navigator
public class UiFormsWindowIT extends UiTest {

    private static final String FOLDER = "UiFormsWindow.uiFormsWindow";
    private static final String FIRST = "UiFormsWindow.uiFormsWindowFirst", SECOND = "UiFormsWindow.uiFormsWindowSecond",
            ITEMS = "UiFormsWindow.uiFormsWindowItems";

    // a window of one form closes the form it displaces at once - on the server too -, which is what it does with no
    // CLOSE said, and shows nothing of its own around the form it draws: no tab strip (#1842)
    @Test
    public void aWindowOfOneFormClosesTheFormItDisplaces() throws Exception {
        seed();
        Locator launcher = launcher(), one = window("one");
        press(launcher, "uiFormsWindowFirstIntoOne");
        assertThat(in(one, "uiFormsWindowFirst")).isVisible();
        press(launcher, "uiFormsWindowSecondIntoOne");
        assertThat(in(one, "uiFormsWindowSecond")).isVisible();
        assertThat(in(one, "uiFormsWindowFirst")).hasCount(0);
        assertThat(one.locator(".nav-tabs")).hasCount(0);
        check("UiFormsWindow.uiFormsWindowCheckClosed", "1");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");
    }

    // two opens into a window of one form in one action, which arrive in one response: the window draws the second
    // and closes the first
    @Test
    public void twoOpensInOneActionLeaveTheSecondDrawn() throws Exception {
        seed();
        Locator launcher = launcher(), one = window("one");
        press(launcher, "uiFormsWindowBothIntoOne");
        assertThat(in(one, "uiFormsWindowSecond")).isVisible();
        assertThat(in(one, "uiFormsWindowFirst")).hasCount(0);
        check("UiFormsWindow.uiFormsWindowCheckClosed", "1");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");
    }

    // a displaced form with unsaved changes asks before it closes, and the one the user keeps stays in the window,
    // hidden, until ACTIVATE FORM brings it back with its changes; a later CLOSE FORM asks again
    @Test
    public void aDisplacedFormWithUnsavedChangesAsksBeforeItCloses() throws Exception {
        String[] items = seed();
        Locator launcher = launcher(), one = window("one");
        press(launcher, "uiFormsWindowItemsIntoOne");
        Locator form = in(one, "uiFormsWindowItems"), quantity = quantity(form, items[0]);
        change(form, quantity);

        press(launcher, "uiFormsWindowFirstIntoOne");
        answer("No");
        assertThat(in(one, "uiFormsWindowFirst")).isVisible();
        assertThat(form).isHidden();

        press(launcher, "uiFormsWindowActivateItems");
        assertThat(form).isVisible();
        assertThat(quantity).hasText("5");
        press(launcher, "uiFormsWindowCloseItems");
        answer("Yes");
        assertThat(form).hasCount(0);
    }

    // a form that refused to close - the user kept its unsaved changes - is asked again by the next CLOSE FORM of its
    // label (#1849)
    @Test
    public void aFormThatRefusedToCloseIsAskedAgain() throws Exception {
        String[] items = seed();
        Locator launcher = launcher(), keep = window("keep");
        press(launcher, "uiFormsWindowKeptIntoKeep");
        Locator form = in(keep, "uiFormsWindowItems");
        change(form, quantity(form, items[0]));

        press(launcher, "uiFormsWindowCloseKept");
        answer("No");
        assertThat(form).isVisible();
        press(launcher, "uiFormsWindowCloseKept");
        answer("Yes");
        assertThat(form).hasCount(0);
    }

    // a window that closes nothing (NOCLOSE) keeps the form it displaced, hidden, and draws it again once the form
    // that took its place is closed (#1842)
    @Test
    public void aWindowThatClosesNothingKeepsWhatItDisplaced() throws Exception {
        Locator launcher = launcher(), keep = window("keep");
        press(launcher, "uiFormsWindowFirstIntoKeep");
        assertThat(in(keep, "uiFormsWindowFirst")).isVisible();
        press(launcher, "uiFormsWindowSecondIntoKeep");
        assertThat(in(keep, "uiFormsWindowSecond")).isVisible();
        assertThat(in(keep, "uiFormsWindowFirst")).isHidden();
        press(launcher, "uiFormsWindowCloseSecond");
        assertThat(in(keep, "uiFormsWindowSecond")).hasCount(0);
        assertThat(in(keep, "uiFormsWindowFirst")).isVisible();
    }

    // ACTIVATE FORM run by a form's own action brings the form it names forward, as the navigator's does (#1845)
    @Test
    public void activateFormFromAFormBringsItForward() throws Exception {
        Locator launcher = launcher(), keep = window("keep");
        press(launcher, "uiFormsWindowFirstIntoKeep");
        press(launcher, "uiFormsWindowSecondIntoKeep");
        assertThat(in(keep, "uiFormsWindowFirst")).isHidden();
        press(launcher, "uiFormsWindowActivateFirst");
        assertThat(in(keep, "uiFormsWindowFirst")).isVisible();
        assertThat(in(keep, "uiFormsWindowSecond")).isHidden();
    }

    // two instances of one form are told apart by their labels: ACTIVATE FORM and CLOSE FORM of a label reach that
    // instance alone, and CLOSE FORM of the form's name every instance (#1844)
    @Test
    public void aFormIsAddressedByItsLabel() throws Exception {
        String[] items = seed();
        Locator launcher = launcher(), keep = window("keep");
        press(launcher, "uiFormsWindowLabelledIntoKeep");
        Locator forms = in(keep, "uiFormsWindowItem");
        assertThat(forms).hasCount(2);
        assertThat(text(keep, items[1])).isVisible();
        assertThat(text(keep, items[0])).isHidden();

        press(launcher, "uiFormsWindowActivateA");
        assertThat(text(keep, items[0])).isVisible();
        assertThat(text(keep, items[1])).isHidden();
        press(launcher, "uiFormsWindowCloseA");
        assertThat(forms).hasCount(1);
        assertThat(text(keep, items[1])).isVisible();
        check("UiFormsWindow.uiFormsWindowCheckItemClosed", "1,0");

        press(launcher, "uiFormsWindowAIntoKeep");
        assertThat(forms).hasCount(2);
        assertThat(text(keep, items[0])).isVisible();
        press(launcher, "uiFormsWindowCloseEveryItem");
        assertThat(forms).hasCount(0);
        check("UiFormsWindow.uiFormsWindowCheckItemClosed", "2,1");
    }

    // a docked-modal child opened into the window its opener is in keeps the opener, though the window closes a form
    // it displaces at once, and the window draws the opener again once the child is closed
    @Test
    public void aDockedModalChildKeepsItsOpener() throws Exception {
        Locator launcher = launcher(), one = window("one");
        press(launcher, "uiFormsWindowFirstIntoOne");
        Locator first = in(one, "uiFormsWindowFirst");
        press(first, "uiFormsWindowChildIntoOne");
        Locator child = in(one, "uiFormsWindowThird");
        assertThat(child).isVisible();
        assertThat(first).isHidden();
        close(child);
        assertThat(first).isVisible();
    }

    // a docked-modal child aimed at a window from a form in another window takes nobody's place there: the window
    // keeps the form it draws, and draws it again once the child is closed
    @Test
    public void aDockedModalChildFromAnotherWindowTakesNobodysPlace() throws Exception {
        Locator launcher = launcher(), one = window("one");
        press(launcher, "uiFormsWindowFirstIntoOne");
        Locator first = in(one, "uiFormsWindowFirst");
        assertThat(first).isVisible();
        press(launcher, "uiFormsWindowSecondIntoOneWait");
        Locator child = in(one, "uiFormsWindowSecond");
        assertThat(child).isVisible();
        assertThat(first).isHidden();
        close(child);
        assertThat(first).isVisible();
    }

    // the opener of a docked-modal child closed by CLOSE FORM while the child is open: once the child is closed too,
    // the window draws the form it still holds rather than nothing
    @Test
    public void anOpenerClosedUnderItsChildLeavesTheWindowDrawing() throws Exception {
        Locator launcher = launcher(), keep = window("keep");
        press(launcher, "uiFormsWindowSecondIntoKeep");
        press(launcher, "uiFormsWindowFirstIntoKeep");
        Locator first = in(keep, "uiFormsWindowFirst"), second = in(keep, "uiFormsWindowSecond");
        assertThat(second).isHidden();
        press(first, "uiFormsWindowChildIntoKeep");
        Locator child = in(keep, "uiFormsWindowThird");
        assertThat(child).isVisible();
        press(launcher, "uiFormsWindowCloseFirst");
        assertThat(first).hasCount(0);
        assertThat(child).isVisible();
        close(child);
        assertThat(second).isVisible();
    }

    // an open that activates the form it finds (SHOW ... ACTIVATE), run by a form's button: the second run brings the
    // form already there forward, and the server still runs the action - the statement and the aspect after the open,
    // and the handler of the button's own change after the action - without building the form again; so it does in a
    // session of its own (NEWSESSION), and the activation stands where the branch with the open is not taken on the
    // server (#1843)
    @Test
    public void anOpenThatActivatesTheFormItFindsStillRunsItsAction() throws Exception {
        seed();
        Locator launcher = launcher(), keep = window("keep");
        Locator first = in(keep, "uiFormsWindowFirst"), second = in(keep, "uiFormsWindowSecond");
        pressCompleted(launcher, "uiFormsWindowFirstOnce");
        marks("after", "aspect after", "property after");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");
        assertThat(first).isVisible();

        press(launcher, "uiFormsWindowSecondIntoKeep");
        assertThat(first).isHidden();
        pressCompleted(launcher, "uiFormsWindowFirstOnce");
        marks("after", "aspect after", "property after", "after", "aspect after", "property after");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");
        assertThat(first).isVisible();
        assertThat(second).isHidden();
        assertThat(first).hasCount(1);

        press(launcher, "uiFormsWindowActivateSecond");
        assertThat(first).isHidden();
        pressCompleted(launcher, "uiFormsWindowFirstOnceInSession");
        marks("after", "aspect after", "property after", "after", "aspect after", "property after", "in a session");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");
        assertThat(first).isVisible();
        assertThat(second).isHidden();

        press(launcher, "uiFormsWindowActivateSecond");
        assertThat(first).isHidden();
        pressCompleted(launcher, "uiFormsWindowFirstOnceIf");
        marks("after", "aspect after", "property after", "after", "aspect after", "property after", "in a session",
                "not taken");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");
        assertThat(first).isVisible();
        assertThat(second).isHidden();
    }

    // an open that activates the form it finds builds one where the form already there has another label, or is in
    // another window - and reuses the one it built the next time
    @Test
    public void anOpenThatActivatesReusesOnlyTheFormOfItsLabelAndWindow() throws Exception {
        seed();
        Locator launcher = launcher(), keep = window("keep"), one = window("one");
        press(launcher, "uiFormsWindowFirstIntoKeep");
        assertThat(in(keep, "uiFormsWindowFirst")).isVisible();
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");

        press(launcher, "uiFormsWindowFirstOnceLabelled");
        marks("labelled");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "2");
        press(launcher, "uiFormsWindowFirstOnceLabelled");
        marks("labelled", "labelled");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "2");
        assertThat(in(keep, "uiFormsWindowFirst")).hasCount(2);

        press(launcher, "uiFormsWindowFirstOnceIntoOne");
        marks("labelled", "labelled", "into one");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "3");
        assertThat(in(one, "uiFormsWindowFirst")).isVisible();
        press(launcher, "uiFormsWindowFirstOnceIntoOne");
        marks("labelled", "labelled", "into one", "into one");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "3");
        assertThat(in(one, "uiFormsWindowFirst")).hasCount(1);
        assertThat(in(keep, "uiFormsWindowFirst")).hasCount(2);
    }

    // the same open run by the navigator, with nothing open in System.forms - with Ctrl held as well, which changes
    // nothing for an open that always activates (ACTIVATE without USER) (#1843)
    @Test
    public void anOpenFromTheNavigatorActivatesTheFormItFinds() throws Exception {
        seed();
        Locator keep = window("keep"), first = in(keep, "uiFormsWindowFirst"), second = in(keep, "uiFormsWindowSecond");
        openCompleted("uiFormsWindowFirstOnce");
        marks("after", "aspect after");
        assertThat(first).isVisible();
        open(FOLDER, "UiFormsWindow.uiFormsWindowSecondIntoKeep");
        assertThat(first).isHidden();
        openCompleted("uiFormsWindowFirstOnce");
        marks("after", "aspect after", "after", "aspect after");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");
        assertThat(first).isVisible();
        assertThat(second).isHidden();

        open(FOLDER, "UiFormsWindow.uiFormsWindowActivateSecond");
        assertThat(first).isHidden();
        completed("UiFormsWindow.uiFormsWindowFirstOnce", () -> openWithCtrl("UiFormsWindow.uiFormsWindowFirstOnce"));
        marks("after", "aspect after", "after", "aspect after", "after", "aspect after");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");
        assertThat(first).isVisible();
        assertThat(second).isHidden();
        assertThat(first).hasCount(1);
    }

    // two opens that reuse the form already there in one action: the first takes the activation the request carries,
    // the second builds a form, which the client finds a duplicate of - and closes, so that the server's form does not
    // stay open unseen (#1848)
    @Test
    public void aDuplicateTheClientDoesNotShowIsClosed() throws Exception {
        seed();
        Locator launcher = launcher(), keep = window("keep"), first = in(keep, "uiFormsWindowFirst");
        press(launcher, "uiFormsWindowFirstIntoKeep");
        assertThat(first).isVisible();
        press(launcher, "uiFormsWindowFirstTwice");
        check("UiFormsWindow.uiFormsWindowCheckClosed", "1");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "2");
        assertThat(first).hasCount(1);
        assertThat(first).isVisible();
    }

    // a value a React view supplies and an action it runs by name are not activated by the client before the server
    // answers: the open of the form already there builds a form, which the client closes when it arrives
    @Test
    public void aViewsValueAndCallBuildAFormTheClientCloses() throws Exception {
        seed();
        Locator first = in(window("keep"), "uiFormsWindowFirst");
        open(FOLDER, "UiFormsWindow.uiFormsWindowFirstOnce");
        marks("after", "aspect after");
        assertThat(first).isVisible();
        Locator form = open(FOLDER, "UiFormsWindow.uiFormsWindowPush");
        form.locator(".ui-forms-window-push-by-name").click();
        marks("after", "aspect after", "after", "aspect after");
        check("UiFormsWindow.uiFormsWindowCheckClosed", "1");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "2");
        form.locator(".ui-forms-window-push-opener").click();
        check("UiFormsWindow.uiFormsWindowCheckClosed", "2");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "3");
        assertThat(first).hasCount(1);
        close(form);
    }

    // an action that closes its own form and then opens, with ACTIVATE, a form the window already holds: the window
    // draws the form it holds, once - the place of the form closed goes to no second one -, and the form the server
    // built for the open, unseen, is closed (#1848)
    @Test
    public void anOpenThatActivatesAfterItsFormClosesLeavesOneForm() throws Exception {
        seed();
        Locator first = form(FIRST), push = form("UiFormsWindow.uiFormsWindowPush");
        open(FOLDER, "UiFormsWindow.uiFormsWindowFirstAndPushIntoForms");
        assertThat(push).isVisible();
        assertThat(first).isHidden();
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");
        push.locator(".ui-forms-window-push-close-and-show").click();
        check("UiFormsWindow.uiFormsWindowCheckClosed", "1");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "2");
        assertThat(push).hasCount(0);
        assertThat(first).hasCount(1);
        assertThat(first).isVisible();
    }

    // a form whose close event opens a floating form, opened once more by an action a React view runs by name: the
    // client answers the open with the form the window holds and closes the duplicate the server built, unseen, and
    // that close's event runs as any other - the floating form it opens arrives, once, and the mark after it is left,
    // with no error -, the push the unseen close carries taken for the confirmation of no open (#1841, #1848)
    @Test
    public void theCloseEventOfADuplicateClosedUnseenRuns() throws Exception {
        seed();
        Locator source = form("UiFormsWindow.uiFormsWindowCloseSource"), push = form("UiFormsWindow.uiFormsWindowPush");
        Locator target = form("UiFormsWindow.uiFormsWindowCloseTarget");
        open(FOLDER, "UiFormsWindow.uiFormsWindowCloseSourceAndPushIntoForms");
        assertThat(push).isVisible();
        assertThat(source).isHidden();
        push.locator(".ui-forms-window-push-close-source").click();
        marks("query close");
        assertThat(target).hasCount(1);
        assertThat(source).hasCount(1);
        assertThat(source).isVisible();
        assertThat(page.locator(".modal-dialog:not(:has([lsfusion-form]))")).hasCount(0);
        close(source);
        close(push);
    }

    // a form with two handlers of its close event, each opening the form already there with ACTIVATE: each handler
    // takes the activation the request carries and runs on - the form is not built again, both leave their mark -,
    // and the form closes. It is closed by its tab's close button: its Close button closes it with no close event
    @Test
    public void twoCloseHandlersShareOneActivation() throws Exception {
        seed();
        Locator first = in(window("keep"), "uiFormsWindowFirst");
        open(FOLDER, "UiFormsWindow.uiFormsWindowFirstOnce");
        marks("after", "aspect after");
        open(FOLDER, "UiFormsWindow.uiFormsWindowSecondIntoKeep");
        assertThat(first).isHidden();
        Locator events = open(FOLDER, "UiFormsWindow.uiFormsWindowEvents");
        page.locator(".tab-bar .nav-item").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Events$")))
                .locator(".tab-close-button").click();
        marks("after", "aspect after", "first handler", "second handler");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");
        events.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
        assertThat(first).isVisible();
    }

    // a value a React view supplies is not taken for the client's confirmation of a close: a change whose handler
    // closes the form asks about its unsaved changes as the form's Close button does, and the form stays when the user
    // keeps them (#1841)
    @Test
    public void aViewsValueIsNotTakenForTheConfirmationOfAClose() throws Exception {
        seed();
        Locator form = open(FOLDER, "UiFormsWindow.uiFormsWindowPush");
        form.locator(".ui-forms-window-push-stored").click();
        assertThat(save(form)).isEnabled(); // the server knows of the change
        form.locator(".ui-forms-window-push-closer").click();
        answer("No");
        assertThat(form).isVisible();
        form.locator("button[lsfusion-container='PROPERTY(formClose())']").click();
        answer("Yes");
        assertThat(form).hasCount(0);
    }

    // a value a React view supplies to a property whose change handler asks for none reaches the handler, which runs
    // (#1840)
    @Test
    public void aViewsValueReachesAHandlerThatAsksForNone() throws Exception {
        seed();
        Locator form = open(FOLDER, "UiFormsWindow.uiFormsWindowPush");
        form.locator(".ui-forms-window-push-plain").click();
        marks("plain");
        close(form);
    }

    // a value a React view supplies while the user holds Shift - typing a capital letter into a field of the view does
    // - is the value, not a GROUP change: the server has the change, and the form saves it (#1847)
    @Test
    public void aViewsValueWithShiftHeldIsNotAGroupChange() throws Exception {
        seed();
        Locator form = open(FOLDER, "UiFormsWindow.uiFormsWindowPush");
        page.keyboard().down("Shift");
        form.locator(".ui-forms-window-push-stored").click();
        page.keyboard().up("Shift");
        assertThat(save(form)).isEnabled(); // the server has the change
        save(form).click();
        check("UiFormsWindow.uiFormsWindowCheckStored", "changed");
        close(form);
    }

    // ---------------------------------------------------------------- a window a React component draws

    // a window a React component draws closes nothing by itself, though it is declared to close a displaced form at
    // once: the component draws every form it holds
    @Test
    public void aWindowAComponentDrawsClosesNothingByItself() throws Exception {
        Locator launcher = launcher(), board = window("board");
        press(launcher, "uiFormsWindowFirstIntoBoard");
        assertThat(in(board, "uiFormsWindowFirst")).isVisible();
        press(launcher, "uiFormsWindowSecondIntoBoard");
        assertThat(in(board, "uiFormsWindowSecond")).isVisible();
        assertThat(in(board, "uiFormsWindowFirst")).isVisible();
        board(SECOND, FIRST, SECOND);
    }

    // the component is given the forms its window holds - in the order they were opened, the last one current - and
    // places the ones it wants: a form it places nowhere stays open, and is placed again as it was left; its select
    // makes a form the current one, its close closes one
    @Test
    public void aComponentPlacesTheFormsItsWindowHolds() throws Exception {
        seed();
        Locator launcher = launcher(), board = window("board");
        place("current");
        press(launcher, "uiFormsWindowFirstIntoBoard");
        board(FIRST, FIRST);
        // a form placed nowhere waits outside the window, so whether it is open is asked of the whole page
        Locator first = form(FIRST), second = form(SECOND);
        assertThat(in(board, "uiFormsWindowFirst")).isVisible();
        first.evaluate("form => form.dataset.uiFormsWindowLeft = 'first'"); // the element, to see that it comes back

        press(launcher, "uiFormsWindowSecondIntoBoard");
        board(SECOND, FIRST, SECOND);
        assertThat(in(board, "uiFormsWindowSecond")).isVisible();
        assertThat(first).hasCount(1);
        assertThat(first).isHidden();

        tab(board, "uiFormsWindowFirst").locator(".ui-forms-window-board-select").click();
        board(FIRST, FIRST, SECOND);
        assertThat(board.locator("[data-ui-forms-window-left='first']")).isVisible();
        assertThat(second).hasCount(1);
        assertThat(second).isHidden();
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");

        tab(board, "uiFormsWindowFirst").locator(".ui-forms-window-board-close").click();
        board(SECOND, SECOND);
        assertThat(first).hasCount(0);
        assertThat(in(board, "uiFormsWindowSecond")).isVisible();
    }

    // a click into a form the component places, which is not the current one, makes it the current one
    @Test
    public void aClickIntoAFormTheComponentPlacesMakesItCurrent() throws Exception {
        Locator launcher = launcher(), board = window("board");
        press(launcher, "uiFormsWindowFirstIntoBoard");
        press(launcher, "uiFormsWindowSecondIntoBoard");
        board(SECOND, FIRST, SECOND);
        in(board, "uiFormsWindowFirst").locator("[lsfusion-container='PROPERTY(uiFormsWindowText())']").click();
        board(FIRST, FIRST, SECOND);
    }

    // a form a docked-modal child is opened from is blocked in the projection, and the component's close of it is
    // refused, as the standard tab disables its close button, until the child is closed
    @Test
    public void aFormBlockedByItsChildIsNotClosed() throws Exception {
        List<String> console = console();
        Locator launcher = launcher(), board = window("board");
        press(launcher, "uiFormsWindowFirstIntoBoard");
        Locator first = in(board, "uiFormsWindowFirst");
        press(first, "uiFormsWindowChildIntoBoard");
        Locator child = in(board, "uiFormsWindowThird");
        assertThat(child).isVisible();
        Locator opener = tab(board, "uiFormsWindowFirst");
        assertThat(opener).hasClass(Pattern.compile("ui-forms-window-board-blocked"));

        opener.locator(".ui-forms-window-board-close").click();
        waitFor(console, "is blocked by a form opened from it and cannot be closed");
        close(child);
        assertThat(first).isVisible(); // the close refused did not close it
        assertThat(opener).not().hasClass(Pattern.compile("ui-forms-window-board-blocked"));
        opener.locator(".ui-forms-window-board-close").click();
        assertThat(first).hasCount(0);
    }

    // the component's close of all the forms its window holds asks each of them to close, from the last one: the one
    // with unsaved changes asks and stays when the user keeps them, while the ones beside it close; its close of that
    // one asks too
    @Test
    public void aComponentsCloseOfAllAsksTheFormWithUnsavedChanges() throws Exception {
        String[] items = seed();
        Locator launcher = launcher(), board = window("board");
        press(launcher, "uiFormsWindowFirstIntoBoard");
        press(launcher, "uiFormsWindowItemsIntoBoard");
        press(launcher, "uiFormsWindowSecondIntoBoard");
        board(SECOND, FIRST, ITEMS, SECOND);
        Locator form = in(board, "uiFormsWindowItems");
        change(form, quantity(form, items[0]));

        board.locator(".ui-forms-window-board-close-all").click();
        answer("No");
        board(ITEMS, ITEMS);
        check("UiFormsWindow.uiFormsWindowCheckClosed", "1");
        tab(board, "uiFormsWindowItems").locator(".ui-forms-window-board-close").click();
        answer("Yes");
        board("");
        assertThat(form).hasCount(0);
    }

    // what the component cannot place it is told in the place itself: a second place of a form says the first one
    // keeps it, and takes it over once the component takes the first one away; a place of a name no open form has
    // says so
    @Test
    public void aComponentsPlaceThatCannotWorkSaysSo() throws Exception {
        Locator launcher = launcher(), board = window("board");
        press(launcher, "uiFormsWindowFirstIntoBoard");
        place("twice");
        Locator firstPlace = board.locator(".ui-forms-window-board-first"), secondPlace = board.locator(".ui-forms-window-board-second");
        assertThat(in(firstPlace, "uiFormsWindowFirst")).isVisible();
        assertThat(secondPlace.locator(".lsf-view-error")).containsText("already placed by another <Platform>");

        place("second");
        assertThat(in(secondPlace, "uiFormsWindowFirst")).isVisible();
        assertThat(secondPlace.locator(".lsf-view-error")).hasCount(0);

        place("none");
        assertThat(board.locator(".ui-forms-window-board-none .lsf-view-error")).containsText("is not an open form");
    }

    // a component's select of the form its window already draws still moves the keyboard into it from a form of
    // another window
    @Test
    public void aComponentsSelectMovesTheKeyboardIntoTheFormItDraws() throws Exception {
        Locator launcher = launcher(), board = window("board");
        press(launcher, "uiFormsWindowFirstIntoBoard");
        Locator first = in(board, "uiFormsWindowFirst");
        assertThat(first).isVisible();
        launcher.locator("[lsfusion-container='PROPERTY(uiFormsWindowText())']").click();
        assertThat(launcher.locator(":focus")).hasCount(1);
        tab(board, "uiFormsWindowFirst").locator(".ui-forms-window-board-select").click();
        assertThat(first.locator(":focus")).hasCount(1);
    }

    // the component chooses the edit mode the platform's mode button chooses, among the modes it is given, and is given
    // the mode chosen; it is given the caption of a form as the form's data changes it
    @Test
    public void aComponentChoosesTheEditModeAndFollowsACaption() throws Exception {
        Locator launcher = launcher(), board = window("board");
        press(launcher, "uiFormsWindowSecondIntoBoard");
        Locator mode = board.locator(".ui-forms-window-board-edit-mode");
        assertThat(mode).hasText("DEFAULT");
        assertThat(board.locator(".ui-forms-window-board-mode")).hasText(new String[] {"Default Mode", "Link Mode", "Dialog Mode", "Group Change Mode"});
        String[] modes = {"LINK", "DIALOG", "GROUPCHANGE", "DEFAULT"};
        String[] captions = {"Link Mode", "Dialog Mode", "Group Change Mode", "Default Mode"};
        for (int i = 0; i < modes.length; i++) {
            board.locator(".ui-forms-window-board-mode[data-mode='" + modes[i] + "']").click();
            assertThat(mode).hasText(modes[i]);
            assertThat(page.locator("button[title='" + captions[i] + "']")).hasCount(1);
        }

        Locator caption = tab(board, "uiFormsWindowSecond").locator(".ui-forms-window-board-select");
        assertThat(caption).hasText("Second");
        edit(in(board, "uiFormsWindowSecond").locator("[lsfusion-container='PROPERTY(uiFormsWindowText())']"), "ab");
        assertThat(caption).hasText("Second ab");
    }

    // the component's full screen and the platform's key for it (ALT+F11) undo each other, either way round: in full
    // screen the page draws System.forms alone - no navigator, no other window, the board neither -, and once it is
    // over the board is drawn again, its component given the forms as they were and full screen off
    @Test
    public void aComponentsFullScreenAndTheKeyUndoEachOther() throws Exception {
        Locator launcher = launcher(), board = window("board");
        Locator navigator = page.locator("[lsfusion-container='System.root']");
        press(launcher, "uiFormsWindowFirstIntoBoard");
        board(FIRST, FIRST);
        fullScreen(false);

        board.locator(".ui-forms-window-board-full-screen").click();
        assertThat(navigator).hasCount(0);
        assertThat(board).hasCount(0);
        assertThat(launcher).isVisible();
        forget();
        idle();
        page.keyboard().press("Alt+F11");
        assertThat(navigator).isVisible();
        assertThat(in(board, "uiFormsWindowFirst")).isVisible();
        board(FIRST, FIRST);
        fullScreen(false);

        idle();
        page.keyboard().press("Alt+F11");
        assertThat(navigator).hasCount(0);
        assertThat(board).hasCount(0);
        forget();
        page.evaluate("() => window.uiFormsWindowBoard.controller.toggleFullScreen()");
        assertThat(navigator).isVisible();
        assertThat(in(board, "uiFormsWindowFirst")).isVisible();
        board(FIRST, FIRST);
        fullScreen(false);
    }

    // ---------------------------------------------------------------- helpers

    private Locator launcher() {
        return open(FOLDER, "UiFormsWindow.uiFormsWindowLauncher");
    }

    // two items under names no other attempt has - what a failed attempt saved stays in the database for its rerun -,
    // the first and the second of the forms; what the server counts and marks starts over, the stored value none
    private static String[] seed() throws Exception {
        String[] items = {"First " + System.nanoTime(), "Second " + System.nanoTime()};
        exec("UiFormsWindow.uiFormsWindowSeed", items);
        return items;
    }

    // what the actions marked since the seed, in turn
    private static void marks(String... marks) throws Exception {
        check("UiFormsWindow.uiFormsWindowCheckMarks", String.join(",", marks));
    }

    // one of the FORMS windows, by its class: one, keep, board
    private Locator window(String name) {
        return page.locator(".ui-forms-window-" + name);
    }

    // a form of the module inside a window or a form
    private static Locator in(Locator scope, String form) {
        return scope.locator("[lsfusion-form='UiFormsWindow." + form + "']");
    }

    // the button of an action drawn on a form, by its name there
    private static void press(Locator form, String action) {
        form.locator("button[lsfusion-container='PROPERTY(" + action + "())']").click();
    }

    // the button of an action pressed, and its request answered: what the window draws then is what the server's
    // answer leaves, not only what the client predicted
    private void pressCompleted(Locator form, String action) {
        completed("ExecuteEventAction", () -> press(form, action));
    }

    // ... and an element of the navigator
    private void openCompleted(String action) {
        completed("UiFormsWindow." + action, () -> open(FOLDER, "UiFormsWindow." + action));
    }

    // the click, then until the answer to the request whose body names what is given has arrived, and the page has run
    // to its next frame
    private void completed(String request, Runnable click) {
        page.waitForResponse(response -> response.request().postData() != null
                && response.request().postData().contains(request), click).finished();
        page.evaluate("() => new Promise(resolve => requestAnimationFrame(resolve))");
    }

    // until no request blocks the input, which the client drops while one does
    private void idle() {
        page.locator(".lsf-blocking").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    // what the board's component was given last forgotten - its controller kept -, to see it given it again
    private void forget() {
        page.evaluate("() => { window.uiFormsWindowBoard.data = null; }");
    }

    // until the board's component is given the full screen mode expected
    private void fullScreen(boolean expected) {
        page.waitForFunction("expected => window.uiFormsWindowBoard.data.fullScreen === expected", expected);
    }

    private static Locator save(Locator form) {
        return form.locator("button[lsfusion-container='PROPERTY(formApply())']");
    }

    // the cell of an item's quantity in a grid of items
    private Locator quantity(Locator form, String item) {
        return row(form, "uiFormsWindowName(i)", item).locator("td[lsfusion-container='PROPERTY(uiFormsWindowQuantity(i))']");
    }

    // a change of a form's stored data - an item's quantity - which the form asks about before it closes: drawn at
    // once, and known to the server once the form's Save is enabled
    private void change(Locator form, Locator quantity) {
        edit(quantity, "5");
        assertThat(quantity).hasText("5");
        assertThat(save(form)).isEnabled();
    }

    private static Locator text(Locator scope, String text) {
        return scope.getByText(text, new Locator.GetByTextOptions().setExact(true));
    }

    // the question a form with unsaved changes asks before it closes, answered
    private void answer(String option) {
        Locator question = page.locator(".modal-dialog").filter(new Locator.FilterOptions().setHasText("Do you really want close the form"));
        question.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(option).setExact(true)).click();
        assertThat(question).hasCount(0);
    }

    // a navigator element clicked with Ctrl held, its folder unfolded first as open does
    private void openWithCtrl(String element) {
        Locator item = page.locator("[lsfusion-container='" + element + "']");
        try {
            assertThat(item).isInViewport(new LocatorAssertions.IsInViewportOptions().setTimeout(1));
        } catch (AssertionError e) {
            page.locator("[lsfusion-container='" + FOLDER + "']").click();
            assertThat(item).isInViewport();
        }
        item.click(new Locator.ClickOptions().setModifiers(Collections.singletonList(KeyboardModifier.CONTROL)));
        page.mouse().move(640, 400);
    }

    // the board's tab of a form
    private static Locator tab(Locator board, String form) {
        return board.locator(".ui-forms-window-board-tab[data-form='UiFormsWindow." + form + "']");
    }

    // what the board places from now on, see UiFormsWindow.js
    private void place(String what) {
        page.waitForFunction("() => !!window.uiFormsWindowBoard");
        page.evaluate("what => window.uiFormsWindowBoard.place(what)", what);
    }

    // until the board's component is given exactly the forms named - by their canonical names, in the order given,
    // each once - and the one named current and no other, failing with what it is given then
    private void board(String current, String... open) {
        try {
            page.waitForFunction("([open, current]) => { const data = window.uiFormsWindowBoard && window.uiFormsWindowBoard.data;"
                    + " if (!data) return false;"
                    + " const names = list => list.map(name => data.byName[name] && data.byName[name].canonicalName).join();"
                    + " return Object.keys(data.byName).sort().join() === data.open.slice().sort().join() && names(data.open) === open"
                    + " && names(data.open.filter(name => data.byName[name].selected)) === current; }",
                    Arrays.asList(String.join(",", open), current), new Page.WaitForFunctionOptions().setTimeout(10_000));
        } catch (PlaywrightException e) {
            throw new AssertionError("the board is given " + page.evaluate("() => JSON.stringify(window.uiFormsWindowBoard && window.uiFormsWindowBoard.data)")
                    + ", not " + Arrays.toString(open) + " with " + current + " current", e);
        }
    }

    // what the page logs from now on
    private List<String> console() {
        List<String> messages = new ArrayList<>();
        page.onConsoleMessage(message -> messages.add(message.text()));
        return messages;
    }

    private void waitFor(List<String> console, String text) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (console.stream().noneMatch(message -> message.contains(text))) {
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError("the page never logged '" + text + "', it logged " + console);
            page.waitForTimeout(100);
        }
    }
}
