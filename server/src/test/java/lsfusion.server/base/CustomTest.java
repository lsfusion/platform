package lsfusion.server.base;

import org.junit.Test;

import static org.junit.Assert.*;

// what a `custom` value is, and what its <Lsf:name> places name. Custom is the single point of determination for
// every surface CUSTOM draws - a navigator window, the forms and log windows, a DESIGN container - and the client
// half of the place protocol (GwtClientUtils.getLsfPlace / unwrapLsfPlaces) is written against exactly these rules.
public class CustomTest {

    // the mapper the walker is given; here it just marks what it was handed, so the walk itself is what is tested
    private static String walk(String template) throws Custom.PlaceError {
        return Custom.mapPlaces(template, name -> "[" + name + "]");
    }

    private static void placeError(String template, String expected) {
        try {
            walk(template);
            fail("expected a PlaceError for: " + template);
        } catch (Custom.PlaceError e) {
            assertEquals(expected, e.getMessage());
        }
    }

    // ---------------------------------------------------------------- what names a React component

    @Test
    public void componentNameIsUpperCamel() {
        assertTrue(Custom.isReactComponent("A"));
        assertTrue(Custom.isReactComponent("Board"));
        assertTrue(Custom.isReactComponent("FormsBoard"));
        assertTrue(Custom.isReactComponent("B1"));
        assertTrue(Custom.isReactComponent("B_$x9"));
    }

    @Test
    public void componentNameIsNotAnythingElse() {
        assertFalse(Custom.isReactComponent(null));
        assertFalse(Custom.isReactComponent(""));
        assertFalse(Custom.isReactComponent("board"));      // the mistake the whole rule exists for
        assertFalse(Custom.isReactComponent("_Board"));
        assertFalse(Custom.isReactComponent("1Board"));
        assertFalse(Custom.isReactComponent("Board-2"));
        assertFalse(Custom.isReactComponent("Board 2"));
        assertFalse(Custom.isReactComponent(" Board"));
        assertFalse(Custom.isReactComponent("Board\n"));    // matches(), so the whole value or nothing
        assertFalse(Custom.isReactComponent("app.Board"));
    }

    // a component name is resolved as a JS global, so it is ASCII by construction - stated here so that widening it
    // is a decision rather than an accident
    @Test
    public void componentNameIsAscii() {
        assertFalse(Custom.isReactComponent("Ёлка"));
        assertFalse(Custom.isReactComponent("Ärger"));
    }

    // ---------------------------------------------------------------- what counts as markup

    @Test
    public void templateIsMarkup() {
        assertTrue(Custom.isHtmlTemplate("<div></div>"));
        assertTrue(Custom.isHtmlTemplate("text before <b>x</b> and after"));
        assertTrue(Custom.isHtmlTemplate("</div>"));           // a tag is a tag, even a closing one
        assertTrue(Custom.isHtmlTemplate("<!-- a comment -->"));
        assertTrue(Custom.isHtmlTemplate("<Lsf:orders>"));
        assertTrue(Custom.isHtmlTemplate("line\nline\n<div>")); // DOTALL: markup on any line counts
        assertTrue(Custom.isHtmlTemplate(Custom.EMPTY_TEMPLATE));
    }

    @Test
    public void textIsNotMarkup() {
        assertFalse(Custom.isHtmlTemplate(null));
        assertFalse(Custom.isHtmlTemplate(""));
        assertFalse(Custom.isHtmlTemplate("orderBoard"));
        assertFalse(Custom.isHtmlTemplate("a < b"));   // a comparison is not a tag
        assertFalse(Custom.isHtmlTemplate("2<3"));
        assertFalse(Custom.isHtmlTemplate("<"));
        assertFalse(Custom.isHtmlTemplate("<1"));
    }

    // the two vocabularies never overlap, which is what lets a value be classified by them alone
    @Test
    public void componentAndTemplateAreDisjoint() {
        for (String value : new String[] {"Board", "<div>", "", "board", "<Lsf:x>", Custom.EMPTY_TEMPLATE})
            assertFalse(value + " is both", Custom.isReactComponent(value) && Custom.isHtmlTemplate(value));
    }

    // ---------------------------------------------------------------- walking the places

    @Test
    public void templateWithoutPlacesIsUnchanged() throws Custom.PlaceError {
        assertEquals("", walk(""));
        assertEquals("<div>plain</div>", walk("<div>plain</div>"));
        assertEquals("Board", walk("Board"));
    }

    @Test
    public void placeNameIsMapped() throws Custom.PlaceError {
        assertEquals("<div><Lsf:[orders]></div>", walk("<div><Lsf:orders></div>"));
    }

    @Test
    public void severalPlacesAreMappedInOrder() throws Custom.PlaceError {
        assertEquals("<Lsf:[a]>|<Lsf:[b]>|<Lsf:[c]>", walk("<Lsf:a>|<Lsf:b>|<Lsf:c>"));
    }

    // the markup is HTML, where a tag's case means nothing, so the prefix is matched either way - and the NAME is not
    @Test
    public void prefixIsCaseInsensitiveAndTheNameIsNot() throws Custom.PlaceError {
        assertEquals("<lsf:[orders]>", walk("<lsf:orders>"));
        assertEquals("<LSF:[orders]>", walk("<LSF:orders>"));
        assertEquals("<LsF:[Orders]>", walk("<LsF:Orders>"));
    }

    // the name ends where the tag or an attribute begins
    @Test
    public void nameEndsAtTheTagOrAnAttribute() throws Custom.PlaceError {
        assertEquals("<Lsf:[a]>", walk("<Lsf:a>"));
        assertEquals("<Lsf:[a] >", walk("<Lsf:a >"));
        assertEquals("<Lsf:[a]\n>", walk("<Lsf:a\n>"));
        assertEquals("<Lsf:[a] class=\"x\">", walk("<Lsf:a class=\"x\">"));
        assertEquals("<Lsf:[PROPERTY(note).caption]>", walk("<Lsf:PROPERTY(note).caption>"));
    }

    // a place is written open, and only open
    @Test
    public void bothOtherSpellingsAreRefused() {
        placeError("<Lsf:a/>", "a place is written open, so 'a' does not close itself");
        placeError("<Lsf:a />", "a place is written open, so 'a' does not close itself");
        placeError("<Lsf:a\n  />", "a place is written open, so 'a' does not close itself");
        placeError("</Lsf:a>", "a place is written without a closing tag, so 'a' has none to close");
        placeError("<Lsf:a></Lsf:a>", "a place is written without a closing tag, so 'a' has none to close");
        placeError("</lsf:a>", "a place is written without a closing tag, so 'a' has none to close");
    }

    // a slash INSIDE an attribute is not the tag closing itself - the check looks only past whitespace
    @Test
    public void aSlashInAnAttributeIsNotSelfClosing() throws Custom.PlaceError {
        assertEquals("<Lsf:[a] class=\"x/y\">", walk("<Lsf:a class=\"x/y\">"));
        assertEquals("<Lsf:[a] data-href=\"/orders\">", walk("<Lsf:a data-href=\"/orders\">"));
    }

    @Test
    public void aPlaceWithoutANameIsRefused() {
        placeError("<Lsf:>", "a place has no name");
        placeError("<Lsf: >", "a place has no name");
        placeError("<div><Lsf:></div>", "a place has no name");
        placeError("<Lsf:", "a place has no name");      // the template ends right after the prefix
        placeError("<Lsf:/>", "a place has no name");    // the empty name is read before the spelling
    }

    // one view, one place: the client fills the first element of the tag it finds, so a second would stay empty
    @Test
    public void aNamePlacedTwiceIsRefused() {
        placeError("<Lsf:a><Lsf:a>", "'a' is placed more than once");
    }

    // the duplicate is judged on what the mapper RETURNS, so two spellings of one thing collide - which is how a
    // window's template catches `<Lsf:orders>` beside `<Lsf:Sale.orders>`
    @Test
    public void twoNamesMappingToOneThingCollide() {
        try {
            Custom.mapPlaces("<Lsf:orders><Lsf:Sale.orders>", name -> "Sale.orders");
            fail("expected a PlaceError");
        } catch (Custom.PlaceError e) {
            assertEquals("'Sale.orders' is placed more than once", e.getMessage());
        }
    }

    // ...and case-insensitively, because an HTML parser does not tell two spellings of a tag apart
    @Test
    public void collisionIsCaseInsensitive() {
        try {
            Custom.mapPlaces("<Lsf:a><Lsf:b>", name -> name.equals("a") ? "Sale.Orders" : "sale.orders");
            fail("expected a PlaceError");
        } catch (Custom.PlaceError e) {
            assertEquals("'b' is placed more than once", e.getMessage());
        }
    }

    // the caller's own error - an element that does not exist, a child the form does not have - comes back as it is
    @Test
    public void theMappersErrorIsRaisedAsItIs() {
        try {
            Custom.mapPlaces("<Lsf:nowhere>", name -> {
                throw new Custom.PlaceError("'" + name + "' is not a navigator element");
            });
            fail("expected a PlaceError");
        } catch (Custom.PlaceError e) {
            assertEquals("'nowhere' is not a navigator element", e.getMessage());
        }
    }

    // ---------------------------------------------------------------- what is deliberately NOT read

    // only the prefix makes a place, so nothing else in the markup is read, let alone rewritten
    @Test
    public void nothingButThePrefixMakesAPlace() throws Custom.PlaceError {
        assertEquals("<lsfoo>", walk("<lsfoo>"));
        assertEquals("<lsf-place>", walk("<lsf-place>"));
        assertEquals("< Lsf:a>", walk("< Lsf:a>"));
        assertEquals("&lt;Lsf:a&gt;", walk("&lt;Lsf:a&gt;"));
    }

    // the template is not parsed as HTML, so a place inside a comment is still a place. Stated here because it is
    // surprising, and because the client half behaves the same way - both ends read the string, not a document
    @Test
    public void aPlaceInsideACommentIsStillAPlace() throws Custom.PlaceError {
        assertEquals("<!-- <Lsf:[a]> -->", walk("<!-- <Lsf:a> -->"));
    }

    // a place the markup never closes is left as the author wrote it: the client's unwrap makes it work, and this
    // walker's job is the NAME, not the well-formedness of the markup around it
    @Test
    public void anUnclosedTagKeepsItsName() throws Custom.PlaceError {
        assertEquals("<div><Lsf:[a]", walk("<div><Lsf:a"));
    }

    // what the mapper puts back is NOT walked again, so a canonical name that happens to contain the prefix cannot
    // turn into a second place - the walk moves past what it wrote
    @Test
    public void whatTheMapperReturnsIsNotWalkedAgain() throws Custom.PlaceError {
        assertEquals("<Lsf:<Lsf:x>>", Custom.mapPlaces("<Lsf:a>", name -> "<Lsf:x>"));
    }

    // every whitespace an HTML author can write between the name and the tag
    @Test
    public void everyWhitespaceEndsTheName() throws Custom.PlaceError {
        assertEquals("<Lsf:[a]\t>", walk("<Lsf:a\t>"));
        assertEquals("<Lsf:[a]\r\n>", walk("<Lsf:a\r\n>"));
        assertEquals("<Lsf:[a]\f>", walk("<Lsf:a\f>"));
        placeError("<Lsf:a\t/>", "a place is written open, so 'a' does not close itself");
    }

    // nothing but '>', '/' and whitespace ends a name, so a name is taken as written - including characters no
    // element sID has. The mapper is what decides whether it names anything
    @Test
    public void anythingElseIsPartOfTheName() throws Custom.PlaceError {
        assertEquals("<Lsf:[a<b]>", walk("<Lsf:a<b>"));
        assertEquals("<Lsf:[a=\"b\"]>", walk("<Lsf:a=\"b\">"));
        assertEquals("<Lsf:[Ёлка]>", walk("<Lsf:Ёлка>"));
    }

    // the walk is linear and the template can be as long as an application's markup
    @Test
    public void manyPlacesAreAllMapped() throws Custom.PlaceError {
        StringBuilder template = new StringBuilder();
        for (int i = 0; i < 200; i++)
            template.append("<div><Lsf:c").append(i).append("></div>");

        String walked = walk(template.toString());
        assertEquals(200, walked.split("\\[c", -1).length - 1);
        assertTrue(walked.contains("<Lsf:[c0]>"));
        assertTrue(walked.contains("<Lsf:[c199]>"));
    }

    // the closing-tag check runs over the whole template, not only where a place is expected - so a stray one after
    // every place is still refused, and its name is read for the message
    @Test
    public void aClosingTagIsFoundWhereverItIs() {
        placeError("<div><Lsf:a></div></Lsf:b>", "a place is written without a closing tag, so 'b' has none to close");
        placeError("</Lsf:>", "a place is written without a closing tag, so '' has none to close");
    }
}
