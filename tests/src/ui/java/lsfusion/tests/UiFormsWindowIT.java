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
        action(launcher, "uiFormsWindowFirstIntoOne").click();
        assertThat(in(one, "uiFormsWindowFirst")).isVisible();
        action(launcher, "uiFormsWindowSecondIntoOne").click();
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
        action(launcher, "uiFormsWindowBothIntoOne").click();
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
        action(launcher, "uiFormsWindowItemsIntoOne").click();
        Locator form = in(one, "uiFormsWindowItems"), quantity = quantity(form, items[0]);
        change(form, quantity);

        action(launcher, "uiFormsWindowFirstIntoOne").click();
        answer("No");
        assertThat(in(one, "uiFormsWindowFirst")).isVisible();
        assertThat(form).isHidden();

        action(launcher, "uiFormsWindowActivateItems").click();
        assertThat(form).isVisible();
        assertThat(quantity).hasText("5");
        action(launcher, "uiFormsWindowCloseItems").click();
        answer("Yes");
        assertThat(form).hasCount(0);
    }

    // a form that refused to close - the user kept its unsaved changes - is asked again by the next CLOSE FORM of its
    // label (#1849)
    @Test
    public void aFormThatRefusedToCloseIsAskedAgain() throws Exception {
        String[] items = seed();
        Locator launcher = launcher(), keep = window("keep");
        action(launcher, "uiFormsWindowKeptIntoKeep").click();
        Locator form = in(keep, "uiFormsWindowItems");
        change(form, quantity(form, items[0]));

        action(launcher, "uiFormsWindowCloseKept").click();
        answer("No");
        assertThat(form).isVisible();
        action(launcher, "uiFormsWindowCloseKept").click();
        answer("Yes");
        assertThat(form).hasCount(0);
    }

    // a window that closes nothing (NOCLOSE) keeps the form it displaced, hidden, and draws it again once the form
    // that took its place is closed (#1842)
    @Test
    public void aWindowThatClosesNothingKeepsWhatItDisplaced() throws Exception {
        Locator launcher = launcher(), keep = window("keep");
        action(launcher, "uiFormsWindowFirstIntoKeep").click();
        assertThat(in(keep, "uiFormsWindowFirst")).isVisible();
        action(launcher, "uiFormsWindowSecondIntoKeep").click();
        assertThat(in(keep, "uiFormsWindowSecond")).isVisible();
        assertThat(in(keep, "uiFormsWindowFirst")).isHidden();
        action(launcher, "uiFormsWindowCloseSecond").click();
        assertThat(in(keep, "uiFormsWindowSecond")).hasCount(0);
        assertThat(in(keep, "uiFormsWindowFirst")).isVisible();
    }

    // ACTIVATE FORM run by a form's own action brings the form it names forward, as the navigator's does (#1845)
    @Test
    public void activateFormFromAFormBringsItForward() throws Exception {
        Locator launcher = launcher(), keep = window("keep");
        action(launcher, "uiFormsWindowFirstIntoKeep").click();
        action(launcher, "uiFormsWindowSecondIntoKeep").click();
        assertThat(in(keep, "uiFormsWindowFirst")).isHidden();
        action(launcher, "uiFormsWindowActivateFirst").click();
        assertThat(in(keep, "uiFormsWindowFirst")).isVisible();
        assertThat(in(keep, "uiFormsWindowSecond")).isHidden();
    }

    // two instances of one form are told apart by their labels: ACTIVATE FORM and CLOSE FORM of a label reach that
    // instance alone, and CLOSE FORM of the form's name every instance (#1844)
    @Test
    public void aFormIsAddressedByItsLabel() throws Exception {
        String[] items = seed();
        Locator launcher = launcher(), keep = window("keep");
        action(launcher, "uiFormsWindowLabelledIntoKeep").click();
        Locator forms = in(keep, "uiFormsWindowItem");
        assertThat(forms).hasCount(2);
        assertThat(text(keep, items[1])).isVisible();
        assertThat(text(keep, items[0])).isHidden();

        action(launcher, "uiFormsWindowActivateA").click();
        assertThat(text(keep, items[0])).isVisible();
        assertThat(text(keep, items[1])).isHidden();
        action(launcher, "uiFormsWindowCloseA").click();
        assertThat(forms).hasCount(1);
        assertThat(text(keep, items[1])).isVisible();
        check("UiFormsWindow.uiFormsWindowCheckItemClosed", "1,0");

        action(launcher, "uiFormsWindowAIntoKeep").click();
        assertThat(forms).hasCount(2);
        assertThat(text(keep, items[0])).isVisible();
        action(launcher, "uiFormsWindowCloseEveryItem").click();
        assertThat(forms).hasCount(0);
        check("UiFormsWindow.uiFormsWindowCheckItemClosed", "2,1");
    }

    // a docked-modal child opened into the window its opener is in keeps the opener, though the window closes a form
    // it displaces at once, and the window draws the opener again once the child is closed
    @Test
    public void aDockedModalChildKeepsItsOpener() throws Exception {
        Locator launcher = launcher(), one = window("one");
        action(launcher, "uiFormsWindowFirstIntoOne").click();
        Locator first = in(one, "uiFormsWindowFirst");
        action(first, "uiFormsWindowChildIntoOne").click();
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
        action(launcher, "uiFormsWindowFirstIntoOne").click();
        Locator first = in(one, "uiFormsWindowFirst");
        assertThat(first).isVisible();
        action(launcher, "uiFormsWindowSecondIntoOneWait").click();
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
        action(launcher, "uiFormsWindowSecondIntoKeep").click();
        action(launcher, "uiFormsWindowFirstIntoKeep").click();
        Locator first = in(keep, "uiFormsWindowFirst"), second = in(keep, "uiFormsWindowSecond");
        assertThat(second).isHidden();
        action(first, "uiFormsWindowChildIntoKeep").click();
        Locator child = in(keep, "uiFormsWindowThird");
        assertThat(child).isVisible();
        action(launcher, "uiFormsWindowCloseFirst").click();
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

        action(launcher, "uiFormsWindowSecondIntoKeep").click();
        assertThat(first).isHidden();
        pressCompleted(launcher, "uiFormsWindowFirstOnce");
        marks("after", "aspect after", "property after", "after", "aspect after", "property after");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");
        assertThat(first).isVisible();
        assertThat(second).isHidden();
        assertThat(first).hasCount(1);

        action(launcher, "uiFormsWindowActivateSecond").click();
        assertThat(first).isHidden();
        pressCompleted(launcher, "uiFormsWindowFirstOnceInSession");
        marks("after", "aspect after", "property after", "after", "aspect after", "property after", "in a session");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");
        assertThat(first).isVisible();
        assertThat(second).isHidden();

        action(launcher, "uiFormsWindowActivateSecond").click();
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
        action(launcher, "uiFormsWindowFirstIntoKeep").click();
        assertThat(in(keep, "uiFormsWindowFirst")).isVisible();
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "1");

        action(launcher, "uiFormsWindowFirstOnceLabelled").click();
        marks("labelled");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "2");
        action(launcher, "uiFormsWindowFirstOnceLabelled").click();
        marks("labelled", "labelled");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "2");
        assertThat(in(keep, "uiFormsWindowFirst")).hasCount(2);

        action(launcher, "uiFormsWindowFirstOnceIntoOne").click();
        marks("labelled", "labelled", "into one");
        check("UiFormsWindow.uiFormsWindowCheckBuilt", "3");
        assertThat(in(one, "uiFormsWindowFirst")).isVisible();
        action(launcher, "uiFormsWindowFirstOnceIntoOne").click();
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
        action(launcher, "uiFormsWindowFirstIntoKeep").click();
        assertThat(first).isVisible();
        action(launcher, "uiFormsWindowFirstTwice").click();
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
    // that close's event runs as any other - the floating form it opens is shown, once, and the mark after it is left,
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
        assertThat(target).isVisible();
        assertThat(source).hasCount(1);
        assertThat(source).isVisible();
        assertThat(page.locator(".modal-dialog:not(:has([lsfusion-form]))")).hasCount(0);
        close(target);
        close(source);
        close(push);
    }

    // a floating form opened without waiting for it is shown, with no backdrop: the page around it stays in use - the
    // launcher under it opens a form as before
    @Test
    public void aFloatingFormNotWaitedForIsShown() throws Exception {
        seed();
        Locator launcher = launcher(), one = window("one"), target = form("UiFormsWindow.uiFormsWindowCloseTarget");
        action(launcher, "uiFormsWindowCloseTargetFloating").click();
        assertThat(target).isVisible();
        action(launcher, "uiFormsWindowFirstIntoOne").click();
        assertThat(in(one, "uiFormsWindowFirst")).isVisible();
        assertThat(target).isVisible();
        close(target);
    }

    // two floating forms not waited for, opened one after the other from the same form, the launcher under them: the
    // later one comes on top of the earlier one, the keyboard in it. A window the server waits for keeps under it the
    // windows its opener's later requests open - those requests run only once it is closed -; one not waited for holds
    // no request back, yet it kept the later one under it as well
    @Test
    public void aFloatingFormOpenedLaterFromTheSameFormComesOnTop() throws Exception {
        Locator launcher = launcher(), third = form("UiFormsWindow.uiFormsWindowThird"), second = form(SECOND);
        action(launcher, "uiFormsWindowThirdFloating").click();
        assertThat(third).isVisible();
        action(launcher, "uiFormsWindowSecondFloating").click();
        assertThat(second).isVisible();
        onTop(second);
        assertThat(second.locator(":focus")).hasCount(1);
        close(second);
        close(third);
    }

    // three floating forms not waited for, each opened from the one under it, closed out of the order they were opened
    // in - the middle one first, by the upper one's CLOSE FORM: the upper one keeps the keyboard while it is open, then
    // hands it to the lower one, past the middle one, and the lower one to the launcher they were opened from, whose
    // key runs
    @Test
    public void floatingFormsClosedOutOfOrderGiveTheKeyboardBack() throws Exception {
        seed();
        Locator launcher = launcher(), lower = form("UiFormsWindow.uiFormsWindowLower"),
                middle = form("UiFormsWindow.uiFormsWindowMiddle"), upper = form("UiFormsWindow.uiFormsWindowUpper");
        action(launcher, "uiFormsWindowLowerFloating").click();
        assertThat(lower).isVisible();
        action(lower, "uiFormsWindowMiddleFloating").click();
        assertThat(middle).isVisible();
        action(middle, "uiFormsWindowUpperFloating").click();
        assertThat(upper).isVisible();

        action(upper, "uiFormsWindowCloseMiddle").click();
        assertThat(middle).hasCount(0);
        assertThat(upper.locator(":focus")).hasCount(1);

        close(upper);
        assertThat(lower.locator(":focus")).hasCount(1);
        close(lower);
        assertThat(launcher.locator(":focus")).hasCount(1);
        press("F6");
        marks("launcher key");
    }

    // a floating form not waited for whose opener - the launcher - is closed under it: once the floating form is closed
    // too, the keyboard goes to the form System.forms shows then, the launcher being gone
    @Test
    public void aFloatingFormWhoseOpenerClosedGivesTheKeyboardToTheFormShown() throws Exception {
        Locator launcher = launcher(), lower = form("UiFormsWindow.uiFormsWindowLower"), second = form(SECOND);
        action(launcher, "uiFormsWindowSecondIntoForms").click();
        assertThat(second).isVisible();
        page.locator(".tab-bar .nav-item").filter(new Locator.FilterOptions().setHasText(Pattern.compile("^Launcher$"))).click();
        assertThat(launcher).isVisible();
        action(launcher, "uiFormsWindowLowerFloating").click();
        assertThat(lower).isVisible();

        action(lower, "uiFormsWindowCloseLauncher").click();
        assertThat(launcher).hasCount(0);
        assertThat(second).isVisible();
        close(lower);
        assertThat(second.locator(":focus")).hasCount(1);
    }

    // a floating form not waited for whose answer comes after the client's placeholder - the window the client shows
    // for an open it predicts, 100 ms after the click, which the form then arrives in -: the placeholder and the form
    // in it are a window not waited for - no backdrop, the page around it in use, a floating form the launcher under it
    // opens then coming on top of it. The placeholder was built as a window waited for, which the form kept
    @Test
    public void aFloatingFormNotWaitedForAnsweredLateLeavesThePageInUse() throws Exception {
        Locator launcher = launcher(), target = form("UiFormsWindow.uiFormsWindowCloseTarget"), second = form(SECOND);
        Locator placeholder = page.locator(".modal-form:not(:has([lsfusion-form]))");
        hold();
        action(launcher, "uiFormsWindowCloseTargetFloating").click();
        assertThat(placeholder.locator(".modal-dialog")).isVisible();
        assertThat(placeholder.locator("> .modal-backdrop")).hasCount(0);
        release();
        assertThat(target).isVisible();
        Locator window = page.locator(".modal-form").filter(new Locator.FilterOptions().setHas(target));
        assertThat(window.locator("> .modal-backdrop")).hasCount(0);
        assertThat(window).hasClass(Pattern.compile("\\bmodal-nowait\\b"));
        action(launcher, "uiFormsWindowSecondFloating").click();
        assertThat(second).isVisible();
        onTop(second);
        close(second);
        close(target);
    }

    // a floating form that comes in the placeholder of another branch of its action - the client predicts the first
    // branch when two branches open two forms, and an open waited for when they open one form both ways -: its window is
    // its own all the same, with a backdrop if the form is waited for and with none if not
    @Test
    public void aFloatingFormHasABackdropIfWaitedForWhicheverBranchWasPredicted() throws Exception {
        Locator launcher = launcher(), target = form("UiFormsWindow.uiFormsWindowCloseTarget");
        Locator placeholder = page.locator(".modal-form:not(:has([lsfusion-form]))");
        Locator window = page.locator(".modal-form").filter(new Locator.FilterOptions().setHas(target));

        // predicted: Third not waited for; opened: Close target waited for
        hold();
        action(launcher, "uiFormsWindowBranchesOfTwoForms").click();
        assertThat(placeholder.locator(".modal-dialog")).isVisible();
        assertThat(placeholder.locator("> .modal-backdrop")).hasCount(0);
        release();
        assertThat(target).isVisible();
        assertThat(window.locator("> .modal-backdrop")).hasCount(1);
        assertThat(window).not().hasClass(Pattern.compile("\\bmodal-nowait\\b"));
        close(target);

        // predicted: Close target waited for, the branches merged; opened: Close target not waited for
        hold();
        action(launcher, "uiFormsWindowBranchesOfOneForm").click();
        assertThat(placeholder.locator(".modal-dialog")).isVisible();
        assertThat(placeholder.locator("> .modal-backdrop")).hasCount(1);
        release();
        assertThat(target).isVisible();
        assertThat(window.locator("> .modal-backdrop")).hasCount(0);
        assertThat(window).hasClass(Pattern.compile("\\bmodal-nowait\\b"));
        close(target);
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
        action(launcher, "uiFormsWindowFirstIntoBoard").click();
        assertThat(in(board, "uiFormsWindowFirst")).isVisible();
        action(launcher, "uiFormsWindowSecondIntoBoard").click();
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
        action(launcher, "uiFormsWindowFirstIntoBoard").click();
        board(FIRST, FIRST);
        // a form placed nowhere waits outside the window, so whether it is open is asked of the whole page
        Locator first = form(FIRST), second = form(SECOND);
        assertThat(in(board, "uiFormsWindowFirst")).isVisible();
        first.evaluate("form => form.dataset.uiFormsWindowLeft = 'first'"); // the element, to see that it comes back

        action(launcher, "uiFormsWindowSecondIntoBoard").click();
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
        action(launcher, "uiFormsWindowFirstIntoBoard").click();
        action(launcher, "uiFormsWindowSecondIntoBoard").click();
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
        action(launcher, "uiFormsWindowFirstIntoBoard").click();
        Locator first = in(board, "uiFormsWindowFirst");
        action(first, "uiFormsWindowChildIntoBoard").click();
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
        action(launcher, "uiFormsWindowFirstIntoBoard").click();
        action(launcher, "uiFormsWindowItemsIntoBoard").click();
        action(launcher, "uiFormsWindowSecondIntoBoard").click();
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
        action(launcher, "uiFormsWindowFirstIntoBoard").click();
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
        action(launcher, "uiFormsWindowFirstIntoBoard").click();
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
        action(launcher, "uiFormsWindowSecondIntoBoard").click();
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
    // screen the page draws System.forms alone - no navigator, no other window, the board neither: hidden, its
    // component kept and given the forms all the same, with full screen off, which is System.forms' -, and once it is
    // over the board is drawn again, its component given the forms as they were and full screen off
    @Test
    public void aComponentsFullScreenAndTheKeyUndoEachOther() throws Exception {
        Locator launcher = launcher(), board = window("board");
        Locator navigator = page.locator("[lsfusion-container='System.root']");
        action(launcher, "uiFormsWindowFirstIntoBoard").click();
        board(FIRST, FIRST);
        fullScreen(false);

        forget();
        board.locator(".ui-forms-window-board-full-screen").click();
        assertThat(navigator).isHidden();
        assertThat(board).isHidden();
        assertThat(launcher).isVisible();
        board(FIRST, FIRST);
        fullScreen(false);
        forget();
        idle();
        page.keyboard().press("Alt+F11");
        assertThat(navigator).isVisible();
        assertThat(in(board, "uiFormsWindowFirst")).isVisible();
        board(FIRST, FIRST);
        fullScreen(false);

        idle();
        page.keyboard().press("Alt+F11");
        assertThat(navigator).isHidden();
        assertThat(board).isHidden();
        forget();
        page.evaluate("() => window.uiFormsWindowBoard.controller.toggleFullScreen()");
        assertThat(navigator).isVisible();
        assertThat(in(board, "uiFormsWindowFirst")).isVisible();
        board(FIRST, FIRST);
        fullScreen(false);
    }

    // the React view of a form in System.forms through the full screen, which draws System.forms alone, and back: the
    // same view all along - what it counted in its own state stays, and its effect does not run again
    @Test
    public void theFullScreenKeepsAViewInSystemForms() throws Exception {
        Locator launcher = launcher(), counter = form("UiFormsWindow.uiFormsWindowCounter").locator(".ui-forms-window-counter");
        Locator navigator = page.locator("[lsfusion-container='System.root']");
        action(launcher, "uiFormsWindowCounterIntoForms").click();
        counter.click();
        assertThat(counter).hasText("Clicked 1");
        mounts(1);

        idle();
        page.keyboard().press("Alt+F11");
        assertThat(navigator).isHidden();
        assertThat(counter).hasText("Clicked 1");
        idle();
        page.keyboard().press("Alt+F11");
        assertThat(navigator).isVisible();
        assertThat(counter).hasText("Clicked 1");
        mounts(1);
    }

    // the grid of a form in System.forms through the full screen and back: given the whole page, it asks for the rows
    // that fit there, and for the ones that fit its window again after - the full screen's layer passes the page's
    // resizes on to System.forms' view, which the hidden layout passes none
    @Test
    public void theFullScreenResizesTheFormsOfSystemForms() throws Exception {
        String[] items = seed();
        Locator launcher = launcher(), navigator = page.locator("[lsfusion-container='System.root']");
        List<String> pageSizes = requests("ChangePageSize");
        action(launcher, "uiFormsWindowItemsIntoForms").click();
        assertThat(text(form(ITEMS), items[0])).isVisible();
        page.waitForTimeout(1000); // the page size the grid asks for as it opens, sent 100 ms after it is measured

        int opened = pageSizes.size();
        idle();
        page.keyboard().press("Alt+F11");
        assertThat(navigator).isHidden();
        waitForSent(pageSizes, opened + 1);
        idle();
        page.keyboard().press("Alt+F11");
        assertThat(navigator).isVisible();
        waitForSent(pageSizes, opened + 2);
    }

    // the component of another forms window through the full screen, which does not draw that window, and back: the
    // same component all along - what it chose to place, in its own state, stays chosen
    @Test
    public void theFullScreenKeepsTheComponentOfAnotherWindow() throws Exception {
        Locator launcher = launcher(), board = window("board");
        action(launcher, "uiFormsWindowFirstIntoBoard").click();
        action(launcher, "uiFormsWindowSecondIntoBoard").click();
        board(SECOND, FIRST, SECOND);
        place("current");
        assertThat(in(board, "uiFormsWindowFirst")).isHidden();

        idle();
        page.keyboard().press("Alt+F11");
        assertThat(board).isHidden();
        idle();
        page.keyboard().press("Alt+F11");
        assertThat(in(board, "uiFormsWindowSecond")).isVisible();
        assertThat(in(board, "uiFormsWindowFirst")).isHidden();
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

    // a click into the form reaches it - no other window is over it -, as playwright's checks before a click tell
    private static void onTop(Locator form) {
        try {
            form.click(new Locator.ClickOptions().setTrial(true).setTimeout(5_000));
        } catch (PlaywrightException e) {
            throw new AssertionError("another window is over " + form, e);
        }
    }

    // the button of an action pressed, and its request answered: what the window draws then is what the server's
    // answer leaves, not only what the client predicted
    private void pressCompleted(Locator form, String action) {
        completed("ExecuteEventAction", () -> action(form, action).click());
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

    // what the board's component was given last forgotten - its controller kept -, to see it given it again
    private void forget() {
        page.evaluate("() => { window.uiFormsWindowBoard.data = null; }");
    }

    // until the board's component is given the full screen mode expected
    private void fullScreen(boolean expected) {
        page.waitForFunction("expected => window.uiFormsWindowBoard.data.fullScreen === expected", expected);
    }

    // until the counter's view has been drawn anew - mounted - the number of times expected, failing with how many
    private void mounts(int expected) {
        try {
            page.waitForFunction("expected => window.uiFormsWindowCounterMounts === expected", expected,
                    new Page.WaitForFunctionOptions().setTimeout(5_000));
        } catch (PlaywrightException e) {
            throw new AssertionError("the counter's view was mounted " + page.evaluate("() => window.uiFormsWindowCounterMounts")
                    + " times, not " + expected, e);
        }
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

    // until that many requests of the kind are sent, failing with how many were
    private void waitForSent(List<String> requests, int count) {
        long deadline = System.currentTimeMillis() + 10_000;
        while (requests.size() < count) {
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError(requests.size() + " requests sent, not " + count);
            page.waitForTimeout(100);
        }
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
