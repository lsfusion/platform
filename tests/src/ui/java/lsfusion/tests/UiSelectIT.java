package lsfusion.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.SelectOption;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// a property's selection (UiSelect.lsf): the platform's own element of it (select.js) in each of its kinds the server
// picks - how it is drawn, what a pick changes, in a panel and in a table, by the mouse and by the keys, read-only, over
// the rows of a group, and as the server changes it -, and under a React view (its views in web/init/uiSelect.jsx): who
// draws it, and what a pick through the view changes
public class UiSelectIT extends UiTest {

    // a SELECT with NOCUSTOM is drawn by the platform inside a React view, which places it - in the panel, per row -, and
    // picks as the platform's element picks; a render function given in CUSTOM draws a SELECT, given the options as that
    // element is
    @Test
    public void aSelectionThePlatformDrawsInAReactView() throws Exception {
        String[] orders = seed();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectPlaced");
        assertThat(form.locator(".ui-select-panel select option:checked")).hasText("New"); // the dropdown
        Locator rows = form.locator(".ui-select-row");
        assertThat(rows).hasCount(2);
        for (int i = 0; i < 2; i++)
            for (String status : new String[] {"New", "Done", "Lost"})
                assertThat(text(rows.nth(i), status)).isVisible();
        text(rows.nth(1), "Done").click();
        assertThat(rows.nth(1).getByLabel("Done", new Locator.GetByLabelOptions().setExact(true))).isChecked();

        assertThat(text(form, "fn New")).hasAttribute("aria-pressed", "true");
        assertThat(text(form, "fn Lost")).hasAttribute("aria-pressed", "false");
        text(form, "fn Lost").click();
        assertThat(text(form, "fn Lost")).hasAttribute("aria-pressed", "true");

        save(form);
        check("UiSelect.uiSelectCheckStatus", orders[1], "Done");
        check("UiSelect.uiSelectCheckStatus", orders[0], "Lost");
        close(form);
    }

    // a pick of one value through the view: an option, its value, null to clear it; an explicit INPUT with a list is
    // given the option's view. What SELECT adds is one field, its list alone here - neither of several values nor one that
    // has to be there, and no element: how it is drawn is the view's own
    @Test
    public void aPickOfOneValue() throws Exception {
        String[] orders = seed();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectPicked");
        view.waitFor("data.o.panelStatus.value === 'New'");
        expect("Object.keys(data.o.panelStatus.select).join()", "list");
        text(form, "ps Done").click();
        view.waitFor("data.o.panelStatus.value === 'Done' && picked(data.o.panelStatus) === 'Done' && data.o.list[0].uiSelectNameStatus.value === 'Done'");
        text(form, "ps by value Lost").click();
        view.waitFor("data.o.panelStatus.value === 'Lost' && picked(data.o.panelStatus) === 'Lost'");
        text(form, "ps clear").click();
        view.waitFor("data.o.panelStatus.value === null && picked(data.o.panelStatus) === '' && data.o.list[0].uiSelectNameStatus.value === null");
        text(form, "is Done").click();
        view.waitFor("data.o.inputStatus.value === 'Done' && data.o.panelStatus.value === 'Done'");
        save(form);
        check("UiSelect.uiSelectCheckStatus", orders[0], "Done");
        close(form);
    }

    // a pick of several values through the view: the options checked from now on - one more, then one more and one less
    // in one call, by their value as the property has it, the values joined by its separator, which the view is given
    // too, with all the options
    @Test
    public void aPickOfSeveralValues() throws Exception {
        String[] orders = seed();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectPicked");
        view.waitFor("picked(data.o.panelTags) === 'red' && data.o.panelTags.value === 'red'");
        expect("JSON.stringify([data.o.panelTags.select.multi, data.o.panelTags.select.separator, data.o.panelTags.select.list.length])", "[true,\", \",3]");
        text(form, "pt green").click();
        view.waitFor("picked(data.o.panelTags) === 'green,red' && data.o.panelTags.value === 'green, red' && picked(data.o.list[0].uiSelectTags) === 'green,red'");
        text(form, "pt value blue, green").click(); // blue checked and red unchecked, in one batch
        view.waitFor("picked(data.o.panelTags) === 'blue,green' && data.o.panelTags.value === 'blue, green'");
        save(form);
        check("UiSelect.uiSelectCheckTags", orders[0], "blue, green");
        close(form);
    }

    // a pick in a row through the view is that row's, as the selection is told on each row's cell
    @Test
    public void aPickInARow() throws Exception {
        String[] orders = seed();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectPicked");
        view.waitFor("data.o.list.length === 2");
        expect("[!!data.o.list[1].uiSelectNameStatus.select.multi, data.o.list[1].uiSelectTags.select.multi].join()", "false,true");
        text(form, "r2 Lost").click();
        view.waitFor("data.o.list[1].uiSelectNameStatus.value === 'Lost' && data.o.list[0].uiSelectNameStatus.value === 'New'");
        text(form, "r2 tag blue").click();
        view.waitFor("picked(data.o.list[1].uiSelectTags) === 'blue' && picked(data.o.list[0].uiSelectTags) === 'red'");
        save(form);
        check("UiSelect.uiSelectCheckStatus", orders[1], "Lost");
        check("UiSelect.uiSelectCheckTags", orders[1], "blue");
        close(form);
    }

    // ---------------------------------------------------------------- the platform's own element of a selection

    // a list - a column of radio buttons, of check boxes for several values - of each kind (uiSelectElementForm)
    @Test
    public void aListOfEachKind() throws Exception {
        optionsOfEachKind("uiSelectList", "list");
    }

    // ... buttons standing apart
    @Test
    public void buttonsOfEachKind() throws Exception {
        optionsOfEachKind("uiSelectButton", "button");
    }

    // ... a button group
    @Test
    public void aButtonGroupOfEachKind() throws Exception {
        optionsOfEachKind("uiSelectButtonGroup", "buttonGroup");
    }

    // an element of options - a radio button or a check box with its label each -, of each kind: drawn with the options'
    // views as text or as HTML, the options the task has picked; a pick of one value, given to the property or to its
    // INPUT, the option picked clicked again kept where the value has to be there, cleared where it may be none; of several
    // values a toggle - each pick shown by every element of the same selection, as the server has it. With none picked of a
    // value that has to be there the element marks it as missing; a change of the server's own - an option renamed, one
    // deleted, one added, other values picked - is shown as made
    private void optionsOfEachKind(String formName, String element) throws Exception {
        String task = seedTask();
        Locator form = open("UiSelect.uiSelect", "UiSelect." + formName);
        for (String name : names(PHASES, STATUSES, TAGS))
            drawn(property(form, name), element, isHtml(name), isTags(name) ? TAG_VIEWS : STATUS_VIEWS, isTags(name));
        shownBy(form, PHASES, "New");
        shownBy(form, STATUSES, "New");
        shownBy(form, TAGS, "red");

        for (String name : PHASES) {
            String other = name.equals("phaseValue") || name.equals("phaseHtmlValue") ? "Lost" : "Done";
            pick(form, name, other);
            shownBy(form, PHASES, other);
            pick(form, name, other); // picked again: kept
            shownBy(form, PHASES, other);
        }
        for (String name : STATUSES) {
            pick(form, name, "Done");
            shownBy(form, STATUSES, "Done");
            pick(form, name, "Done"); // picked again: cleared
            shownBy(form, STATUSES);
        }
        pick(form, "statusValue", "Lost");
        shownBy(form, STATUSES, "Lost");
        pick(form, "tags", "green");
        shownBy(form, TAGS, "red", "green");
        pick(form, "tagsHtml", "red");
        shownBy(form, TAGS, "green");
        pick(form, "tagsHtml", "blue");
        shownBy(form, TAGS, "blue", "green");
        save(form);
        check("UiSelect.uiSelectCheckTask", task, "Lost / Lost / blue, green");

        propertyButton(form, "dropPhase").click();
        for (String name : PHASES)
            missing(property(form, name), element, true);
        shownBy(form, PHASES);
        pick(form, "phaseHtmlValue", "Done");
        shownBy(form, PHASES, "Done");
        for (String name : PHASES)
            missing(property(form, name), element, false);
        save(form);
        check("UiSelect.uiSelectCheckTask", task, "Done / Lost / blue, green");

        propertyButton(form, "shift").click();
        for (String name : names(PHASES, STATUSES, TAGS))
            drawn(property(form, name), element, isHtml(name), isTags(name) ? SHIFTED_TAG_VIEWS : SHIFTED_STATUS_VIEWS, isTags(name));
        shownOf(form, PHASES, SHIFTED_STATUS_VIEWS, "Late");
        shownOf(form, STATUSES, SHIFTED_STATUS_VIEWS);
        shownOf(form, TAGS, SHIFTED_TAG_VIEWS, "black", "green");
        cancel(form);
        close(form);
    }

    // a panel's native radios (also btn-check): arrows focus and pick the adjacent option; clearing a picked radio is
    // mouse-only. Space toggles a check box of several values; the other elements and the server agree
    @Test
    public void optionsInAPanelByTheKeys() throws Exception {
        for (String element : new String[] {"List", "Button", "ButtonGroup"}) {
            String task = seedTask();
            Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelect" + element);
            for (String name : new String[] {"phase", "status"}) {
                String[] names = name.equals("phase") ? PHASES : STATUSES;
                Locator inputs = property(form, name).locator("input");
                press(inputs.first(), "ArrowDown");
                shownBy(form, names, "Done");
                press(inputs.nth(1), "ArrowUp");
                shownBy(form, names, "New");
                press(inputs.first(), "ArrowRight");
                shownBy(form, names, "Done");
                press(inputs.nth(1), "ArrowLeft");
                shownBy(form, names, "New");
                press(inputs.first(), "ArrowUp"); // native radio groups wrap to the last option
                shownBy(form, names, "Lost");
                save(form);
                check("UiSelect.uiSelectCheckTask", task, name.equals("phase") ? "Lost / New / red" : "Lost / Lost / red");
            }
            for (String name : TAGS) {
                Locator checkbox = property(form, name).getByLabel("green", new Locator.GetByLabelOptions().setExact(true));
                press(checkbox, "Space");
                shownBy(form, TAGS, "green", "red");
                save(form);
                check("UiSelect.uiSelectCheckTask", task, "Lost / Lost / green, red");
                press(checkbox, "Space");
                shownBy(form, TAGS, "red");
                save(form);
                check("UiSelect.uiSelectCheckTask", task, "Lost / Lost / red");
            }
            close(form);
        }
    }

    // a drop-down list of each kind: of one value with the options' views as text the native one, its empty option to
    // clear the value - hidden where the value has to be there -; as HTML and of several values select2's, its list opened
    // by a click on it. A pick of one value, given to the property or to its INPUT, of one value or none the empty option
    // clearing it; of several values a toggle in the list it opens, which a pick leaves open and a click on the list's
    // field closes
    @Test
    public void aDropdownOfEachKind() throws Exception {
        String task = seedTask();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectDropdown");
        for (String name : NATIVE) {
            Locator select = property(form, name).locator("select");
            assertThat(select).hasClass(Pattern.compile("\\bform-select\\b"));
            if (name.startsWith("phase")) // the value has to be there: no empty option to clear it
                assertThat(select.locator("option").first()).hasAttribute("hidden", "");
            else
                assertThat(select.locator("option").first()).not().hasAttribute("hidden", Pattern.compile(".*"));
        }
        for (String name : names(PICKED, TAGS))
            assertThat(property(form, name).locator(".select2-container")).hasCount(1);
        for (String name : new String[] {"phaseHtml", "phaseHtmlValue"})
            assertThat(property(form, name).locator("select option").first()).hasAttribute("hidden", "");
        dropdownOptions(form, STATUS_VIEWS, TAG_VIEWS);
        dropdownPicked(form, PHASES, "New");
        dropdownPicked(form, STATUSES, "New");
        choices(form, TAGS, "red");
        for (String name : PICKED)
            assertThat(property(form, name).locator(".select2-selection__rendered b")).hasText("New");
        assertThat(property(form, "tagsHtml").locator(".select2-selection__choice__display i")).hasText("red");

        for (String name : PHASES) {
            String other = name.endsWith("Value") ? "Lost" : "Done";
            dropdownPick(form, name, other);
            dropdownPicked(form, PHASES, other);
        }
        for (String name : STATUSES) {
            dropdownPick(form, name, "Done");
            dropdownPicked(form, STATUSES, "Done");
            dropdownPick(form, name, ""); // the empty option: cleared
            dropdownPicked(form, STATUSES, "");
        }
        dropdownPick(form, "statusHtml", "Lost");
        dropdownPicked(form, STATUSES, "Lost");

        Locator field = property(form, "tags").locator(".select2-selection");
        field.click();
        assertThat(resultOptions()).hasText(new String[] {"blue", "green", "red"});
        resultOption("green").click();
        choices(form, TAGS, "green", "red");
        resultOption("blue").click(); // the list stays open for the next pick
        choices(form, TAGS, "blue", "green", "red");
        field.click();
        assertThat(resultOptions()).hasCount(0);
        property(form, "tagsHtml").locator(".select2-selection").click();
        resultOption("red").click(); // picked: no longer
        choices(form, TAGS, "blue", "green");
        property(form, "tagsHtml").locator(".select2-selection").click();
        assertThat(resultOptions()).hasCount(0);
        save(form);
        check("UiSelect.uiSelectCheckTask", task, "Lost / Lost / blue, green");

        propertyButton(form, "dropPhase").click();
        dropdownPicked(form, PHASES, "");
        for (String name : PHASES) {
            assertThat(property(form, name).locator("select")).hasClass(Pattern.compile("\\bis-invalid\\b"));
            assertThat(property(form, name).locator("select option").first()).hasAttribute("hidden", "");
        }
        dropdownPick(form, "phaseHtmlValue", "Done");
        dropdownPicked(form, PHASES, "Done");
        for (String name : PHASES)
            assertThat(property(form, name).locator("select")).not().hasClass(Pattern.compile("\\bis-invalid\\b"));
        save(form);
        check("UiSelect.uiSelectCheckTask", task, "Done / Lost / blue, green");

        // the server's own change: an option renamed, one deleted, one added - an option the update changes in place shown
        // as changed, also by select2, which keeps what it read of an option (the list of several values as text showed it
        // as it was)
        propertyButton(form, "shift").click();
        for (String name : NATIVE)
            assertThat(property(form, name).locator("select option")).hasText(Stream.concat(Stream.of(""), Arrays.stream(SHIFTED_STATUS_VIEWS)).toArray(String[]::new));
        dropdownPicked(form, PHASES, "Late");
        dropdownPicked(form, STATUSES, "");
        choices(form, TAGS, "black", "green");
        cancel(form);
        close(form);
    }

    // the keys in a panel's drop-down list of one value: down in the native one, space, down and enter in select2,
    // given to the property or to its explicit INPUT, the pick shown by the other elements and kept on the server
    @Test
    public void aDropdownByTheKeys() throws Exception {
        String task = seedTask();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectDropdown");
        for (String name : new String[] {"status", "statusValue"}) {
            Locator select = property(form, name).locator("select");
            select.focus();
            press(select, "ArrowDown");
            dropdownPicked(form, STATUSES, name.equals("status") ? "Done" : "Lost");
        }
        save(form);
        check("UiSelect.uiSelectCheckTask", task, "New / Lost / red");
        for (String name : new String[] {"statusHtml", "statusHtmlValue"}) {
            dropdownPick(form, "status", "New");
            Locator field = property(form, name).locator(".select2-selection");
            field.focus();
            press(field, "Space");
            assertThat(resultOption("New").locator("b")).hasText("New");
            press("ArrowDown"); // Done
            press("Enter");
            dropdownPicked(form, STATUSES, "Done");
        }
        save(form);
        check("UiSelect.uiSelectCheckTask", task, "New / Done / red");
        close(form);
    }

    // the keys in select2's list of several values, as text and HTML: Space opens it, up moves to the previous option,
    // Enter toggles it and leaves the list open; a click on the field closes it, and the server keeps both picks
    @Test
    public void aDropdownOfSeveralValuesByTheKeys() throws Exception {
        String task = seedTask();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectDropdown");
        for (String name : TAGS) {
            Locator field = property(form, name).locator(".select2-selection");
            field.focus();
            press(field, "Space");
            assertThat(isHtml(name) ? resultOptions().locator("i") : resultOptions()).hasText(TAG_VIEWS);
            press("ArrowUp");
            String next = name.equals("tags") ? "green" : "blue";
            assertThat(resultOption(next)).hasClass(Pattern.compile("\\bselect2-results__option--highlighted\\b"));
            press("Enter");
            String[] picked = name.equals("tags") ? new String[] {"green", "red"} : TAG_VIEWS;
            choices(form, TAGS, picked);
            assertThat(resultOptions()).hasText(TAG_VIEWS); // the pick leaves the list open
            field.click(); // Escape after a pick goes to the form, a bug of its own
            assertThat(resultOptions()).hasCount(0);
            save(form);
            check("UiSelect.uiSelectCheckTask", task, "New / New / " + String.join(", ", picked));
        }
        close(form);
    }

    // an input of several values - the options picked shown, the others looked up by the text typed, its views as text
    // or as HTML: a pick of an option looked up - none of the ones the element was given -, a drop by the cross of a picked
    // one, the change of the server's own shown
    @Test
    public void anInputOfSeveralValues() throws Exception {
        String task = seedTask();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectInput");
        items(form, TAGS, "red");
        assertThat(property(form, "tagsHtml").locator(".selectize-input .item i")).hasText("red");

        Locator search = property(form, "tags").locator(".selectize-input input");
        search.click(); // the list opens with the options not picked
        assertThat(lookedUp()).hasText(new String[] {"green", "blue"});
        search.pressSequentially("gr");
        assertThat(lookedUp()).hasText(new String[] {"green"});
        lookedUp().first().click();
        items(form, TAGS, "red", "green");
        assertThat(lookedUp()).hasCount(0);

        search = property(form, "tagsHtml").locator(".selectize-input input");
        search.click();
        search.pressSequentially("b");
        assertThat(lookedUp()).hasText(new String[] {"blue"});
        assertThat(lookedUp().locator("i")).hasText("blue");
        lookedUp().first().click();
        items(form, TAGS, "red", "green", "blue");
        for (String tag : new String[] {"red", "green", "blue"})
            assertThat(property(form, "tagsHtml").locator(".selectize-input .item i").filter(new Locator.FilterOptions().setHasText(exact(tag)))).hasCount(1);

        property(form, "tags").locator(".item").filter(new Locator.FilterOptions().setHasText("red")).locator(".remove").click();
        items(form, TAGS, "green", "blue");
        save(form);
        check("UiSelect.uiSelectCheckTask", task, "New / New / blue, green");

        propertyButton(form, "shift").click();
        items(form, TAGS, "green", "black");
        cancel(form);
        close(form);
    }

    // the input's keys, as text and HTML: type a lookup, Enter picks its option, Backspace drops the last item shown;
    // the other input shows each change too, and the server keeps it
    @Test
    public void anInputByTheKeys() throws Exception {
        for (String name : TAGS) {
            String task = seedTask();
            Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectInput");
            Locator search = property(form, name).locator(".selectize-input input");
            search.click();
            assertThat(isHtml(name) ? lookedUp().locator("i") : lookedUp()).hasText(new String[] {"green", "blue"});
            search.pressSequentially("gr");
            assertThat(isHtml(name) ? lookedUp().locator("i") : lookedUp()).hasText(new String[] {"green"});
            press(search, "Enter");
            items(form, TAGS, "red", "green");
            assertThat(search).hasValue("");
            assertThat(lookedUp()).hasCount(0);
            save(form);
            check("UiSelect.uiSelectCheckTask", task, "New / New / green, red");

            // the server may reorder the picks; Backspace must drop the last item it shows
            String kept = property(form, name).locator(".selectize-input .item").first().innerText().replace("×", "").trim();
            search.click();
            assertThat(lookedUp()).hasText(new String[] {"blue"});
            press(search, "Backspace");
            items(form, TAGS, kept);
            save(form);
            check("UiSelect.uiSelectCheckTask", task, "New / New / " + kept);
            close(form);
        }
    }

    // the server grows the options by two, then deletes two - of several values one of them picked -: each element shows
    // their order and the picks after each change, and a pick still changes the right option - seen by the other elements
    // of the same selection. The options stay in the form's session, dropped at the end
    @Test
    public void theOptionsGrowAndShrink() throws Exception {
        for (String element : new String[] {"List", "Button", "ButtonGroup", "Dropdown", "Input"}) {
            String task = seedTask();
            Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelect" + element);
            boolean input = element.equals("Input"), dropdown = element.equals("Dropdown");
            for (boolean grow : new boolean[] {true, false}) {
                propertyButton(form, grow ? "grow" : "shrink").click();
                String[] statuses = grow ? new String[] {"New", "Done", "Lost", "Ready", "Wait"} : new String[] {"New", "Done", "Wait"};
                String[] tags = grow ? new String[] {"amber", "blue", "green", "red", "violet"} : new String[] {"green", "red", "violet"};
                String[] picked = grow ? new String[] {"red"} : new String[] {"red", "violet"};
                if (input) {
                    items(form, TAGS, picked);
                    for (String name : TAGS) {
                        property(form, name).locator(".selectize-input input").click();
                        String[] unpicked = grow ? new String[] {"green", "blue", "amber", "violet"} : new String[] {"green"};
                        assertThat(isHtml(name) ? lookedUp().locator("i") : lookedUp()).hasText(unpicked);
                        press("Escape");
                        assertThat(lookedUp()).hasCount(0);
                    }
                } else if (dropdown) {
                    dropdownOptions(form, statuses, tags);
                    dropdownPicked(form, PHASES, grow ? "New" : "Wait");
                    dropdownPicked(form, STATUSES, grow ? "New" : "Wait");
                    choices(form, TAGS, picked);
                } else {
                    for (String name : names(PHASES, STATUSES, TAGS))
                        drawn(property(form, name), element.equals("List") ? "list" : element.equals("Button") ? "button" : "buttonGroup",
                                isHtml(name), isTags(name) ? tags : statuses, isTags(name));
                    shownOf(form, PHASES, statuses, grow ? new String[] {"New"} : new String[] {"Wait"});
                    shownOf(form, STATUSES, statuses, grow ? new String[] {"New"} : new String[] {"Wait"});
                    shownOf(form, TAGS, tags, picked);
                }

                if (!input)
                    for (String[] names : new String[][] {PHASES, STATUSES})
                        for (int i = 0; i < names.length; i++) {
                            String next = grow ? (i % 2 == 0 ? "Ready" : "Wait") : (i % 2 == 0 ? "Done" : "Wait"); // Wait: kept by the shrink
                            if (dropdown) {
                                dropdownPick(form, names[i], next);
                                dropdownPicked(form, names, next);
                            } else {
                                pick(form, names[i], next);
                                shownOf(form, names, statuses, next);
                            }
                        }
                for (int i = 0; i < TAGS.length; i++) {
                    String next = grow ? (i == 0 ? "amber" : "violet") : (i == 0 ? "green" : "red");
                    if (input) {
                        if (!grow && i == 1)
                            property(form, TAGS[i]).locator(".item").filter(new Locator.FilterOptions().setHasText("red")).locator("a.remove").click();
                        else {
                            property(form, TAGS[i]).locator(".selectize-input input").click();
                            lookedUp().filter(new Locator.FilterOptions().setHasText(exact(next))).click();
                        }
                    } else if (dropdown)
                        dropdownPick(form, TAGS[i], next);
                    else
                        pick(form, TAGS[i], next);
                    picked = grow ? (i == 0 ? new String[] {"amber", "red"} : new String[] {"amber", "red", "violet"})
                            : (i == 0 ? new String[] {"green", "red", "violet"} : new String[] {"green", "violet"});
                    if (input)
                        items(form, TAGS, picked);
                    else if (dropdown)
                        choices(form, TAGS, picked);
                    else
                        shownOf(form, TAGS, tags, picked);
                }
            }
            cancel(form);
            close(form);
            check("UiSelect.uiSelectCheckTask", task, "New / New / red");
        }
    }

    // excel's drop-down lists: the native one of one value as text, multiple-select's of HTML and several values,
    // drawn with the picks, a pick or a toggle kept on the server, a change of the server's own shown by each kind
    @Test
    public void dropdownsInExcel() throws Exception {
        String task = seedTask();
        try {
            exec("UiSelect.uiSelectExcel");
            page.reload();
            page.locator("[lsfusion-container='System.root']").waitFor();
            Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectDropdown");
            dropdownPicked(form, NATIVE, "New");
            excelPicked(form, PICKED, "New");
            excelPicked(form, TAGS, "red");
            for (String name : PICKED)
                assertThat(property(form, name).locator(".ms-choice > span b")).hasText("New");
            assertThat(property(form, "tagsHtml").locator(".ms-choice > span i")).hasText("red");
            for (String name : names(PICKED, TAGS)) {
                assertThat(property(form, name).locator(".ms-parent")).hasCount(1);
                property(form, name).locator(".ms-choice").click();
                if (isHtml(name))
                    assertThat(excelOptions().locator(isTags(name) ? "i" : "b")).hasText(views(name));
                else
                    assertThat(excelOptions()).hasText(views(name));
                property(form, name).locator(".ms-choice").click();
            }
            for (String name : NATIVE)
                dropdownPick(form, name, "Done");
            for (int i = 0; i < PICKED.length; i++) { // each pick another option: the one given to the INPUT too
                String view = i % 2 == 0 ? "Lost" : "New";
                property(form, PICKED[i]).locator(".ms-choice").click();
                excelOption(view).click();
                excelPicked(form, PICKED[i].startsWith("phase") ? new String[] {"phaseHtml", "phaseHtmlValue"}
                        : new String[] {"statusHtml", "statusHtmlValue"}, view);
            }
            dropdownPicked(form, NATIVE, "New");
            for (String name : TAGS) {
                property(form, name).locator(".ms-choice").click();
                excelOption(name.equals("tags") ? "green" : "red").click();
                property(form, name).locator(".ms-choice").click();
                excelPicked(form, TAGS, name.equals("tags") ? "green, red" : "green");
            }
            save(form);
            check("UiSelect.uiSelectCheckTask", task, "New / New / green");

            propertyButton(form, "shift").click();
            dropdownPicked(form, new String[] {"phase", "phaseValue"}, "Late");
            dropdownPicked(form, new String[] {"status", "statusValue"}, "");
            excelPicked(form, new String[] {"phaseHtml", "phaseHtmlValue"}, "Late");
            excelPicked(form, new String[] {"statusHtml", "statusHtmlValue"}, "");
            excelPicked(form, TAGS, "black, green");
            for (String name : names(PICKED, TAGS)) {
                property(form, name).locator(".ms-choice").click();
                String[] views = isTags(name) ? SHIFTED_TAG_VIEWS : SHIFTED_STATUS_VIEWS;
                assertThat(isHtml(name) ? excelOptions().locator(isTags(name) ? "i" : "b") : excelOptions()).hasText(views);
                property(form, name).locator(".ms-choice").click();
            }
            cancel(form);
            close(form);
        } finally {
            exec("UiSelect.uiSelectDefaultTheme");
        }
    }

    // excel's lists, buttons and input: each element draws text and HTML, a pick of one value and a toggle of several
    // values - a looked-up option in the input - shown by the other elements and kept on the server
    @Test
    public void listsButtonsAndInputInExcel() throws Exception {
        try {
            exec("UiSelect.uiSelectExcel");
            page.reload();
            page.locator("[lsfusion-container='System.root']").waitFor();
            for (String element : new String[] {"List", "Button", "ButtonGroup", "Input"}) {
                String task = seedTask();
                Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelect" + element);
                if (element.equals("Input")) {
                    items(form, TAGS, "red");
                    assertThat(property(form, "tagsHtml").locator(".selectize-input .item i")).hasText("red");
                    property(form, "tagsHtml").locator(".selectize-input input").click();
                    assertThat(lookedUp().locator("i")).hasText(new String[] {"green", "blue"});
                    lookedUp().filter(new Locator.FilterOptions().setHasText(exact("green"))).click();
                    items(form, TAGS, "red", "green");
                    save(form);
                    check("UiSelect.uiSelectCheckTask", task, "New / New / green, red");
                } else {
                    for (String name : names(PHASES, STATUSES, TAGS))
                        drawn(property(form, name), element.equals("List") ? "list" : element.equals("Button") ? "button" : "buttonGroup",
                                isHtml(name), views(name), isTags(name));
                    shownBy(form, PHASES, "New");
                    shownBy(form, STATUSES, "New");
                    shownBy(form, TAGS, "red");
                    pick(form, "phaseHtmlValue", "Done");
                    shownBy(form, PHASES, "Done");
                    pick(form, "statusHtml", "Lost");
                    shownBy(form, STATUSES, "Lost");
                    pick(form, "tagsHtml", "green");
                    shownBy(form, TAGS, "red", "green");
                    save(form);
                    check("UiSelect.uiSelectCheckTask", task, "Done / Lost / green, red");
                }
                close(form);
            }
        } finally {
            exec("UiSelect.uiSelectDefaultTheme");
        }
    }

    // a server answer while excel's list stays open used to leave multiple-select's options and its picked views as
    // they were: the skipped refresh must run once the list closes, and the next pick must use the added option
    @Test
    public void excelCatchesUpWithAnUpdateWhileItsListWasOpen() throws Exception {
        String task = seedTask();
        try {
            exec("UiSelect.uiSelectExcel");
            page.reload();
            page.locator("[lsfusion-container='System.root']").waitFor();
            Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectOpenDropdown");
            excelPicked(form, new String[] {"tags"}, "red");
            Locator field = property(form, "tags").locator(".ms-choice");
            field.click();
            assertThat(excelOptions()).hasText(TAG_VIEWS);
            excelOption("green").click();
            assertThat(property(form, "tags").locator("select option")).hasText(new String[] {"amber", "blue", "lime", "red"});
            assertThat(property(form, "tagsText").locator(".panel-renderer-value")).hasText("lime, red");
            assertThat(page.locator(".ms-drop:visible")).hasCount(1); // the update arrived with the list still open
            field.click();
            excelPicked(form, new String[] {"tags"}, "lime, red");
            field.click();
            assertThat(excelOptions()).hasText(new String[] {"amber", "blue", "lime", "red"});
            for (String tag : new String[] {"lime", "red"})
                assertThat(excelOption(tag).locator("input")).isChecked();
            excelOption("amber").click();
            assertThat(property(form, "tagsText").locator(".panel-renderer-value")).hasText("amber, lime, red");
            field.click();
            excelPicked(form, new String[] {"tags"}, "amber, lime, red");
            cancel(form);
            close(form);
            check("UiSelect.uiSelectCheckTask", task, "New / New / red");
        } finally {
            exec("UiSelect.uiSelectDefaultTheme");
        }
    }

    // each element in a column of a table, a cell in each row: the row's selection, which a pick in the cell changes -
    // whether the row was the current one or not. The keys: the grid's arrows go from cell to cell into the options of
    // one, where shift with an arrow picks the next option and the grid stays in its row, an arrow alone moves it; in
    // the cell of the input enter opens its list, escape closes it - not the form -, and with the list closed an arrow
    // moves the grid. A pick given to the property's explicit INPUT is left out: in a table it changes nothing, in the
    // current row or another - the click only makes the row current (master too)
    @Test
    public void inATable() throws Exception {
        String[] tasks = seedTasks();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectGrid");
        Locator first = row(form, "number", tasks[0]), second = row(form, "number", tasks[1]);
        shown(cell(first, "list"), STATUS_VIEWS, "New");
        shown(cell(second, "list"), STATUS_VIEWS);

        click(cell(first, "number"));
        for (int i = 0; i < 3; i++)
            press("ArrowRight"); // the list's cell, the buttons', the button group's
        press("Shift+ArrowRight");
        shown(cell(first, "list"), STATUS_VIEWS, "Done");
        assertThat(first).hasClass(Pattern.compile("\\btable-active\\b"));
        press("ArrowDown");
        assertThat(second).hasClass(Pattern.compile("\\btable-active\\b"));
        shown(cell(first, "buttonGroup"), STATUS_VIEWS, "Done");
        for (int i = 0; i < 3; i++)
            press("ArrowRight"); // the drop-down list's cell, the one of several values', the input's
        press("Enter");
        assertThat(lookedUp()).hasText(new String[] {"red", "green", "blue"});
        press("Escape");
        assertThat(lookedUp()).hasCount(0);
        assertThat(form).isVisible();
        press("ArrowUp");
        assertThat(first).hasClass(Pattern.compile("\\btable-active\\b"));

        text(cell(second, "list"), "Lost").click();
        shown(cell(second, "button"), STATUS_VIEWS, "Lost");
        text(cell(first, "button"), "New").click();
        shown(cell(first, "buttonGroup"), STATUS_VIEWS, "New");
        text(cell(second, "buttonGroup"), "Lost").click(); // picked again: cleared
        shown(cell(second, "list"), STATUS_VIEWS);
        cell(second, "dropdown").locator("select").selectOption(new SelectOption().setLabel("Done"));
        shown(cell(second, "buttonGroup"), STATUS_VIEWS, "Done");
        Locator field = cell(second, "tagsDropdown").locator(".select2-selection");
        field.click();
        resultOption("green").click();
        field.click();
        assertThat(resultOptions()).hasCount(0);
        assertThat(cell(second, "tagsDropdown").locator(".select2-selection__choice__display")).hasText(new String[] {"green"});
        items(cell(second, "tagsInput"), "green");
        shown(cell(first, "phase"), STATUS_VIEWS, "New");
        shown(cell(second, "phase"), STATUS_VIEWS, "Done");
        for (String name : new String[] {"tagsList", "tagsButton"}) {
            drawn(cell(second, name), name.equals("tagsList") ? "list" : "button", false, TAG_VIEWS, true);
            shown(cell(second, name), TAG_VIEWS, "green");
        }

        click(cell(first, "number"));
        text(cell(second, "phase"), "Lost").click(); // a value that has to be there, in another row
        shown(cell(second, "phase"), STATUS_VIEWS, "Lost");
        shown(cell(first, "phase"), STATUS_VIEWS, "New");
        click(cell(first, "number"));
        cell(second, "statusHtml").locator(".select2-selection").click();
        assertThat(resultOption("Lost").locator("b")).hasText("Lost");
        resultOption("Lost").click();
        assertThat(cell(second, "statusHtml").locator(".select2-selection__rendered b")).hasText("Lost");
        shown(cell(second, "list"), STATUS_VIEWS, "Lost");
        shown(cell(second, "statusValue"), STATUS_VIEWS, "Lost"); // its pick is left out: lost in a table, a bug of its own
        click(cell(first, "number"));
        text(cell(second, "tagsList"), "blue").click();
        shown(cell(second, "tagsButton"), TAG_VIEWS, "blue", "green");
        click(cell(first, "number"));
        text(cell(second, "tagsButton"), "green").click();
        shown(cell(second, "tagsList"), TAG_VIEWS, "blue");
        shown(cell(first, "tagsList"), TAG_VIEWS, "red");
        save(form);
        check("UiSelect.uiSelectCheckTask", tasks[0], "New / New / red");
        check("UiSelect.uiSelectCheckTask", tasks[1], "Lost / Lost / blue");
        close(form);
    }

    // HTML in every table element, a required native drop-down and select2 of several HTML values: a pick in another
    // row changes that row alone; the explicit INPUT column is only drawn, since its pick is lost in a table
    @Test
    public void htmlAndRequiredValuesInATable() throws Exception {
        String[] tasks = seedTasks();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectHtmlGrid");
        Locator first = row(form, "number", tasks[0]), second = row(form, "number", tasks[1]);
        for (Locator row : new Locator[] {first, second}) {
            for (String name : new String[] {"listHtml", "buttonHtml", "buttonGroupHtml", "statusHtmlValue"})
                drawn(cell(row, name), name.equals("listHtml") ? "list" : name.equals("buttonGroupHtml") ? "buttonGroup" : "button",
                        true, STATUS_VIEWS, false);
            Locator phase = cell(row, "phase").locator("select");
            assertThat(phase).hasClass(Pattern.compile("\\bform-select\\b"));
            assertThat(phase.locator("option")).hasText(new String[] {"", "New", "Done", "Lost"});
            assertThat(phase.locator("option").first()).hasAttribute("hidden", "");
            assertThat(cell(row, "statusHtml").locator(".select2-container")).hasCount(1);
            assertThat(cell(row, "tagsHtmlDropdown").locator(".select2-container")).hasCount(1);
            assertThat(cell(row, "tagsHtmlInput").locator(".selectize-control")).hasCount(1);
        }
        assertThat(cell(first, "phase").locator("select option:checked")).hasText("New");
        assertThat(cell(second, "phase").locator("select option:checked")).hasText("Done");
        assertThat(cell(first, "statusHtml").locator(".select2-selection__rendered b")).hasText("New");
        assertThat(cell(first, "tagsHtmlDropdown").locator(".select2-selection__choice__display i")).hasText("red");
        assertThat(cell(first, "tagsHtmlInput").locator(".selectize-input .item i")).hasText("red");
        items(cell(second, "tagsHtmlInput"));
        for (String name : new String[] {"listHtml", "buttonHtml", "buttonGroupHtml"}) {
            click(cell(first, "number"));
            assertThat(first).hasClass(Pattern.compile("\\btable-active\\b"));
            String next = name.equals("buttonHtml") ? "Lost" : "Done";
            text(cell(second, name), next).click();
            for (String other : new String[] {"listHtml", "buttonHtml", "buttonGroupHtml", "statusHtmlValue"})
                shown(cell(second, other), STATUS_VIEWS, next);
            assertThat(cell(second, "statusHtml").locator(".select2-selection__rendered b")).hasText(next);
        }
        click(cell(first, "number"));
        cell(second, "phase").locator("select").selectOption(new SelectOption().setLabel("Lost"));
        assertThat(cell(second, "phase").locator("select option:checked")).hasText("Lost");
        click(cell(first, "number"));
        cell(second, "statusHtml").locator(".select2-selection").click();
        assertThat(resultOptions().locator("b")).hasText(STATUS_VIEWS);
        resultOption("Lost").click();
        shown(cell(second, "listHtml"), STATUS_VIEWS, "Lost");
        click(cell(first, "number"));
        Locator field = cell(second, "tagsHtmlDropdown").locator(".select2-selection");
        field.click();
        assertThat(resultOptions().locator("i")).hasText(TAG_VIEWS);
        resultOption("green").click();
        resultOption("blue").click();
        field.click();
        assertThat(resultOptions()).hasCount(0);
        assertThat(cell(second, "tagsHtmlDropdown").locator(".select2-selection__choice__display i")).hasText(new String[] {"blue", "green"});
        items(cell(second, "tagsHtmlInput"), "blue", "green");
        click(cell(first, "number"));
        Locator search = cell(second, "tagsHtmlInput").locator(".selectize-input input");
        search.click();
        assertThat(lookedUp().locator("i")).hasText(new String[] {"red"});
        search.pressSequentially("r");
        assertThat(lookedUp().locator("i")).hasText(new String[] {"red"});
        lookedUp().first().click();
        items(cell(second, "tagsHtmlInput"), "blue", "green", "red");
        for (String tag : TAG_VIEWS)
            assertThat(cell(second, "tagsHtmlInput").locator(".selectize-input .item i").filter(new Locator.FilterOptions().setHasText(exact(tag)))).hasCount(1);
        shown(cell(first, "listHtml"), STATUS_VIEWS, "New");
        assertThat(cell(first, "phase").locator("select option:checked")).hasText("New");
        items(cell(first, "tagsHtmlInput"), "red");
        save(form);
        check("UiSelect.uiSelectCheckTask", tasks[0], "New / New / red");
        check("UiSelect.uiSelectCheckTask", tasks[1], "Lost / Lost / blue, green, red");
        close(form);
    }

    // an element read-only: drawn so - the options marked, not disabled -, attempts to pick, drop and type changing
    // nothing on the server, of every element kind; disabled: its controls disabled. The read-only select2 list of
    // several values opens and takes a pick locally, so only the server's value is checked
    @Test
    public void readOnly() throws Exception {
        String task = seedTask();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectReadonly");
        trackRequests();
        shownBy(form, new String[] {"list", "button", "buttonGroup", "disabledList"}, "New");
        for (String name : new String[] {"list", "button", "buttonGroup"}) {
            Locator inputs = property(form, name).locator("input");
            assertThat(inputs).hasCount(3);
            for (int i = 0; i < 3; i++) {
                assertThat(inputs.nth(i)).hasClass(Pattern.compile("\\bis-readonly\\b"));
                assertThat(inputs.nth(i)).isEnabled();
            }
            text(property(form, name), "Done").click();
            press(property(form, name).locator("input").first(), "ArrowDown");
            unchangedTask(form, task);
        }
        assertThat(property(form, "dropdown").locator("select")).hasClass(Pattern.compile("\\bis-readonly\\b"));
        assertThat(property(form, "dropdown").locator("select")).isEnabled();
        property(form, "dropdown").locator("select").selectOption(new SelectOption().setLabel("Done"));
        press(property(form, "dropdown").locator("select"), "ArrowDown");
        unchangedTask(form, task);
        assertThat(property(form, "statusHtml").locator(".select2-selection")).hasClass(Pattern.compile("\\bis-readonly\\b"));
        dropdownPick(form, "statusHtml", "Done");
        unchangedTask(form, task);
        assertThat(property(form, "tagsDropdown").locator(".select2-selection")).hasClass(Pattern.compile("\\bis-readonly\\b"));
        Locator field = property(form, "tagsDropdown").locator(".select2-selection");
        field.click();
        resultOption("green").click();
        resultOption("red").click(); // a drop of the picked option, in the open list
        field.click();
        unchangedTask(form, task);
        assertThat(property(form, "tagsInput").locator(".selectize-input")).hasClass(Pattern.compile("\\blocked\\b"));
        property(form, "tagsInput").locator(".item a.remove").click();
        Locator search = property(form, "tagsInput").locator(".selectize-input input");
        search.click();
        press(search, "Backspace");
        press(search, "g");
        unchangedTask(form, task);

        Locator disabled = property(form, "disabledList").locator("input");
        for (int i = 0; i < 3; i++)
            assertThat(disabled.nth(i)).isDisabled();
        text(property(form, "disabledList"), "Done").click(new Locator.ClickOptions().setForce(true));
        assertThat(property(form, "disabledDropdown").locator("select")).isDisabled();
        assertThat(property(form, "disabledTagsInput").locator(".selectize-input")).hasClass(Pattern.compile("\\bdisabled\\b"));
        unchangedTask(form, task);
        close(form); // with nothing changed: no question about saving
    }

    // HTML and explicit INPUT kinds read-only: their controls stay enabled, but a click, a toggle, a drop or a typed
    // lookup leaves the server's task unchanged, including the HTML multi lists and the input
    @Test
    public void htmlAndValueSelectionsStayReadOnly() throws Exception {
        String task = seedTask();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectReadonlyKinds");
        trackRequests();
        for (String name : new String[] {"listHtml", "buttonHtml", "buttonGroupHtml", "listValue", "buttonValue", "buttonGroupValue"}) {
            drawn(property(form, name), name.startsWith("list") ? "list" : name.startsWith("buttonGroup") ? "buttonGroup" : "button",
                    isHtml(name), STATUS_VIEWS, false);
            shown(property(form, name), STATUS_VIEWS, "New");
            Locator inputs = property(form, name).locator("input");
            for (int i = 0; i < 3; i++) {
                assertThat(inputs.nth(i)).hasClass(Pattern.compile("\\bis-readonly\\b"));
                assertThat(inputs.nth(i)).isEnabled();
            }
            pick(form, name, "Done");
            unchangedTask(form, task);
        }
        assertThat(property(form, "statusValue").locator("select")).hasClass(Pattern.compile("\\bis-readonly\\b"));
        dropdownPick(form, "statusValue", "Done");
        unchangedTask(form, task);
        assertThat(property(form, "statusHtmlValue").locator(".select2-selection")).hasClass(Pattern.compile("\\bis-readonly\\b"));
        dropdownPick(form, "statusHtmlValue", "Done");
        unchangedTask(form, task);
        assertThat(property(form, "tagsHtmlDropdown").locator(".select2-selection")).hasClass(Pattern.compile("\\bis-readonly\\b"));
        Locator field = property(form, "tagsHtmlDropdown").locator(".select2-selection");
        field.click();
        assertThat(resultOptions().locator("i")).hasText(TAG_VIEWS);
        resultOption("green").click();
        resultOption("red").click();
        field.click();
        unchangedTask(form, task);
        assertThat(property(form, "tagsHtmlInput").locator(".selectize-input")).hasClass(Pattern.compile("\\blocked\\b"));
        assertThat(property(form, "tagsHtmlInput").locator(".selectize-input .item i")).hasText("red");
        property(form, "tagsHtmlInput").locator(".item a.remove").click();
        Locator search = property(form, "tagsHtmlInput").locator(".selectize-input input");
        search.click();
        press(search, "Backspace");
        press(search, "g");
        unchangedTask(form, task);
        close(form);
    }

    // READONLYIF follows the task's flag: a pick changes nothing while it is set, then the same HTML element with an
    // explicit INPUT becomes writable when the flag is dropped and the server keeps its pick
    @Test
    public void aSelectionBecomesWritable() throws Exception {
        String task = seedTask();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectReadonlyIf");
        trackRequests();
        Locator element = property(form, "statusHtmlValue").locator(".panel-renderer-value"), inputs = element.locator("input");
        drawn(element, "buttonGroup", true, STATUS_VIEWS, false);
        for (int i = 0; i < 3; i++)
            assertThat(inputs.nth(i)).hasClass(Pattern.compile("\\bis-readonly\\b"));
        text(element, "Lost").click();
        unchangedTask(form, task);
        propertyButton(form, "unlock").click();
        for (int i = 0; i < 3; i++) {
            assertThat(inputs.nth(i)).not().hasClass(Pattern.compile("\\bis-readonly\\b"));
            assertThat(inputs.nth(i)).isEnabled();
        }
        text(element, "Done").click();
        shown(element, STATUS_VIEWS, "Done");
        save(form);
        check("UiSelect.uiSelectCheckTask", task, "New / Done / red");
        close(form);
    }

    // the platform's element drawing the rows of a group, the group's render function named by it without an argument -
    // a list, buttons, a button group, a drop-down list, and buttons of one value or none by their old name, option -:
    // the rows its options, a pick changing their `selected`;
    // and the input a selection of several values drawn as its text is changed in, the form of its change, embedded in
    // place of the text with the options picked, closed once the focus leaves it. Its list is left out: opened by a click
    // that form's input loads the options and shows no list - a bug of its own
    @Test
    public void overTheRowsOfAGroup() throws Exception {
        String task = seedTask();
        Locator form = open("UiSelect.uiSelect", "UiSelect.uiSelectRows");
        Locator group = form.locator("[lsfusion-container='GRID(groupStatus)']"), option = form.locator("[lsfusion-container='GRID(optionStatus)']");
        assertThat(group.locator(".option-btn-container.btn-group")).hasCount(1);
        assertThat(option.locator(".option-btn-container:not(.btn-group)")).hasCount(1);
        shown(group, STATUS_VIEWS, "New");
        shown(option, STATUS_VIEWS, "New");

        text(group, "Done").click();
        shown(option, STATUS_VIEWS, "Done");
        text(group, "Done").click(); // picked again: kept
        shown(option, STATUS_VIEWS, "Done");
        text(option, "Done").click(); // picked again: cleared
        shown(group, STATUS_VIEWS);
        text(option, "Lost").click();
        shown(group, STATUS_VIEWS, "Lost");

        for (String name : new String[] {"listStatus", "buttonStatus"}) {
            Locator rows = form.locator("[lsfusion-container='GRID(" + name + ")']");
            drawn(name.equals("listStatus") ? rows.locator(".option-container") : rows.locator(".option-btn-container").locator(".."),
                    name.equals("listStatus") ? "list" : "button", false, STATUS_VIEWS, false);
            shown(rows, STATUS_VIEWS, "Lost");
            text(rows, "Done").click();
            shown(group, STATUS_VIEWS, "Done");
            text(rows, "Done").click(); // without an argument: a value that has to be there
            shown(group, STATUS_VIEWS, "Done");
            text(option, "Lost").click();
            shown(rows, STATUS_VIEWS, "Lost");
        }
        Locator dropdown = form.locator("[lsfusion-container='GRID(dropdownStatus)'] select");
        assertThat(dropdown.locator("option")).hasText(new String[] {"", "New", "Done", "Lost"});
        assertThat(dropdown.locator("option").first()).hasAttribute("hidden", "");
        assertThat(dropdown.locator("option:checked")).hasText("Lost");
        dropdown.selectOption(new SelectOption().setLabel("Done"));
        shown(group, STATUS_VIEWS, "Done");
        shown(option, STATUS_VIEWS, "Done");

        Locator text = property(form, "tagsText");
        assertThat(text).hasText(Pattern.compile("red"));
        text.locator(".panel-renderer-value").first().click(); // a panel property changes on one click: a second would open its form again
        items(page.locator("body"), "red");
        page.locator(".selectize-input .item").filter(new Locator.FilterOptions().setHasText("red")).locator("a.remove").click();
        items(page.locator("body"));
        form.getByText("Status", new Locator.GetByTextOptions().setExact(true)).first().click(); // the focus leaves it
        assertThat(page.locator(".selectize-control")).hasCount(0);
        assertThat(text).not().hasText(Pattern.compile("red"));
        save(form);
        check("UiSelect.uiSelectCheckTask", task, "New / Done / none");
        close(form);
    }

    // the selections of a task on the form of each element (uiSelectElementForm), by their names there: of one value that
    // has to be there (its phase), of one value or none (its status), of several values (its tags); a pick of one value
    // given to the property or, Value, to its explicit INPUT as the option's view; the options viewed as text or, Html,
    // as HTML
    private static final String[] PHASES = {"phase", "phaseValue", "phaseHtml", "phaseHtmlValue"};
    private static final String[] STATUSES = {"status", "statusValue", "statusHtml", "statusHtmlValue"};
    private static final String[] TAGS = {"tags", "tagsHtml"};
    // ... the ones of one value a drop-down list draws natively, as text, and by select2, as HTML
    private static final String[] NATIVE = {"phase", "phaseValue", "status", "statusValue"};
    private static final String[] PICKED = {"phaseHtml", "phaseHtmlValue", "statusHtml", "statusHtmlValue"};
    // the views of the options, in their order - the statuses as created, the tags by name (their CONCAT's ORDER) -, and
    // after the server's own change (uiSelectShift)
    private static final String[] STATUS_VIEWS = {"New", "Done", "Lost"};
    private static final String[] TAG_VIEWS = {"blue", "green", "red"};
    private static final String[] SHIFTED_STATUS_VIEWS = {"Fresh", "Done", "Late"};
    private static final String[] SHIFTED_TAG_VIEWS = {"black", "crimson", "green"};

    private static boolean isTags(String name) {
        return name.startsWith("tags");
    }

    private static boolean isHtml(String name) {
        return name.contains("Html");
    }

    private static String[] views(String name) {
        return isTags(name) ? TAG_VIEWS : STATUS_VIEWS;
    }

    private static String[] names(String[]... names) {
        return Arrays.stream(names).flatMap(Arrays::stream).toArray(String[]::new);
    }

    // a property of a form by its name there: in a panel its component, in a table its cells
    private static Locator property(Locator form, String name) {
        return form.locator("[lsfusion-container='PROPERTY(" + name + ")']");
    }

    private static Locator cell(Locator row, String name) {
        return row.locator("td[lsfusion-container='PROPERTY(" + name + ")']");
    }

    // an element of options as its kind draws it: a column of radio buttons - check boxes of several values - with their
    // labels, or such buttons standing apart or in a group; the labels the options' views in their order, as text or as
    // HTML
    private static void drawn(Locator property, String element, boolean html, String[] views, boolean several) {
        Locator inputs = property.locator("input"), labels = property.locator("label.option-item");
        assertThat(inputs).hasCount(views.length);
        for (int i = 0; i < views.length; i++)
            assertThat(inputs.nth(i)).hasAttribute("type", several ? "checkbox" : "radio");
        assertThat(labels).hasText(views);
        assertThat(property.locator("label.option-item.html-or-text-is-html")).hasCount(html ? views.length : 0);
        assertThat(labels.locator("b, i")).hasCount(html ? views.length : 0);
        switch (element) {
            case "list":
                assertThat(property).hasClass(Pattern.compile("\\boption-container\\b"));
                assertThat(property.locator(":scope > .form-check > input.form-check-input")).hasCount(views.length);
                break;
            case "button":
                assertThat(property.locator(":scope > .option-btn-container:not(.btn-group) > input.btn-check")).hasCount(views.length);
                break;
            default:
                assertThat(property.locator(":scope > .option-btn-container.btn-group[role=group] > input.btn-check")).hasCount(views.length);
        }
    }

    // the options each element of the same selection shows picked, by their views - the others not
    private static void shownBy(Locator form, String[] names, String... picked) {
        for (String name : names)
            shown(property(form, name), views(name), picked);
    }

    // ... of the views given
    private static void shownOf(Locator form, String[] names, String[] views, String... picked) {
        for (String name : names)
            shown(property(form, name), views, picked);
    }

    // ... by one element
    private static void shown(Locator element, String[] views, String... picked) {
        List<String> pickedViews = Arrays.asList(picked);
        for (String view : views) {
            Locator input = element.getByLabel(view, new Locator.GetByLabelOptions().setExact(true));
            if (pickedViews.contains(view))
                assertThat(input).isChecked();
            else
                assertThat(input).not().isChecked();
        }
    }

    // a click on the option of the view
    private static void pick(Locator form, String name, String view) {
        text(property(form, name), view).click();
    }

    // of one value that has to be there none picked: the element marks its options - list's radio buttons invalid,
    // buttons outlined as a danger
    private static void missing(Locator property, String element, boolean missing) {
        assertThat(property.locator(element.equals("list") ? "input.is-invalid" : "label.btn-outline-danger")).hasCount(missing ? 3 : 0);
        assertThat(property.locator(element.equals("list") ? "input:not(.is-invalid)" : "label.btn-outline-secondary")).hasCount(missing ? 0 : 3);
    }

    // the option a drop-down list of one value shows picked, '' for none: the native one's option, select2's rendering
    private static void dropdownPicked(Locator form, String[] names, String view) {
        for (String name : names)
            if (Arrays.asList(NATIVE).contains(name))
                assertThat(property(form, name).locator("select option:checked")).hasText(view);
            else
                assertThat(property(form, name).locator(".select2-selection__rendered")).hasText(view);
    }

    // the options of each drop-down list in their order: native, select2, the HTML in the list drawn as markup
    private void dropdownOptions(Locator form, String[] statuses, String[] tags) {
        for (String name : NATIVE)
            assertThat(property(form, name).locator("select option")).hasText(Stream.concat(Stream.of(""), Arrays.stream(statuses)).toArray(String[]::new));
        for (String name : names(PICKED, TAGS)) {
            Locator field = property(form, name).locator(".select2-selection");
            field.click();
            assertThat(isHtml(name) ? resultOptions().locator(isTags(name) ? "i" : "b") : resultOptions()).hasText(isTags(name) ? tags : statuses);
            field.click();
            assertThat(resultOptions()).hasCount(0);
        }
    }

    // a pick in a drop-down list: of the native one its option by the view, '' the empty one; of select2's the option of
    // the list a click on it opens, closed after a pick of several values
    private void dropdownPick(Locator form, String name, String view) {
        if (Arrays.asList(NATIVE).contains(name)) {
            Locator select = property(form, name).locator("select");
            if (view.isEmpty())
                select.selectOption(new SelectOption().setIndex(0));
            else
                select.selectOption(new SelectOption().setLabel(view));
        } else {
            property(form, name).locator(".select2-selection").click();
            resultOption(view).click();
            if (isTags(name)) {
                property(form, name).locator(".select2-selection").click();
                assertThat(resultOptions()).hasCount(0);
            }
        }
    }

    // the options select2 shows picked in each drop-down list of several values, in their order
    private static void choices(Locator form, String[] names, String... views) {
        for (String name : names)
            assertThat(property(form, name).locator(".select2-selection__choice__display")).hasText(views);
    }

    // the options of the list a select2 drop-down list has opened, and one of them by its view - '' the empty one
    private Locator resultOptions() {
        return page.locator(".select2-container--open .select2-results__option");
    }

    private Locator resultOption(String view) {
        return resultOptions().filter(new Locator.FilterOptions().setHasText(exact(view)));
    }

    // multiple-select's field in excel, and the options of its open list, appended to the body
    private static void excelPicked(Locator form, String[] names, String view) {
        for (String name : names)
            assertThat(property(form, name).locator(".ms-choice > span")).hasText(view);
    }

    private Locator excelOptions() {
        return page.locator(".ms-drop:visible li label");
    }

    // ... the one of the view, its label holding the text with the space around it
    private Locator excelOption(String view) {
        return excelOptions().filter(new Locator.FilterOptions().setHasText(Pattern.compile("^\\s*" + escaped(view) + "\\s*$")));
    }

    // the options picked an input shows in each input of the same selection - an item each, with its cross
    private static void items(Locator form, String[] names, String... views) {
        for (String name : names)
            items(property(form, name), views);
    }

    private static void items(Locator element, String... views) {
        Locator items = element.locator(".selectize-input .item");
        assertThat(items).hasCount(views.length);
        for (String view : views)
            assertThat(items.filter(new Locator.FilterOptions().setHasText(Pattern.compile("^" + escaped(view) + "×?$")))).hasCount(1);
    }

    // the text as a whole, a pattern playwright matches in javascript, which has no \Q...\E of Pattern.quote
    private static Pattern exact(String text) {
        return Pattern.compile("^" + escaped(text) + "$");
    }

    private static String escaped(String text) {
        return text.replaceAll("[\\\\^$.*+?()\\[\\]{}|-]", "\\\\$0");
    }

    // the options an input's list holds, looked up by the text typed
    private Locator lookedUp() {
        return page.locator(".selectize-dropdown:visible [data-selectable]");
    }

    // the changes of the form's session dropped, as its Cancel asks: confirmed
    private void cancel(Locator form) {
        form.locator("button[lsfusion-container='PROPERTY(formCancel())']").click();
        dialog("Do you really want to undo changes?").getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Yes").setExact(true)).click();
        assertThat(dialog("Do you really want to undo changes?")).hasCount(0);
    }

    // an attempt in a read-only element: save any change it might have made where the form has a save button,
    // then check what the server kept
    private void unchangedTask(Locator form, String task) throws Exception {
        settled();
        Locator apply = propertyButton(form, "formApply()");
        if (apply.count() > 0 && apply.isEnabled())
            save(form);
        check("UiSelect.uiSelectCheckTask", task, "New / New / red");
    }

    // a task under a name no other attempt has, of the phase New, the status New and the tag red, shown from now on
    private static String seedTask() throws Exception {
        String task = "t-" + System.nanoTime();
        exec("UiSelect.uiSelectSeedTask", task);
        return task;
    }

    // ... two of them, the second of the phase Done, no status and no tag
    private static String[] seedTasks() throws Exception {
        String[] tasks = {"t1-" + System.nanoTime(), "t2-" + System.nanoTime()};
        exec("UiSelect.uiSelectSeedTasks", tasks);
        return tasks;
    }

    // two orders under names no other attempt has - what a failed attempt saved stays in the database for its rerun -:
    // the first New and red, the second with no status and no tag
    private static String[] seed() throws Exception {
        String[] orders = {"1-" + System.nanoTime(), "2-" + System.nanoTime()};
        exec("UiSelect.uiSelectSeed", orders);
        return orders;
    }

    // what the view is given (window.uiSelectData, set as it draws) as `data`, and picked(entry) - the values of the
    // options of its `select` list that are picked, sorted, joined
    private static final String DATA = "const data = window.uiSelectData;"
            + " const picked = entry => entry.select.list.filter(o => o.selected).map(o => o.value).sort().join();";
    private final View view = new View(DATA, "!!data && !JSON.stringify(data).includes('\"loading\"')",
            "the view is given ", "window.uiSelectData");

    private void expect(String expression, String expected) {
        assertEquals(expression, expected, view.value(expression));
    }

    private static void save(Locator form) {
        form.locator("button[lsfusion-container='PROPERTY(formApply())']").click();
    }
}
