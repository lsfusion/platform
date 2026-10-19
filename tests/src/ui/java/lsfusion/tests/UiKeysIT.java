package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Mouse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Request;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.BoundingBox;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.Test;

import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

// the keys and mouse bindings of a form (UiKeys.lsf, its React views in UiKeys.js): those of what React views draw - a
// component's binding is taken while the form shows it and the view that draws it is shown -, and the platform's own,
// which the form binds once for every property and filter, whoever draws them (#1901); and an input that has no cell to
// be entered in (#1894). What a bound action did, it writes in the module's log
public class UiKeysIT extends UiTest {

    private static final String FOLDER = "UiKeys.uiKeys";

    // the console line of the client cancelling an input it has no cell to enter in
    private static final String NO_CELL = "there is no cell to enter it in";

    // a filter group a React view of its container places, under its SHOWIF: its key is not taken while it is hidden -
    // its entry says so and its host is hidden -, and is once it is shown. The filter group's own React view (#1901)
    @Test
    public void aFilterGroupsOwnViewTakesItsKeyOnlyWhileShown() throws Exception {
        filterGroupKeys("UiKeys.uiKeysKindOwn");
    }

    // ... and the platform's widget, custom = NULL (#1901)
    @Test
    public void aFilterGroupThePlatformDrawsTakesItsKeyOnlyWhileShown() throws Exception {
        filterGroupKeys("UiKeys.uiKeysKindPlaced");
    }

    private void filterGroupKeys(String name) throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, name);
        Locator board = form.locator(".ui-keys-board"), rows = form.locator(".ui-keys-rows"),
                host = form.locator("[data-lsf-sid='FILTERGROUP(uiKeysKind)']");
        assertThat(board).hasAttribute("data-kind-hidden", "true");
        assertThat(host).isHidden();
        assertThat(rows).hasText(String.join(", ", items));
        board.focus();
        press("F10"); // Expensive, were it taken
        button(form, "uiKeysToggleKind()").click();
        assertThat(board).hasAttribute("data-kind-hidden", "false");
        assertThat(host).isVisible();
        assertThat(rows).hasText(String.join(", ", items));
        board.focus();
        press("F10"); // Expensive
        assertThat(rows).hasText(items[0] + ", " + items[2]);
        press("F11"); // Cheap
        assertThat(rows).hasText(items[1]);
        close(form);
    }

    // the keys of actions a React view draws - of a panel action, of an action in the rows it draws, on the current
    // row, and of one with CONFIRM, which asks first in the platform's dialog -, beside the key of a button of the
    // platform's the view places (#1901)
    @Test
    public void theKeysOfActionsAViewDraws() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysActions");
        Locator board = form.locator(".ui-keys-actions");
        board.focus();
        press("F6");
        press("F7");
        log("bumped by the platform", "bumped");
        makeCurrent(form, items[1]);
        board.focus();
        press("F8");
        log("bumped by the platform", "bumped", "marked " + items[1]);
        makeCurrent(form, items[2]);
        board.focus();
        press("Control+F8");
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Yes").setExact(true)).click();
        log("bumped by the platform", "bumped", "marked " + items[1], "marked when asked " + items[2]);
        close(form);
    }

    // ESCAPE - the key of the form's close button - closes a form whose toolbar a React view draws (TOOLBARBOX in its
    // container), as it closes one with no view (#1901)
    @Test
    public void escapeClosesAFormWhoseToolbarAViewDraws() throws Exception {
        String[] items = seed();
        Locator classic = open(FOLDER, "UiKeys.uiKeysClassic");
        name(classic, items[0]).click();
        press("Escape");
        classic.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));

        Locator form = open(FOLDER, "UiKeys.uiKeysToolbar");
        form.locator(".ui-keys-toolbar").focus();
        press("Escape");
        form.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
    }

    // a property a React view draws whose change asks a value: a call of its exec() is refused - the view gives the
    // value itself - with no error dialog, and change(value) sets it (#1894)
    @Test
    public void aPropertyAskingAValueTakesItFromItsView() throws Exception {
        Locator form = open(FOLDER, "UiKeys.uiKeysScan");
        form.locator(".ui-keys-exec").click();
        assertThat(form.locator(".ui-keys-said")).containsText("'uiKeysBarcode' asks for a value, which a view gives itself: call change(value)");
        String barcode = String.valueOf(System.nanoTime());
        Locator input = form.locator(".ui-keys-barcode");
        input.fill(barcode);
        input.press("Enter");
        assertThat(form.locator(".ui-keys-value")).hasText(barcode);
        assertThat(page.locator(".modal-content")).hasCount(0);
        close(form);
    }

    // the key of the platform's editor a React view places in every row it draws opens it in the current row (#1901)
    @Test
    public void theKeyOfAnEditorPlacedInEveryRow() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysRows");
        makeCurrent(form, items[1]);
        form.locator(".ui-keys-cards").focus();
        press("F2");
        assertThat(card(form, items[1]).locator("input")).isFocused();
        press("Escape");
        close(form);
    }

    // actions a React view draws that ask a value: the key of one whose input the platform knows ahead is not taken,
    // and a call of its exec() is refused - neither has a cell to enter the value in -; one that asks only as it runs
    // runs, by its key and by its exec(), and its input is cancelled, as a user cancels one, saying so on the console -
    // no error dialog, no editor (#1894)
    @Test
    public void anActionAskingAValue() throws Exception {
        Locator form = open(FOLDER, "UiKeys.uiKeysAsking");
        Locator board = form.locator(".ui-keys-ask"), log = form.locator(".ui-keys-log");
        board.focus();
        press("F9");
        form.locator(".ui-keys-exec-uiKeysAsk").click();
        assertThat(form.locator(".ui-keys-said")).containsText("'uiKeysAsk' asks for a value, which no call can give an action");
        board.focus();
        cancelled(() -> press("Control+F9"));
        assertThat(log).hasText("asked later");
        cancelled(() -> form.locator(".ui-keys-exec-uiKeysAskLater").click());
        assertThat(log).hasText("asked later, asked later");
        idle();
        assertThat(page.locator(".modal-content")).hasCount(0);
        assertThat(page.locator("input:focus")).hasCount(0);
        close(form);
    }

    // a form's own event that asks a value - the current item changed by a click on a row -: there is no cell to enter
    // it in, and the input is cancelled, as a user cancels one, saying so on the console - no error dialog (#1894)
    @Test
    public void aFormsEventAskingAValue() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysEvent");
        cancelled(() -> name(form, items[1]).click());
        log("asked on " + items[1]);
        idle();
        assertThat(page.locator(".modal-content")).hasCount(0);
        close(form);
    }

    // the platform's own bindings, on a form with no React view: a panel action's key, a key of an action in the grid's
    // rows - on the row selected -, a filter's key, and an action of the rows bound to a double click (#1901)
    @Test
    public void thePlatformsOwnBindings() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysClassic");
        name(form, items[0]).click();
        press("F7");
        log("bumped");
        name(form, items[1]).click();
        press("F8");
        log("bumped", "marked " + items[1]);
        press("F10"); // Expensive
        assertThat(name(form, items[1])).hasCount(0);
        assertThat(name(form, items[0])).hasCount(1);
        name(form, items[2]).dblclick();
        log("bumped", "marked " + items[1], "opened " + items[2]);
        close(form);
    }

    // a double click on a row of the platform's dialog picks it: System.formOk's binding there, dialog=only (#1901)
    @Test
    public void aDoubleClickPicksARowOfADialog() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysClassic");
        button(form, "uiKeysClassicPick()").click();
        doubleClick(name(form("UiKeys.uiKeysPickList"), items[1]));
        log("picked " + items[1]);
        close(form);
    }

    // the keys of the platform's dialog: a key of the form that opened it is not taken while it is open; ctrl ENTER -
    // System.formOk's key - picks the row selected, ESCAPE - System.formClose's - closes it picking nothing (#1901)
    @Test
    public void theKeysOfADialog() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysClassic");
        Locator dialog = form("UiKeys.uiKeysPickList");
        button(form, "uiKeysClassicPick()").click();
        select(name(dialog, items[2]));
        press("F7"); // the form's Bump
        press("Control+Enter");
        log("picked " + items[2]);
        button(form, "uiKeysClassicPick()").click();
        select(name(dialog, items[1]));
        press("Escape");
        dialog.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
        name(form, items[0]).click();
        press("F7");
        log("picked " + items[2], "bumped");
        close(form);
    }

    // a list's standard EDIT opens the item's edit form on a double click on a row - its CHANGEMOUSE - and on ENTER -
    // its CHANGEKEY (#1901)
    @Test
    public void editOpensOnADoubleClickAndOnEnter() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysEdits"), edit = form("UiKeys.uiKeysItem");
        name(form, items[1]).dblclick();
        assertThat(edit.locator("[lsfusion-container='PROPERTY(uiKeysName(i))'] input")).hasValue(items[1]);
        press("Escape");
        edit.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
        name(form, items[2]).click();
        press("Enter");
        assertThat(edit.locator("[lsfusion-container='PROPERTY(uiKeysName(i))'] input")).hasValue(items[2]);
        press("Escape");
        edit.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.DETACHED));
        close(form);
    }

    // a panel action's key and a key of an action in the grid's rows are taken only while SHOWIF shows them: turned on,
    // off and on again (#1901)
    @Test
    public void keysOnlyWhileShowIfShows() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysShowIf"), on = button(form, "uiKeysSwitch()");
        name(form, items[1]).click();
        press("F7");
        press("F8");
        on.click();
        log("on");
        name(form, items[1]).click();
        press("F7");
        press("F8");
        log("on", "ran", "marked " + items[1]);
        on.click();
        name(form, items[1]).click();
        press("F7");
        press("F8");
        on.click();
        name(form, items[1]).click();
        press("F7");
        press("F8");
        log("on", "ran", "marked " + items[1], "off", "on", "ran", "marked " + items[1]);
        close(form);
    }

    // the keys of actions on a page of tabs that is not shown: a key is taken once its page is shown, one with
    // showing=all while it is not too; one on the page shown is the control (#1901)
    @Test
    public void keysOnAPageNotShown() throws Exception {
        seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysTabs");
        button(form, "uiKeysHere()").focus();
        press("F9");
        press("F7");
        press("F6");
        log("here", "there, showing all");
        form.getByText("Two", new Locator.GetByTextOptions().setExact(true)).click();
        press("F7");
        press("F6");
        log("here", "there, showing all", "there", "there, showing all");
        close(form);
    }

    // one key of actions of two groups: the group the focus is in takes it, on its row (#1901)
    @Test
    public void aKeyOfTwoGroupsGoesToTheGroupOfTheFocus() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysGroups");
        cell(form, "uiKeysName(a)", items[1]).click();
        press("F8");
        log("marked in a " + items[1]);
        cell(form, "uiKeysName(b)", items[2]).click();
        press("F8");
        log("marked in a " + items[1], "marked in b " + items[2]);
        close(form);
    }

    // of two keys of one priority the later in the form takes it, as the CHANGEKEY docs say (#1901)
    @Test
    public void aKeyOfOnePriorityGoesToTheLaterInTheForm() throws Exception {
        seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysPriorities");
        button(form, "uiKeysFirst()").focus();
        press("F9");
        log("second");
        close(form);
    }

    // of two keys of priorities given the higher one takes it, though it is the first in the form (#1901)
    @Test
    public void aKeyGoesToTheHigherPriority() throws Exception {
        seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysPriorities");
        button(form, "uiKeysFirst()").focus();
        press("F11");
        log("priority 200");
        close(form);
    }

    // a key a property gives itself (changeKey = <property>) is taken while the property gives it: ctrl F6 at first,
    // ctrl F7 once changed, none once NULL, ctrl F6 again (#1901)
    @Test
    public void aKeyAPropertyGivesItself() throws Exception {
        seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysGiven"), run = button(form, "uiKeysRunGiven()");
        run.focus();
        press("Control+F6");
        log("ran");
        button(form, "uiKeysGiveF7()").click();
        run.focus();
        press("Control+F7");
        log("ran", "ctrl F7 given", "ran");
        press("Control+F6");
        button(form, "uiKeysGiveNone()").click();
        run.focus();
        press("Control+F7");
        press("Control+F6");
        button(form, "uiKeysGiveF6()").click();
        run.focus();
        press("Control+F6");
        log("ran", "ctrl F7 given", "ran", "no key given", "ctrl F6 given", "ran");
        close(form);
    }

    // a double click a property gives itself (changeMouse = <property>), with no static mouse binding of it, runs it on
    // the row (#1901)
    @Test
    public void aDoubleClickAPropertyGivesItself() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysGiven");
        name(form, items[1]).dblclick();
        log("opened " + items[1]);
        close(form);
    }

    // the key of a panel action in a column per item (COLUMNS) runs one column's action - the first one's, whichever
    // item is current (#1901)
    @Test
    public void aKeyOfAnActionInColumnsRunsTheFirstColumn() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysColumns");
        cell(form, "uiKeysName(c)", items[1]).click();
        press("Control+F8");
        log("column " + items[0]);
        close(form);
    }

    // a double click in a tree: on a node with children it opens the node - the tree's own binding -, on a leaf and on
    // a node with no children it runs the action of the toolbar bound to it (#1901)
    @Test
    public void aDoubleClickInATree() throws Exception {
        String[] items = seed();
        Locator form = open(FOLDER, "UiKeys.uiKeysTree");
        cell(form, "uiKeysName(t)", items[0]).dblclick();
        cell(form, "uiKeysName(t)", items[1]).dblclick();
        log("opened " + items[1]);
        cell(form, "uiKeysName(t)", items[2]).dblclick();
        log("opened " + items[1], "opened " + items[2]);
        close(form);
    }

    // three items under names no other attempt has - what a failed attempt saved stays in the database for its rerun -:
    // Apple, Banana under Apple, Cherry, the price of Banana alone 100 or less
    private static String[] seed() throws Exception {
        long id = System.nanoTime();
        String[] items = {"Apple " + id, "Banana " + id, "Cherry " + id};
        exec("UiKeys.uiKeysSeed", items);
        return items;
    }

    // what the bound actions did since the seed, in turn
    private static void log(String... done) throws Exception {
        check("UiKeys.uiKeysCheckLog", String.join(", ", done));
    }

    // a real key press - Playwright's names, joined by + - to the element that has the focus, once no request blocks
    // the input, which the client drops while one does; and until the request the key sends, if any, is answered
    private void press(String keys) {
        idle();
        page.keyboard().press(keys);
        idle();
    }

    private void idle() {
        page.locator(".lsf-blocking").waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.HIDDEN));
    }

    // what runs cancels an input it has no cell to enter in, saying so on the console
    private void cancelled(Runnable run) {
        page.waitForConsoleMessage(new Page.WaitForConsoleMessageOptions().setPredicate(message -> message.text().contains(NO_CELL)), run);
    }

    // a row of the platform's dialog other than its current one selected by a click, before a key picks it: the row a
    // click selects goes to the server 50 ms after it, and what the dialog's OK does goes at once - it has no group to
    // send the row first -, so the key waits until the row is sent
    private void select(Locator cell) {
        page.waitForRequest(UiKeysIT::changesRow, cell::click);
    }

    // a double click as a hand makes it: its first press selects the row, as select does, its second one - with the
    // count of two, the double click - goes once the row is sent and no request blocks the input
    private void doubleClick(Locator cell) {
        BoundingBox box = cell.boundingBox();
        double x = box.x + box.width / 2, y = box.y + box.height / 2;
        page.waitForRequest(UiKeysIT::changesRow, () -> page.mouse().click(x, y));
        idle(); // a raw press is lost on the glass of a request
        page.mouse().down(new Mouse.DownOptions().setClickCount(2));
        page.mouse().up(new Mouse.UpOptions().setClickCount(2));
    }

    // the request that sends the current row of a group, after a click on another row
    private static boolean changesRow(Request request) {
        String body = request.postData();
        return body != null && body.contains("ChangeGroupObject");
    }

    // a card of the rows a React view draws, made the current one by a click on its name, as the view shows it
    private void makeCurrent(Locator form, String name) {
        Locator card = card(form, name);
        card.locator("span").first().click();
        assertThat(card).hasClass(Pattern.compile("ui-keys-current"));
    }

    private static Locator card(Locator form, String name) {
        return form.locator(".ui-keys-card").filter(new Locator.FilterOptions().setHasText(name));
    }

    private Locator name(Locator form, String name) {
        return cell(form, "uiKeysName(i)", name);
    }

    private static Locator button(Locator form, String property) {
        return form.locator("button[lsfusion-container='PROPERTY(" + property + ")']");
    }
}
