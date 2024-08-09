package lsfusion.server.language;

import lsfusion.base.Pair;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Function;

import static lsfusion.base.BaseUtils.*;
import static lsfusion.server.physics.dev.i18n.LocalizedString.CLOSE_CH;
import static lsfusion.server.physics.dev.i18n.LocalizedString.OPEN_CH;

public class ScriptedStringUtils {
    public static final char QUOTE = '\'';
    public static final String specialCharacters = "nrt";
    public static final char INTERP_CH = '$';
    public static final char INLINE_CH = 'I';
    public static final char RESOURCE_CH = 'R';
    public static final char IMAGE_CH = 'M';

    public static final String INLINE_PREFIX = String.valueOf(INTERP_CH) + INLINE_CH + OPEN_CH; // $I{

    private static final String SLASH_ERR_TEXT = "wrong escape sequence at the end of the string";

    public static class TransformationError extends Exception {
        public TransformationError(String text) {
            super(text);
        }
    }
    
    private static final String wrongEscapeSeqFormat = "wrong escape sequence: '\\%s'";
    
    public static String transformStringLiteral(String literal, Function<String, String> getIdFromReversedI18NDictionary) throws TransformationError {
        if (literal == null) {
            return null;
        }
        if (getIdFromReversedI18NDictionary != null) {
            literal = removeMetacodeReverseI18NSpecialStrings(literal, false, getIdFromReversedI18NDictionary).first;
        }
        StringBuilder b = new StringBuilder();
        for (int i = 1; i+1 < literal.length(); i++) {
            if (literal.charAt(i) == '\\') {
                if (i+2 == literal.length()) {
                    throw new TransformationError(SLASH_ERR_TEXT);
                }
                
                char nextCh = literal.charAt(i+1);
                switch (nextCh) {
                    case '\\': b.append('\\'); break;
                    case QUOTE: b.append(QUOTE); break;
                    case INTERP_CH: b.append(INTERP_CH); break;
                    case 'n': b.append('\n'); break;
                    case 'r': b.append('\r'); break;
                    case 't': b.append('\t'); break;
                    case OPEN_CH: case CLOSE_CH: {
                        throw new TransformationError(String.format(wrongEscapeSeqFormat, nextCh) + ", literal has no localization");    
                    }
                    default: {
                        throw new TransformationError(String.format(wrongEscapeSeqFormat, nextCh));
                    }
                }
                ++i;
            } else {
                b.append(literal.charAt(i));
            }
        }
        return b.toString();
    }

    private static Pair<String, Boolean> removeMetacodeReverseI18NSpecialStrings(String literal, boolean isLocalizedLiteral, Function<String, String> getIdFromReversedI18NDictionary) {
        StringBuilder newLiteral = new StringBuilder();
        int len = literal.length();
        boolean hasSpecialSequence = false;
        for (int i = 0; i < len; ++i) {
            char ch = literal.charAt(i);
            Pair<Integer, Integer> indices = specialMetacodeReverseI18NSequencePosition(literal, i);
            if (indices != null) {
                int nextCloseIndex = indices.first;
                int lastCloseIndex = indices.second;        
                hasSpecialSequence = true;
                String id = literal.substring(i+2, nextCloseIndex);
                String rawLiteral = literal.substring(nextCloseIndex+1, lastCloseIndex);
                boolean isCorrectId = isCorrectId(id, rawLiteral, getIdFromReversedI18NDictionary);
                // The latter condition is needed to correctly transform special sequence to result literal when we are generating a bundle file.  
                if (isCorrectId || System.getProperty("generateBundleFile") != null && id.trim().equals("{id}")) { // todo [dale]: improve this part
                    if (isLocalizedLiteral && isCorrectId) {
                        newLiteral.append(id);
                    } else {
                        newLiteral.append(rawLiteral);
                    }
                    i = lastCloseIndex;
                } else {
                    newLiteral.append(ch);
                }
            } else {
                newLiteral.append(ch);
                if (ch == '\\' && i+1 < len) {
                    ++i;
                    newLiteral.append(literal.charAt(i));
                }
            }
        }
        return new Pair<>(newLiteral.toString(), hasSpecialSequence); 
    }
    
    private static boolean isCorrectId(String id, String rawLiteral, Function<String, String> getIdFromReversedI18NDictionary) {
        try {
            if (getIdFromReversedI18NDictionary != null) {
                String propertyFileValue = ScriptedStringUtils.transformAnyStringLiteralToPropertyFileValue(quote(rawLiteral));
                String translateId = getIdFromReversedI18NDictionary.apply(propertyFileValue.trim());
                if (translateId != null) {
                    translateId = OPEN_CH + translateId + CLOSE_CH;
                    return translateId.equals(id.trim());
                }
            }
        } catch (TransformationError ignored) {
        }
        return false;
    }
    
    private static Pair<Integer, Integer> specialMetacodeReverseI18NSequencePosition(String literal, int index) {
        int len = literal.length();
        if (literal.charAt(index) == OPEN_CH && index + 1 < len && literal.charAt(index+1) == OPEN_CH) {
            int idOpenIndex = indexOfUnquoted(literal, OPEN_CH, index+2);
            if (idOpenIndex == -1) return null;
            int idCloseIndex = indexOfUnquoted(literal, CLOSE_CH, idOpenIndex+1);
            if (idCloseIndex == -1) return null;
            int nextCloseIndex = indexOfUnquoted(literal, CLOSE_CH, idCloseIndex+1);
            if (nextCloseIndex == -1) return null;
            int lastCloseIndex = indexOfUnquoted(literal, CLOSE_CH, nextCloseIndex+1);
            if (lastCloseIndex == -1) return null;
            return new Pair<>(nextCloseIndex, lastCloseIndex);
        }
        return null;
    } 
    
    private static int indexOfUnquoted(String s, char target, int startIndex) {
        int len = s.length();
        for (int i = startIndex; i < len; ++i) {
            if (s.charAt(i) == '\\') {
                ++i;
            } else if (s.charAt(i) == target) {
                return i;
            }
        }
        return -1;
    }
    
    public static LocalizedString transformLocalizedStringLiteral(String literal, Function<String, String> getIdFromReversedI18NDictionary, Consumer<String> appendEntry) throws TransformationError, LocalizedString.FormatError {
        if (literal == null) {
            return null;
        }
        
        boolean hadSpecialSequence = false;
        if (getIdFromReversedI18NDictionary != null) {
            Pair<String, Boolean> result = removeMetacodeReverseI18NSpecialStrings(literal, true, getIdFromReversedI18NDictionary);
            literal = result.first;
            hadSpecialSequence = result.second;
        }
        
        String sourceString = transformLocalizedStringLiteralToSourceString(literal);
        
        LocalizedString.checkLocalizedStringFormat(sourceString);
        
        if (getIdFromReversedI18NDictionary != null && hasNoLocalizationFeatures(sourceString, false)) {
            String propertyFileValue = transformAnyStringLiteralToPropertyFileValue(literal);
            // hadSpecialSequence condition is needed to prevent second addition of metacode literal to the bundle file
            if (!hadSpecialSequence && System.getProperty("generateBundleFile") != null) {
                addToResourceBundle(propertyFileValue, appendEntry);
            }
            
            Pair<Integer, Integer> spaces = getSpaces(propertyFileValue);
            if (spaces.first + spaces.second < propertyFileValue.length()) {
                String translateId = getIdFromReversedI18NDictionary.apply(propertyFileValue.trim());
                if (translateId != null) {
                    sourceString = setSpaces(OPEN_CH + translateId + CLOSE_CH, spaces);
                    return LocalizedString.create(sourceString, true);  
                }
            }
        }
        
        return LocalizedString.create(sourceString);
    }

    public static String transformLocalizedStringLiteralToSourceString(String s) throws TransformationError {
        return transformStringLiteral(s, true, String.valueOf(QUOTE) + INTERP_CH, "\\" + OPEN_CH + CLOSE_CH);
    }

    public static String removeEscaping(String s) throws TransformationError {
        return transformString(s, true, "\\" + QUOTE + INTERP_CH + OPEN_CH + CLOSE_CH, "");
    }

    private static String removeQuoteEscaping(String s) throws TransformationError {
        return transformString(s, false, String.valueOf(QUOTE), null);
    }

    private static String transformString(String s, boolean replaceSpecial, String removeEscaping, String keepEscaping) throws TransformationError {
        if (s == null) return null;
        return transformStringPart(s, 0, s.length(), replaceSpecial, removeEscaping, keepEscaping);
    }

    private static String transformStringLiteral(String s, boolean replaceSpecial, String removeEscaping, String keepEscaping) throws TransformationError {
        if (s == null) return null;
        return transformStringPart(s, 1, s.length() - 1, replaceSpecial, removeEscaping, keepEscaping);
    }

    private static String transformStringPart(String s, int start, int end, boolean replaceSpecial, String removeEscaping, String keepEscaping) throws TransformationError {
        StringBuilder b = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (s.charAt(i) == '\\') {
                if (i+1 == end) {
                    throw new TransformationError(SLASH_ERR_TEXT);
                }

                char nextCh = s.charAt(i+1);
                if (replaceSpecial && specialCharacters.indexOf(nextCh) != -1) {
                    b.append(toSpecialCharacter(nextCh));
                } else if (removeEscaping.indexOf(nextCh) != -1) {
                    b.append(nextCh);
                } else if (keepEscaping == null || keepEscaping.indexOf(nextCh) != -1) {
                    b.append('\\');
                    b.append(nextCh);
                } else {
                    throw new TransformationError(String.format(wrongEscapeSeqFormat, nextCh));
                }
                ++i;
            } else {
                b.append(s.charAt(i));
            }
        }
        return b.toString();
    }

    public static String transformAnyStringLiteralToPropertyFileValue(final String literal) throws TransformationError {
        return transformStringLiteral(literal, false, String.valueOf(QUOTE) + OPEN_CH + CLOSE_CH, null);
    }
    
    public static Pair<Integer, Integer> getSpaces(String s) {
        int leadingSpaces = 0;
        while (leadingSpaces < s.length() && s.charAt(leadingSpaces) == ' ') {
            ++leadingSpaces;
        }

        int trailingSpaces = 0;
        while (trailingSpaces < s.length() && s.charAt(s.length() - trailingSpaces - 1) == ' ') {
            ++trailingSpaces;
        }
        return new Pair<>(leadingSpaces, trailingSpaces);
    }
    
    public static String setSpaces(String s, Pair<Integer, Integer> spaces) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < spaces.first; ++i) {
            builder.append(' ');
        }
        builder.append(s);
        for (int i = 0; i < spaces.second; ++i) {
            builder.append(' ');
        }
        return builder.toString();
    }
    
    public static void addToResourceBundle(String s, Consumer<String> appendEntry) {
        if (hasCyrillicChars(s) && appendEntry != null) {
            appendEntry.accept(s.trim());
        }
    }
    
    private static boolean hasCyrillicChars(String s) {
        for (int i = 0; i < s.length(); ++i) {
            if (Character.UnicodeBlock.of(s.charAt(i)).equals(Character.UnicodeBlock.CYRILLIC)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean hasNoLocalizationFeatures(String s, Boolean isLiteral) {
        if (!isLiteral) {
            return LocalizedString.canBeOptimized(s);
        }
        // It turned out that this code is the same as LocalizedString.canBeOptimized, but when isLiteral equals true
        // the input string is a raw literal with different format and escape sequences   
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            if (ch == OPEN_CH || ch == CLOSE_CH) {
                return false;
            } else if (ch == '\\') {
                ++i;
            }
        }
        return true;
    }

    private enum StringInterpolateState { PLAIN, INTERPOLATION, INLINE, RESOURCE, IMAGE }

    public static List<String> parseStringInterpolateProp(String source) throws TransformationError {
        List<String> literals = new ArrayList<>();
        int pos = 0;
        int nestingDepth = 0;
        int nestingInterpolationDepth = 0;
        String currentLiteral = "";
        StringInterpolateState state = StringInterpolateState.PLAIN;
        StringInterpolateState newState;
        Stack<Boolean> interpolationMarks = new Stack<>();
        while (pos < source.length()) {
            char c = source.charAt(pos);
            newState = prefixState(source, pos);
            if (c == '\\') {
                if (pos+1 == source.length()) {
                    throw new TransformationError(SLASH_ERR_TEXT);
                }
                char nextc = source.charAt(pos + 1);
                // removes quote's escaping when at top level inside interpolation
                if (nextc != QUOTE || nestingInterpolationDepth != 1) {
                    currentLiteral += '\\';
                }
                currentLiteral += nextc;
                ++pos;
            } else if (newState != StringInterpolateState.PLAIN) {
                boolean isInterpolationState = newState == StringInterpolateState.INTERPOLATION;
                if (nestingDepth == 0) {
                    currentLiteral = flushCurrentLiteral(literals, currentLiteral);
                    state = newState;
                } else {
                    currentLiteral += source.substring(pos, pos + (isInterpolationState ? 2 : 3));
                }
                if (isInterpolationState) {
                    ++nestingInterpolationDepth;
                }
                ++nestingDepth;
                interpolationMarks.push(isInterpolationState);
                pos += (isInterpolationState ? 1 : 2);
            } else if (c == OPEN_CH) {
                ++nestingDepth;
                currentLiteral += c;
                interpolationMarks.push(false);
            } else if (c == CLOSE_CH) {
                --nestingDepth;
                if (interpolationMarks.peek()) {
                    --nestingInterpolationDepth;
                }
                interpolationMarks.pop();
                if (nestingDepth == 0 && state != StringInterpolateState.PLAIN) {
                    addToLiterals(currentLiteral, state, literals);
                    currentLiteral = "";
                    state = StringInterpolateState.PLAIN;
                } else {
                    currentLiteral += c;
                }
            } else {
                currentLiteral += c;
            }
            ++pos;
        }

        flushCurrentLiteral(literals, currentLiteral);
        return literals;
    }

    private static void addToLiterals(String currentLiteral, StringInterpolateState state, List<String> literals) {
        if (state == StringInterpolateState.INTERPOLATION) {
            literals.add("STRING(" + currentLiteral + ")");
        } else if (state == StringInterpolateState.INLINE) {
            literals.add(quote(INLINE_PREFIX + currentLiteral + CLOSE_CH));
        } else if (state == StringInterpolateState.RESOURCE) {
            literals.add(quote(wrapResource(currentLiteral)));
        } else if (state == StringInterpolateState.IMAGE) {
            literals.add(quote(wrapImage(currentLiteral)));
        } else
            assert false;
    }

    public static String wrapResource(String currentLiteral) {
        return inlineFileSeparator + currentLiteral + inlineFileSeparator;
    }

    public static String wrapImage(String currentLiteral) {
        return inlineFileSeparator + inlineImageSeparator + currentLiteral + inlineFileSeparator;
    }

    public static String wrapSerializedImage(String currentLiteral) {
        return inlineFileSeparator + inlineSerializedImageSeparator + currentLiteral + inlineFileSeparator;
    }

    private static String flushCurrentLiteral(List<String> literals, String currentLiteral) {
        if (!currentLiteral.isEmpty()) {
            literals.add(quote(currentLiteral));
        }
        return "";
    }

    // heuristic procedure
    // allows ${, $I{, $R{, $M{ special syntax, but not localization or escaping
    // adds escaping to all our special characters except for ${, $I{, $R{, $M{
    // and corresponding closing braces.
    // This can lead to an error if content contains, for example, javascript code
    // with ${ inside
    public static String escapeInlineContent(String content) {
        StringBuilder builder = new StringBuilder();
        int nestingDepth = 0;
        boolean insideSpecialState = false;
        for (int i = 0; i < content.length(); ++i) {
            StringInterpolateState state = prefixState(content, i);
            if (!insideSpecialState && state != StringInterpolateState.PLAIN) {
                insideSpecialState = true;
                nestingDepth = 1;
                int end = i + (state == StringInterpolateState.INTERPOLATION ? 1 : 2);
                builder.append(content, i, end+1);
                i = end;
            } else {
                char ch = content.charAt(i);
                if (ch == OPEN_CH) {
                    if (insideSpecialState) {
                        ++nestingDepth;
                    } else {
                        builder.append('\\');
                    }
                } else if (ch == CLOSE_CH) {
                    if (insideSpecialState) {
                        --nestingDepth;
                        if (nestingDepth == 0) {
                            insideSpecialState = false;
                        }
                    } else {
                        builder.append('\\');
                    }
                } else if (("\\" + QUOTE + INTERP_CH).indexOf(ch) != -1) {
                    if (!insideSpecialState) {
                        builder.append('\\');
                    }
                } else if ("\n\r\t".indexOf(ch) != -1) {
                    builder.append('\\');
                    ch = fromSpecialChar(ch);
                }
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    public static boolean isInlineSequence(String s) {
        if (!s.startsWith(INLINE_PREFIX)) return false;
        int depth = 0;
        for (int i = 0; i < s.length(); ++i) {
            char ch = s.charAt(i);
            if (ch == '\\') {
                if (i+1 == s.length()) {
                    return false;
                } else {
                    ++i;
                }
            } else if (ch == OPEN_CH) {
                ++depth;
            } else if (ch == CLOSE_CH) {
                --depth;
                if (depth < 0 || depth == 0 && i+1 < s.length()) return false;
            }
        }
        return depth == 0;
    }

    public static boolean containsSpecialSequence(String s) {
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == '\\') {
                ++i;
            } else if (prefixState(s, i) != StringInterpolateState.PLAIN) {
                return true;
            }
        }
        return false;
    }

    private static StringInterpolateState prefixState(String s, int pos) {
        if (!compareChar(s, pos, INTERP_CH)) return StringInterpolateState.PLAIN;
        if (compareChar(s, pos + 1, OPEN_CH)) return StringInterpolateState.INTERPOLATION;
        if (compareChar(s, pos + 1, INLINE_CH) && compareChar(s, pos + 2, OPEN_CH)) return StringInterpolateState.INLINE;
        if (compareChar(s, pos + 1, RESOURCE_CH) && compareChar(s, pos + 2, OPEN_CH)) return StringInterpolateState.RESOURCE;
        if (compareChar(s, pos + 1, IMAGE_CH) && compareChar(s, pos + 2, OPEN_CH)) return StringInterpolateState.IMAGE;
        return StringInterpolateState.PLAIN;
    }

    private static boolean compareChar(String source, int pos, char cmp) {
        return pos < source.length() && source.charAt(pos) == cmp;
    }

    private static char toSpecialCharacter(char ch) {
        switch (ch) {
            case 'n': return '\n';
            case 'r': return '\r';
            case 't': return '\t';
        }
        return ch;
    }

    private static char fromSpecialChar(char ch) {
        switch (ch) {
            case '\n': return 'n';
            case '\r': return 'r';
            case '\t': return 't';
        }
        return ch;
    }

    public static String unquote(String s) {
        if (s.length() >= 2 && s.charAt(0) == QUOTE && s.charAt(s.length()-1) == QUOTE) {
            s = s.substring(1, s.length()-1);
        }
        return s;
    }

    public static String quote(String s) {
        return QUOTE + s + QUOTE;
    }

    public static String capitalizeIfNeeded(String s, boolean toCapitalize) {
        if (toCapitalize && s.length() > 0) {
            s = StringUtils.capitalize(s);
        }
        return s;
    }
    
    public static boolean isRawStringSpecialChar(int ch) {
        String rawStringForbiddenSpecialChars = " \n\t'+*,=<>[](){}_#";
        return !(ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch >= '0' && ch <= '9') &&
                rawStringForbiddenSpecialChars.indexOf(ch) == -1;
    }
}
