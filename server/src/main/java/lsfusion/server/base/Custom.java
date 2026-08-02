package lsfusion.server.base;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

// what a `custom` value is, and what the <Lsf:name> places in it name. The single point of determination, shared by
// everything a `custom` can draw - a DESIGN container, a navigator window - which do not accept the same vocabulary:
// a container takes '' (lay the children out in order), a component name, or markup; a window takes a component name
// or markup only, since it has no children of its own to lay out
public class Custom {

    private static final Pattern REACT_COMPONENT_NAME = Pattern.compile("[A-Z][A-Za-z0-9_$]*");

    // what is stored when only a property gives the value: the view that draws it is chosen before the first value
    // arrives, so there has to be SOMETHING there, and an empty template draws nothing until the value replaces it.
    // A container reads it as "not simple" and a window as "not the standard toolbar"
    public static final String EMPTY_TEMPLATE = "<div/>";

    public static boolean isReactComponent(String custom) {
        return custom != null && REACT_COMPONENT_NAME.matcher(custom).matches();
    }

    private static final Pattern HTML_TEMPLATE = Pattern.compile(".*<[A-Za-z/!].*", Pattern.DOTALL);

    // the client half of this prefix is GwtClientUtils.getLsfPlace / unwrapLsfPlaces - one protocol, two ends
    private static final String PLACE = "<Lsf:";
    private static final String CLOSING_PLACE = "</Lsf:";

    // walks the <Lsf:name> places of a template, handing each name to the caller and putting back what it returns.
    // A place is recognized by its prefix alone, so nothing else in the markup is read, let alone rewritten. The
    // markup is HTML, where the case of a tag means nothing, so the prefix is matched either way - but on the string
    // itself: lowercasing a copy and indexing the original with its offsets breaks on any character whose lower case
    // is longer than it is.
    //
    // A place is written open, and only open. An HTML parser leaves it that way and unwrapLsfPlaces hands back what it
    // swallowed, so a closing tag would be a second way to write the same thing - and one that can be closed with the
    // wrong name. Both other ways of writing it are rejected here, where the template is read.
    public static String mapPlaces(String custom, PlaceMapper mapper) throws PlaceError {
        StringBuilder result = new StringBuilder();
        Set<String> placed = new HashSet<>();
        int pos = 0;
        while (true) {
            int at = -1;
            for (int i = pos; i < custom.length(); i++) {
                if (custom.regionMatches(true, i, CLOSING_PLACE, 0, CLOSING_PLACE.length()))
                    throw new PlaceError("a place is written without a closing tag, so '" + readName(custom, i + CLOSING_PLACE.length()) + "' has none to close");
                if (custom.regionMatches(true, i, PLACE, 0, PLACE.length())) {
                    at = i;
                    break;
                }
            }
            if (at < 0)
                break;

            int nameAt = at + PLACE.length();
            String name = readName(custom, nameAt);
            if (name.isEmpty())
                throw new PlaceError("a place has no name");

            int end = nameAt + name.length();
            if (isSelfClosing(custom, end))
                throw new PlaceError("a place is written open, so '" + name + "' does not close itself");

            String mapped = mapper.map(name);
            // one view, one place - and the second would be left empty in silence, since the client fills the first
            // element of the tag it finds. Case-insensitively, because an HTML parser does not tell the two apart
            if (!placed.add(mapped.toLowerCase()))
                throw new PlaceError("'" + name + "' is placed more than once");

            result.append(custom, pos, nameAt).append(mapped);
            pos = end;
        }
        return result.append(custom.substring(pos)).toString();
    }

    private static final String WHITESPACE = " \t\r\n\f";
    private static final String TERMINATORS = ">/" + WHITESPACE;

    private static String readName(String custom, int from) {
        int end = from;
        while (end < custom.length() && TERMINATORS.indexOf(custom.charAt(end)) < 0)
            end++;
        return custom.substring(from, end);
    }

    // the slash has to follow the name, with only whitespace between: looking further would read a slash inside an
    // attribute value as one
    private static boolean isSelfClosing(String custom, int afterName) {
        for (int i = afterName; i < custom.length(); i++) {
            char c = custom.charAt(i);
            if (c == '/')
                return true;
            if (WHITESPACE.indexOf(c) < 0)
                return false;
        }
        return false;
    }

    // the names a template's places ask for, as it wrote them - which for a literal that has been read is already the
    // canonical name, since mapPlaces rewrote each one when the module was read
    public static Set<String> places(String custom) {
        Set<String> names = new HashSet<>();
        try {
            mapPlaces(custom, name -> {
                names.add(name);
                return name;
            });
        } catch (PlaceError ignored) { // a literal that got here has been walked once already, and a computed one is
        }                             // not walked at all - either way there is nothing to say a second time
        return names;
    }

    public interface PlaceMapper {
        String map(String name) throws PlaceError;
    }

    // so the caller can raise its own error - an unresolved navigator element, a container child that does not exist
    public static class PlaceError extends Exception {
        public PlaceError(String message) {
            super(message);
        }
    }

    // an HTML template is recognized by having markup in it, so that a misspelled component name (a bare identifier
    // that is not UpperCamel) stays an error instead of quietly becoming a template that draws its own text
    public static boolean isHtmlTemplate(String custom) {
        return custom != null && HTML_TEMPLATE.matcher(custom).matches();
    }

    // what a written value may weigh. A window and a container hand their custom to the client as one modified-UTF
    // string, which cannot carry more than this - and a longer one is not refused there but THROWN, on every client
    // that connects, long after the module was read: the application starts and nobody can log in. So it is refused
    // where it is written, by name. A template that really needs to be bigger than this is computed by a property,
    // which is pushed as an ordinary property value and has no such limit
    public static final int MAX_LENGTH = 65535;

    public static boolean isTooLong(String custom) {
        return custom != null && utfLength(custom) > MAX_LENGTH;
    }

    // the length writeUTF counts: one byte below 0x80, two below 0x800, three above - and two for a NUL, which is
    // why it is not simply the UTF-8 length
    private static int utfLength(String custom) {
        int length = 0;
        for (int i = 0; i < custom.length(); i++) {
            char c = custom.charAt(i);
            length += c >= 0x0001 && c <= 0x007F ? 1 : c > 0x07FF ? 3 : 2;
        }
        return length;
    }
}
