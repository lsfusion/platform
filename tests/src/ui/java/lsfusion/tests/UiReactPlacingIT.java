package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.BoundingBox;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// what a React view places of what the platform draws, by the name it reads the child's entry by (UiReactPlacing.lsf,
// its views in UiReactPlacing.js): a panel property, an action, a grid property per row, a container; what a placement
// that cannot work shows; what a placed child shows and hides by; the labels of what the view does not place; and a
// view on a page of the platform's tab strip, in a window, under a SHOWIF of a container it draws, in a `tabbed`
// container it draws
public class UiReactPlacingIT extends UiTest {

    // a panel property and an action the platform draws are placed by their names - the property's host showing its
    // value, both marked with the class lsf-platform, merged with the view's own, and with the name -, and a grid
    // property NOCUSTOM once per row, each host holding that row's value; the property's SHOWIF hides its host, which
    // shows the property again once it is shown
    @Test
    public void aChildThePlatformDrawsIsPlacedByItsName() throws Exception {
        String note = seed();
        Locator form = open("UiReactPlacing.uiRpl", "UiReactPlacing.uiRplBoard");
        Locator noteHost = form.locator(".ui-rpl-note");
        assertShows(noteHost, note);
        assertThat(noteHost).hasAttribute("data-lsf-sid", "c.note");
        assertThat(noteHost).containsClass("lsf-platform ui-rpl-note ui-rpl-a");
        Locator toggle = form.locator(".ui-rpl-toggle");
        assertThat(toggle).hasAttribute("data-lsf-sid", "uiRplToggleNote");
        assertThat(toggle.locator("button")).hasText("Toggle note");
        assertQuantities(form);

        toggle.locator("button").click();
        assertThat(noteHost).isHidden();
        toggle.locator("button").click();
        assertThat(noteHost).isVisible();
        assertShows(noteHost, note);
        close(form);
    }

    // a placement that cannot work says so in its host, in a span of the class lsf-view-error, the console having the
    // whole reason, and the host keeps its marks and its class, itself never of that class: a name that names nothing
    // in the container, a property drawn per row given no row, a row for what has one view, a property the view draws
    // itself, a column the view draws, a row that is no row - while the hosts placed right keep their views. Naming
    // nothing takes the views out of the hosts that had them - a <Platform>'s and a usePlatform's -, keeping them out
    // of sight; the message is a child of the host, so a render that changes the host's class keeps it, and naming the
    // child right takes that span away alone - not a child of the view's own that carries the same class -, placing
    // the same view again, which fills its host, a render that changes the host's class keeping it so
    @Test
    public void aPlacementThatCannotWorkSaysSoInItsHost() throws Exception {
        String note = seed();
        List<String> errors = pageErrors();
        Locator form = open("UiReactPlacing.uiRpl", "UiReactPlacing.uiRplBoard");
        Locator noteHost = form.locator(".ui-rpl-note"), own = form.locator(".ui-rpl-own-host");
        assertShows(noteHost, note);
        assertShows(own, "memo");
        assertNotMarked(noteHost, own);
        // the platform's views in the hosts: the note's, and the memo's beside the view's own child
        page.evaluate("() => { window.uiRplNoteView = document.querySelector('.ui-rpl-note').firstElementChild;"
                + " window.uiRplMemoView = Array.from(document.querySelector('.ui-rpl-own-host').children)"
                + ".find(child => !child.classList.contains('ui-rpl-own')); }");
        waitFor("window.uiRplMemoView.contains(document.querySelector('.ui-rpl-own-host input'))");

        click(form, "mistakes");
        assertMistake(noteHost, "'c.nope' is not inside 'uiRplBoardView'");
        assertThat(noteHost).hasAttribute("data-lsf-sid", "c.nope");
        assertThat(noteHost).containsClass("lsf-platform ui-rpl-note ui-rpl-a");
        assertThat(noteHost.locator("input")).hasCount(0);
        assertMistake(form.locator(".ui-rpl-no-row"), "'i.qty' is drawn per row, so its <Platform> needs a row");
        assertMistake(form.locator(".ui-rpl-one-view"), "'c.note' is not a grid property this view places per row");
        assertMistake(form.locator(".ui-rpl-drawn"), "'c.cCode' has no view of its own");
        assertMistake(form.locator(".ui-rpl-column"), "'i.iName' is not inside 'uiRplBoardView'");
        assertMistake(form.locator(".ui-rpl-not-a-row"), "the `row` given to 'i.qty' is not a row");
        assertMistake(own, "'uiRplNothing' is not inside 'uiRplBoardView'");
        assertThat(own.locator("b.ui-rpl-own")).hasText("mine");
        assertNotMarked(noteHost, own);
        // the console has every complete reason and no other error
        assertLogged(errors,
                "lsFusion custom view: component 'c.nope' is not inside container 'uiRplBoardView', nor in a container"
                        + " it draws itself",
                "lsFusion custom view: component 'uiRplNothing' is not inside container 'uiRplBoardView', nor in a"
                        + " container it draws itself",
                "lsFusion custom view: 'i.qty' is a property the platform draws per row: its <Platform> is given the row"
                        + " it belongs to - <Platform name=\"i.qty\" row={row}/> -, since each row has a renderer of its own",
                "lsFusion custom view: 'c.note' is given a row, but it names no grid property the platform draws of a"
                        + " group the React view of 'uiRplBoardView' draws - group, then integration name, as its column"
                        + " entry is keyed -, so nothing is drawn per row for it here",
                "lsFusion custom view: 'c.cCode' inside container 'uiRplBoardView' has no view of its own, so the view"
                        + " draws it from `data`: give it `custom = NULL` for the platform to draw it, and place it"
                        + " with <Platform>",
                "lsFusion custom view: component 'i.iName' is not inside container 'uiRplBoardView', nor in a container"
                        + " it draws itself",
                "lsFusion custom view: the `row` passed to 'i.qty' does not identify a row: pass the row object from the"
                        + " projected data, not its key");
        assertQuantities(form);
        // the views are out of their hosts, and kept: in the page, in none of the view's hosts, out of sight
        waitFor(parked("uiRplNoteView") + " && " + parked("uiRplMemoView"));

        click(form, "switch class");
        assertThat(noteHost).containsClass("lsf-platform ui-rpl-note ui-rpl-b");
        assertMistake(noteHost, "'c.nope' is not inside 'uiRplBoardView'");
        assertThat(own).containsClass("ui-rpl-own-host ui-rpl-b");
        assertMistake(own, "'uiRplNothing' is not inside 'uiRplBoardView'");
        assertNotMarked(noteHost, own);

        click(form, "right");
        assertThat(noteHost.locator(".lsf-view-error")).hasCount(0);
        assertShows(noteHost, note);
        waitFor("document.querySelector('.ui-rpl-note').contains(window.uiRplNoteView)"
                + " && document.querySelector('.ui-rpl-own-host').contains(window.uiRplMemoView)"); // the same views
        assertThat(own.locator("span.lsf-view-error")).hasCount(0);
        assertThat(own.locator("b.ui-rpl-own")).hasText("mine");
        assertThat(own.locator("b.ui-rpl-own")).hasClass(Pattern.compile("\\blsf-view-error\\b"));
        assertShows(own, "memo");
        assertNotMarked(noteHost, own);
        assertThat(form.locator(".ui-rpl-no-row, .ui-rpl-one-view, .ui-rpl-drawn, .ui-rpl-column, .ui-rpl-not-a-row")).hasCount(0);
        waitFor(FILLS);

        click(form, "switch class"); // with the view in the host
        assertThat(noteHost).containsClass("lsf-platform ui-rpl-note ui-rpl-a");
        assertShows(noteHost, note);
        assertNotMarked(noteHost, own);
        waitFor(FILLS);
        close(form);
    }

    // a view on a page of a tab strip the platform draws: the page left and come back to, the children it places are
    // placed again
    @Test
    public void aViewOnAPageOfThePlatformsTabStripPlacesItsChildrenAgain() throws Exception {
        String note = seed();
        Locator form = open("UiReactPlacing.uiRpl", "UiReactPlacing.uiRplBoard");
        Locator noteHost = form.locator(".ui-rpl-note");
        assertShows(noteHost, note);
        form.locator("[lsfusion-tab='uiRplPlainPage']").click();
        assertThat(form.locator(".ui-rpl-board")).isHidden();
        form.locator("[lsfusion-tab='uiRplBoardPage']").click();
        assertThat(noteHost).isVisible();
        assertShows(noteHost, note);
        assertQuantities(form);
        close(form);
    }

    // a label is read where it is drawn: the platform's tab strip draws the title of a page it does not show - a
    // property's own label, with its caption's class and its change key -, a React view the caption of a child the
    // platform draws that it does not place; each follows a computed caption meanwhile
    @Test
    public void aLabelIsReadWhereItIsDrawn() throws Exception {
        Locator form = open("UiReactPlacing.uiRpl", "UiReactPlacing.uiRplLabels");
        Locator tabs = form.locator("[lsfusion-container='uiRplLabelTabs'] .nav-link");
        assertThat(tabs).hasCount(3);
        assertThat(tabs.nth(1)).hasText("Header 0");
        assertThat(captionClassed(tabs.nth(1), "ui-rpl-header-0")).hasCount(1);
        assertThat(tabs.nth(2)).hasText("Third (F5)");
        Locator unplaced = form.locator(".ui-rpl-unplaced-caption");
        assertThat(unplaced).hasText("Header 0");
        assertThat(form.locator(".ui-rpl-other")).hasText("other");

        form.locator("button[lsfusion-container='PROPERTY(uiRplBump())']").click();
        assertThat(tabs.nth(1)).hasText("Header 1");
        assertThat(captionClassed(tabs.nth(1), "ui-rpl-header-1")).hasCount(1);
        assertThat(captionClassed(tabs.nth(1), "ui-rpl-header-0")).hasCount(0);
        assertThat(tabs.nth(2)).hasText("Third (F6)");
        assertThat(unplaced).hasText("Header 1");
        close(form);
    }

    // what a child the platform draws shows and hides by: the SHOWIF of a container it is - placed in an element of
    // the view's own, marked as a host -: its entry says hidden, and the view hides the host, which keeps the
    // platform's view in it -, and of a property, which hides its host as
    // well; a property's comment is in its entry, for the view to draw, not in the platform's view; an action's entry
    // has no caption - its caption is on its button, which runs it
    @Test
    public void aPlacedChildShowsAndHidesByWhatTheFormShows() throws Exception {
        Locator form = open("UiReactPlacing.uiRpl", "UiReactPlacing.uiRplShows");
        Locator box = form.locator(".ui-rpl-box"), boxEntry = form.locator(".ui-rpl-box-entry");
        assertThat(boxEntry).hasText("box shown");
        assertThat(box).isVisible();
        assertShows(box, "in box");
        // the view's own element is the container's host, marked as any host
        assertThat(box).containsClass("lsf-platform ui-rpl-box");
        assertThat(box).hasAttribute("data-lsf-sid", "uiRplBox");

        click(form, "toggle box");
        assertThat(boxEntry).hasText("box hidden");
        assertThat(box).isHidden();
        assertThat(box.locator(":scope > *")).not().hasCount(0); // the platform's view stays in the host
        click(form, "toggle box");
        assertThat(boxEntry).hasText("box shown");
        assertThat(box).isVisible();
        assertShows(box, "in box");

        Locator note = form.locator(".ui-rpl-shown-note"), noteEntry = form.locator(".ui-rpl-note-entry");
        assertShows(note, "shown note");
        assertThat(form.locator(".ui-rpl-note-comment")).hasText("Note comment");
        assertThat(note).not().containsText("Note comment");
        click(form, "toggle note");
        assertThat(noteEntry).hasText("note hidden");
        assertThat(note).isHidden();
        click(form, "toggle note");
        assertThat(noteEntry).hasText("note shown");
        assertThat(note).isVisible();
        assertShows(note, "shown note");

        assertThat(form.locator(".ui-rpl-go-entry")).hasText("nothing");
        Locator go = form.locator(".ui-rpl-go button");
        assertThat(go).hasText("Go");
        go.click();
        assertThat(form.locator(".ui-rpl-gone")).hasText("1");
        close(form);
    }

    // a form whose content is a React view, opened as a window, comes up holding what the view drew first: the window
    // is measured as it opens and keeps that size (#1817)
    @Test
    public void aWindowOfAReactViewHoldsWhatTheViewDrewFirst() throws Exception {
        Locator opener = open("UiReactPlacing.uiRpl", "UiReactPlacing.uiRplFloatOpener");
        opener.locator("button[lsfusion-container='PROPERTY(uiRplOpenFloating())']").click();
        Locator window = form("UiReactPlacing.uiRplFloating"), content = window.locator(".ui-rpl-floating");
        assertThat(content).hasText("float");
        BoundingBox inner = content.boundingBox(), outer = window.boundingBox();
        assertTrue("the window " + box(outer) + " does not hold what the view drew, " + box(inner),
                outer.x <= inner.x && outer.y <= inner.y
                        && inner.x + inner.width <= outer.x + outer.width && inner.y + inner.height <= outer.y + outer.height);
        // nothing covers its middle either: the window shows it
        assertEquals(true, content.evaluate("e => { const r = e.getBoundingClientRect();"
                + " const hit = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2); return e === hit || e.contains(hit); }"));
        close(window);
        close(opener);
    }

    // a SHOWIF on a container a React view draws reaches the container's entry, and the entry of a property's filter
    // inside it, which the view draws by its name, with nothing failing in the page; a property the view draws there is
    // hidden by its own SHOWIF
    @Test
    public void aShowIfOnAContainerAViewDrawsReachesWhatIsInside() throws Exception {
        List<String> errors = pageErrors();
        Locator form = open("UiReactPlacing.uiRpl", "UiReactPlacing.uiRplShowIf");
        Locator container = form.locator(".ui-rpl-container-entry"), filter = form.locator(".ui-rpl-filter-entry"),
                label = form.locator(".ui-rpl-label-entry");
        assertThat(container).hasText("container shown");
        assertThat(filter).hasText("filter shown");
        assertThat(label).hasText("label label");

        Locator toggle = form.locator("button[lsfusion-container='PROPERTY(uiRplToggleContainer())']");
        toggle.click();
        assertThat(container).hasText("container hidden");
        assertThat(filter).hasText("filter hidden");
        assertThat(label).hasText("label hidden");
        toggle.click();
        assertThat(container).hasText("container shown");
        assertThat(filter).hasText("filter shown");
        assertThat(label).hasText("label label");
        assertEquals("errors in the page", new ArrayList<>(), errors);
        close(form);
    }

    // `tabbed` on a container a React view draws itself means nothing: the view draws both pages, and the server reads
    // the one no tab strip would show as well; an ACTIVATE TAB of a page there is refused, the reason shown. And the
    // view's controller declares that the user is editing in an element of the view, and that the user is not
    @Test
    public void aTabbedContainerAViewDrawsHasEveryPageRead() throws Exception {
        Locator form = open("UiReactPlacing.uiRpl", "UiReactPlacing.uiRplTabbed");
        assertThat(form.locator(".ui-rpl-first-page")).hasText("first");
        assertThat(form.locator(".ui-rpl-second-page")).hasText("second");
        assertEquals("is editing: before, declared, declared no more", "false,true,false", page.evaluate("() => {"
                + " const controller = window.uiRplTabbedController, element = document.querySelector('.ui-rpl-tabbed');"
                + " const editing = [controller.isEditing(element)]; controller.startEditing(element);"
                + " editing.push(controller.isEditing(element)); controller.stopEditing(element);"
                + " editing.push(controller.isEditing(element)); return editing.join(); }"));

        form.locator("button[lsfusion-container='PROPERTY(uiRplActivateSecond())']").click();
        Locator refusal = page.locator(".modal.show").filter(new Locator.FilterOptions().setHasText("ACTIVATE TAB is set for"));
        assertThat(refusal).containsText("ACTIVATE TAB is set for 'uiRplTabbedSecond', which a CUSTOM REACT component draws"
                + " or places - its visibility is controlled by that component");
        refusal.getByText("Close", new Locator.GetByTextOptions().setExact(true)).click();
        assertThat(refusal).hasCount(0);
        close(form);
    }

    // the board's category under a name no other attempt has - what a failed attempt saved stays in the database for
    // its rerun -, shown from now on; its note
    private static String seed() throws Exception {
        long id = System.nanoTime();
        String note = "note " + id;
        exec("UiReactPlacing.uiRplSeed", "Category " + id, note);
        return note;
    }

    // each item's quantity is in the host the view places it in for that item's row, marked with its name
    private static void assertQuantities(Locator form) {
        String[][] items = {{"Banana", "9"}, {"Apple", "5"}, {"Cherry", "1"}};
        assertThat(form.locator(".ui-rpl-row")).hasCount(items.length);
        for (String[] item : items) {
            Locator quantity = form.locator(".ui-rpl-row").filter(new Locator.FilterOptions().setHasText(item[0])).locator(".ui-rpl-qty");
            assertThat(quantity).hasText(item[1]);
            assertThat(quantity).hasAttribute("data-lsf-sid", "i.qty");
        }
    }

    // the value a host shows: the platform's view of a panel property is an input
    private static void assertShows(Locator host, String value) {
        assertThat(host.locator("input")).hasValue(value);
    }

    // hosts the view gave the platform, none marked as a mistake itself: a mistake's message is a child of the host
    private static void assertNotMarked(Locator... hosts) {
        for (Locator host : hosts)
            assertThat(host).not().hasClass(Pattern.compile("\\blsf-view-error\\b"));
    }

    // a view of the platform's saved in the page under this name, out of the view's hosts and kept: in the page, in
    // none of the hosts, out of sight
    private static String parked(String view) {
        return "window." + view + ".isConnected && !window." + view + ".closest('.ui-rpl-board')"
                + " && window." + view + ".getClientRects().length === 0";
    }

    // the mistake a host shows: one message, its own child, in sight
    private static void assertMistake(Locator host, String mistake) {
        Locator message = host.locator(":scope > span.lsf-view-error");
        assertThat(message).hasText("lsFusion: " + mistake);
        assertThat(message).isVisible();
    }

    // a tab's caption, or an element inside it, of the caption's class
    private static Locator captionClassed(Locator tab, String captionClass) {
        return tab.locator("xpath=descendant-or-self::*[contains(concat(' ', normalize-space(@class), ' '), ' " + captionClass + " ')]");
    }

    // a button of a view, by its text
    private static void click(Locator form, String text) {
        form.getByText(text, new Locator.GetByTextOptions().setExact(true)).click();
    }

    private static String box(BoundingBox box) {
        return "(" + box.x + ", " + box.y + ", " + box.width + " x " + box.height + ")";
    }

    // the platform's view of the note fills the host the view gives a height of 120 px, but for its margins - as tall
    // as the host but 20 px at most, where its own height is that of one line of text
    private static final String FILLS = "(() => { const host = document.querySelector('.ui-rpl-note'), view = window.uiRplNoteView;"
            + " return host.contains(view) && host.getBoundingClientRect().height >= 119"
            + " && view.getBoundingClientRect().height >= host.getBoundingClientRect().height - 20; })()";

    // until a condition holds in the page
    private void waitFor(String condition) {
        try {
            page.waitForFunction("() => { try { return !!(" + condition + "); } catch (e) { return false; } }",
                    null, new Page.WaitForFunctionOptions().setTimeout(10_000));
        } catch (PlaywrightException e) {
            throw new AssertionError("never " + condition, e);
        }
    }

    // the complete error log, with no dependence on the order React mounts its hosts
    private void assertLogged(List<String> errors, String... expected) {
        List<String> wanted = new ArrayList<>(Arrays.asList(expected));
        Collections.sort(wanted);
        long deadline = System.currentTimeMillis() + 10_000;
        while (true) {
            List<String> shown = new ArrayList<>(errors);
            Collections.sort(shown);
            if (wanted.equals(shown))
                return;
            if (System.currentTimeMillis() > deadline)
                throw new AssertionError("the page logged " + shown + ", expected " + wanted);
            page.waitForTimeout(100);
        }
    }

    // what the page fails with from now on - its errors and the errors it logs -, but the push registration a test's
    // browser context refuses on every load
    private List<String> pageErrors() {
        List<String> errors = new ArrayList<>();
        Pattern push = Pattern.compile("Push API|Registration failed - permission denied");
        page.onPageError(error -> {
            if (!push.matcher(error).find())
                errors.add(error);
        });
        page.onConsoleMessage(message -> {
            if (message.type().equals("error") && !push.matcher(message.text()).find())
                errors.add(message.text());
        });
        return errors;
    }
}
