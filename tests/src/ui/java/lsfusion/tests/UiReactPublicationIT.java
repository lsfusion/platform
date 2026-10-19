package lsfusion.tests;

import com.microsoft.playwright.Locator;
import org.junit.Test;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.Assert.assertEquals;

// when a React view's projection is published: what a member writes is published before the member returns, inside the
// event handler that called it, and what the form is set out with goes out with its first changes. React puts a
// controlled input back to what its store says once the handler is done, so a publication after it moves the caret;
// and a window is measured when it is shown, from what the view draws of the form's first changes
public class UiReactPublicationIT extends UiTest {

    // a controlled input written back through a member from its onChange keeps what the user types in the middle of it
    // and the caret where it is typed, right after each key and once the server has answered the change: a property's
    // value, beside the echo of it the server computes, and a condition on the group's name, beside the rows it leaves
    @Test
    public void aControlledInputKeepsItsCaret() throws Exception {
        long id = System.nanoTime(); // unique : the items of a failed attempt stay in the database for its rerun
        // in their order on the form; each key typed into the search leaves fewer of them
        String[] names = {"a " + id, "ap " + id, "apl " + id, "aple " + id, "apple " + id, "b " + id};
        exec("UiReactPublication.uiReactPublicationSeed", names);
        Locator form = open("UiReactPublication.uiReactPublication", "UiReactPublication.uiReactPublicationInputs");
        Locator note = form.locator(".ui-react-publication-note"), echo = form.locator(".ui-react-publication-echo"),
                search = form.locator(".ui-react-publication-search"), rows = form.locator(".ui-react-publication-rows");
        assertThat(note).hasValue("abcdef");
        assertThat(echo).hasText("[abcdef]");
        assertThat(rows).hasText(String.join(", ", names));

        typeAt(note, 2, "XY", value -> assertThat(echo).hasText("[" + value + "]"));
        assertThat(note).hasValue("abXYcdef");

        Consumer<String> filtered = value -> assertThat(rows).hasText(Arrays.stream(names).filter(name -> name.contains(value))
                .collect(Collectors.joining(", ")));
        typeAt(search, 0, "aple", filtered);
        typeAt(search, 1, "p", filtered);
        assertThat(search).hasValue("apple");
        close(form);
    }

    // a window with the default main container and a view taller than its default content is measured with the view
    // drawn: the view fits it, and nothing in the window scrolls
    @Test
    public void aWindowIsMeasuredWithItsViewDrawn() throws Exception {
        open("UiReactPublication.uiReactPublication", "UiReactPublication.uiReactPublicationOpenTall");
        Locator window = form("UiReactPublication.uiReactPublicationTall"), tall = window.locator(".ui-react-publication-tall");
        assertThat(tall).isVisible();
        // what holds the view and scrolls - its content beyond its size, its overflow other than visible -, up to the
        // page
        assertEquals("", tall.evaluate("view => { const scrolled = [];"
                + " for (let e = view.parentElement; e && e !== document.body; e = e.parentElement) {"
                + "  const style = getComputedStyle(e);"
                + "  if (e.scrollHeight > e.clientHeight + 2 && style.overflowY !== 'visible') scrolled.push(e.className + ' ' + e.clientHeight + ' < ' + e.scrollHeight);"
                + "  if (e.scrollWidth > e.clientWidth + 2 && style.overflowX !== 'visible') scrolled.push(e.className + ' width ' + e.clientWidth + ' < ' + e.scrollWidth); }"
                + " return scrolled.join('; '); }"));
        close(window);
    }

    // the text typed one key at a time, the caret put at `at` first. Right after each key - its handler over, what the
    // member it calls writes published in it - and again once the server's answer to the change is applied, which
    // `answered` waits for by what only that answer shows, the input holds what is typed and the caret is right after
    // the key. A change through a member is no request with a glass: the next key waits for the answer instead
    private void typeAt(Locator input, int at, String text, Consumer<String> answered) {
        input.evaluate("(input, at) => { input.focus(); input.setSelectionRange(at, at); }", at);
        String value = (String) input.evaluate("input => input.value");
        for (int i = 0; i < text.length(); i++) {
            int caret = at + i + 1;
            value = value.substring(0, caret - 1) + text.charAt(i) + value.substring(caret - 1);
            page.keyboard().press(text.substring(i, i + 1));
            typed(input, value, caret);
            answered.accept(value);
            typed(input, value, caret);
        }
    }

    private static void typed(Locator input, String value, int caret) {
        assertEquals("the value typed", value, input.evaluate("input => input.value"));
        assertEquals("the caret in " + value, caret, ((Number) input.evaluate("input => input.selectionStart")).intValue());
    }
}
