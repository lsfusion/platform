package lsfusion.http.controller;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

// A view writes a value so that the browser takes it as the value whatever it holds: a ${...} goes into the markup
// through fn:escapeXml, in quotes in an attribute, and into a script only as a literal the controller makes, standing
// as an expression of its own; a <%= %> writes a text of the message bundle. Not checked are the attributes of the JSP
// actions that read a value rather than write it, and the client's own page, drawn after a login, which writes the
// application's settings into its markup and its scripts
public class ViewValuesTest {

    private static final String CLIENT_PAGE = "main.jsp";
    private static final Pattern READING_ACTION = Pattern.compile("<(c:if|c:forEach|c:remove|lsf:writeResources)\\s");

    // the links to the desktop client: markup the controller makes, with the address in it escaped
    private static final List<String> MARKUP = Arrays.asList("jnlpUrls");
    // a notification's id, a number, and string literals
    private static final List<String> SCRIPT_LITERALS = Arrays.asList("id", "query", "redirectURL");

    private static final Pattern EL = Pattern.compile("\\$\\{(.*?)}", Pattern.DOTALL);
    private static final Pattern ESCAPED = Pattern.compile("fn:escapeXml\\([^()?:]*\\)");
    private static final Pattern EXPRESSION = Pattern.compile("<%=(.*?)%>", Pattern.DOTALL);
    private static final Pattern MESSAGE = Pattern.compile("ServerMessages\\.getString\\(request, \"[^\"]*\"\\)");

    @Test
    public void viewsWriteValuesAsText() throws Exception {
        File[] views = new File("src/main/webapp/WEB-INF/view").listFiles((dir, name) -> name.endsWith(".jsp") && !name.equals(CLIENT_PAGE));
        assertTrue(views != null && views.length > 0);

        List<String> unescaped = new ArrayList<>();
        for (File view : views) {
            String page = new String(Files.readAllBytes(view.toPath()), StandardCharsets.UTF_8);

            Matcher el = EL.matcher(page);
            while (el.find()) {
                String value = el.group(1).trim();
                if (!inReadingAction(page, el.start()) && !(inScript(page, el.start())
                        ? SCRIPT_LITERALS.contains(value) && !inString(page, el.start()) && wholeExpression(page, el.start(), el.end())
                        : (ESCAPED.matcher(value).matches() || MARKUP.contains(value)) && !inUnquotedAttribute(page, el.start())))
                    unescaped.add(view.getName() + ": " + el.group());
            }

            Matcher expression = EXPRESSION.matcher(page);
            while (expression.find())
                if (!MESSAGE.matcher(expression.group(1).trim()).matches())
                    unescaped.add(view.getName() + ": " + expression.group());
        }
        assertEquals(new ArrayList<>(), unescaped);
    }

    private static boolean inReadingAction(String page, int position) {
        int tag = tagStart(page, position);
        return tag >= 0 && READING_ACTION.matcher(page).region(tag, page.length()).lookingAt() && tagEnd(page, tag) > position;
    }

    // in the body of a script element, not in the attributes of its tag
    private static boolean inScript(String page, int position) {
        String before = page.substring(0, position).toLowerCase();
        int script = before.lastIndexOf("<script");
        return script > before.lastIndexOf("</script") && tagEnd(page, script) < position;
    }

    // in a string literal of the script, which the line opens before the position
    private static boolean inString(String page, int position) {
        char quote = 0;
        for (int i = page.lastIndexOf('\n', position) + 1; i < position; i++) {
            char c = page.charAt(i);
            if (quote != 0) {
                if (c == '\\')
                    i++;
                else if (c == quote)
                    quote = 0;
            } else if (c == '"' || c == '\'' || c == '`')
                quote = c;
        }
        return quote != 0;
    }

    // a literal of the controller stands where an expression of the script does
    private static boolean wholeExpression(String page, int start, int end) {
        String before = page.substring(0, start).trim(), after = page.substring(end).trim();
        return !before.isEmpty() && ":=(,[".indexOf(before.charAt(before.length() - 1)) >= 0 && !after.isEmpty() && ",;)]}".indexOf(after.charAt(0)) >= 0;
    }

    // in the value of an attribute with no quotes around it, which an escaped value can still end
    private static boolean inUnquotedAttribute(String page, int position) {
        int tag = tagStart(page, position);
        if (tag < 0 || tagEnd(page, tag) < position)
            return false;
        char quote = 0;
        for (int i = tag; i < position; i++) {
            char c = page.charAt(i);
            if (quote != 0) {
                if (c == quote)
                    quote = 0;
            } else if (c == '"' || c == '\'')
                quote = c;
        }
        return quote == 0;
    }

    // the "<" that starts the tag the position is in or after: one followed by a name or a "/"
    private static int tagStart(String page, int position) {
        for (int i = position - 1; i >= 0; i--)
            if (page.charAt(i) == '<' && (Character.isLetter(page.charAt(i + 1)) || page.charAt(i + 1) == '/'))
                return i;
        return -1;
    }

    // the position of the ">" that ends the tag, past the quoted values of its attributes
    private static int tagEnd(String page, int tag) {
        char quote = 0;
        for (int i = tag; i < page.length(); i++) {
            char c = page.charAt(i);
            if (quote != 0) {
                if (c == quote)
                    quote = 0;
            } else if (c == '"' || c == '\'')
                quote = c;
            else if (c == '>')
                return i;
        }
        return page.length();
    }
}
