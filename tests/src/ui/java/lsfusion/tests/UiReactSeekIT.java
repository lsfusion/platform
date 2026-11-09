package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// window.lsfusion.useSeekOnScroll in a React view whose rows scroll in a box of its own (UiReactSeek.js), over 500 rows
// read a page of 50 at a time: the hook reseats the current row on the edge it leaves through, and holds the row it
// sought in place while the window shifts under the box. A current moved from outside - an ACTIVATE here - is not the
// hook's: the row the hook sought before it may still be in the window the move reads, and it must not drag the view
// back off the new current, nor make the hook seek away from it
public class UiReactSeekIT extends UiTest {

    private final View cards = view("window.uiReactSeek",
            " const list = data && data.i ? data.i.list : [], cur = list.find(r => r.current),"
            + " seeks = view ? view.seeks : [];"
            + " const sc = document.querySelector('[lsfusion-form=\"UiReactSeek.uiReactSeekCards\"] .ui-react-seek-scroller'),"
            + " ce = sc && sc.querySelector('.ui-react-seek-current');"
            + " const top = el => el && sc ? el.getBoundingClientRect().top - sc.getBoundingClientRect().top : null,"
            + " shown = el => !!el && !!sc && top(el) >= -1"
            + " && el.getBoundingClientRect().bottom <= sc.getBoundingClientRect().bottom + 1,"
            + " row = n => sc && sc.querySelector('[data-number=\"' + n + '\"]');");

    private static final String STATE = "JSON.stringify({len: list.length, first: list[0] && list[0].number.value,"
            + " last: list.length && list[list.length - 1].number.value, cur: cur && cur.number.value,"
            + " scrollTop: sc && sc.scrollTop, scrollMax: sc && sc.scrollHeight - sc.clientHeight,"
            + " curShown: shown(ce), curTop: top(ce),"
            + " offsets: sc ? Array.from(sc.children).filter(shown).map(el => [Number(el.dataset.number), top(el)]) : [],"
            + " seeks: seeks})";

    // ACTIVATE LAST after the hook's own seek: the window read up to the end still holds the row the hook sought, with
    // fewer rows above it - the last row is current and on screen, and nothing is sought away from it
    @Test
    public void anOutsideJumpKeepsItsCurrent() throws Exception {
        exec("UiReactSeek.uiReactSeekSeed", "500");
        Locator form = open("UiReactSeek.uiReactSeek", "UiReactSeek.uiReactSeekCards");
        seekFrom400();

        int seeksBefore = ((Number) cards.value("seeks.length")).intValue();
        cards.call("controller.toLast.exec()");
        waitFor("list.length && list[list.length - 1].number.value === 500"); // the current itself may not last
        page.waitForTimeout(1500); // the settle, any correction and any seek after it

        String after = (String) cards.value(STATE);
        assertEquals("the outside jump keeps the last row current: " + after, "500", cards.value("String(cur && cur.number.value)"));
        assertEquals("the last row is on screen inside the box: " + after, true, cards.value("shown(ce)"));
        assertEquals("nothing is sought after toLast: " + after, seeksBefore, ((Number) cards.value("seeks.length")).intValue());
        close(form);
    }

    // ACTIVATE of a row on screen after the hook's own seek: the window is read around that row, so it shifts under the
    // box, and the row stays where it was on screen - the shift corrected for once, before the row is shown
    @Test
    public void anOutsideMoveOntoARowOnScreenKeepsItsPlace() throws Exception {
        exec("UiReactSeek.uiReactSeekSeed", "500");
        Locator form = open("UiReactSeek.uiReactSeek", "UiReactSeek.uiReactSeekCards");
        seekFrom400();

        int aim = ((Number) cards.value("cur.number.value")).intValue() + 3;
        exec("UiReactSeek.uiReactSeekSetAim", Integer.toString(aim));
        String before = (String) cards.value(STATE);
        assertEquals("the aimed row is on screen before the move: " + before, true, cards.value("shown(row(" + aim + "))"));
        double topBefore = ((Number) cards.value("top(row(" + aim + "))")).doubleValue();
        int firstBefore = ((Number) cards.value("list[0].number.value")).intValue();
        int seeksBefore = ((Number) cards.value("seeks.length")).intValue();

        cards.call("controller.toAim.exec()");
        waitFor("cur && cur.number.value === " + aim);
        assertTrue("the window is read around the aimed row, so it shifts under the box: " + before + " -> " + cards.value(STATE),
                ((Number) cards.value("list[0].number.value")).intValue() != firstBefore);
        page.waitForTimeout(1500);

        String after = (String) cards.value(STATE);
        assertEquals("the outside move keeps the aimed row current: " + after, Integer.toString(aim), cards.value("String(cur && cur.number.value)"));
        assertEquals("nothing is sought after the move: " + after, seeksBefore, ((Number) cards.value("seeks.length")).intValue());
        assertEquals("the aimed row keeps its place inside the box: " + before + " -> " + after,
                topBefore, ((Number) cards.value("top(row(" + aim + "))")).doubleValue(), 1.0);
        close(form);
    }

    // ACTIVATE of 400 - brought into view, as a current moved from outside is -, then the box scrolled until 400 leaves
    // above: the hook seeks the first row on screen, which holds its place from then on, and the seek lands
    private void seekFrom400() {
        waitFor("list.length > 0 && cur");
        cards.call("controller.to400.exec()");
        waitFor("cur && cur.number.value === 400 && shown(ce)");
        page.waitForTimeout(600);
        cards.call("seeks.length = 0; sc.scrollTop += ce.getBoundingClientRect().bottom - sc.getBoundingClientRect().top + 100");
        waitFor("seeks.length > 0 && cur && cur.number.value === seeks[seeks.length - 1]");
        page.waitForTimeout(600); // the seek's flight and the settle after it

        String landed = (String) cards.value(STATE);
        assertEquals("the current is the row the hook sought: " + landed,
                cards.value("seeks[seeks.length - 1]"), cards.value("cur && cur.number.value"));
        assertEquals("the hook sought the first row on screen: " + landed, 0.0, ((Number) cards.value("top(ce)")).doubleValue(), 1.0);
    }

    // a failed wait says where the box was scrolled, as well as which rows it had
    private void waitFor(String condition) {
        try {
            cards.waitFor(condition);
        } catch (AssertionError error) {
            throw new AssertionError("never " + condition + ": " + cards.value(STATE), error);
        }
    }
}
