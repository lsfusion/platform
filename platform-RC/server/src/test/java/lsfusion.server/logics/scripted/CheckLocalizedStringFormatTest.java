package lsfusion.server.logics.scripted;

import lsfusion.server.logics.i18n.LocalizedString;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CheckLocalizedStringFormatTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void checkSimpleString() throws LocalizedString.FormatError {
        String source = "simple test string";
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkCorrectI18nString() throws LocalizedString.FormatError {
        String source = "{string1}simple {inner.value} test string {string2}";
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkStringWithBraces() throws LocalizedString.FormatError {
        String source = "{main.firstID} - \\{Not an ID\\} {main.secondID}";
        LocalizedString.checkLocalizedStringFormat(source);
    }
    
    @Test
    public void checkCorrectI18nString2() throws LocalizedString.FormatError {
        String source = "simple {string1}{inner.value} test{string2} string";
        LocalizedString.checkLocalizedStringFormat(source);
    }
    
    @Test
    public void checkCorrectI18nString3() throws LocalizedString.FormatError {
        String source = "simple {string1\\{\\}}{inner.value} test{str\\\\ing2} string";
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkCorrectI18nString4() throws LocalizedString.FormatError {
        String source = "\\\\simple {string1\\\\=}{inner.value} test{string2} string\\\\";
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkWhiteSpacesInsideKeyError() throws LocalizedString.FormatError {
        String source = "{complex key}";
        thrown.expect(LocalizedString.FormatError.class);
        thrown.expectMessage("whitespace");
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkIncompleteKeyError() throws LocalizedString.FormatError {
        String source = "{complexkey";
        thrown.expect(LocalizedString.FormatError.class);
        thrown.expectMessage("not closed");
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkIncompleteKeyError2() throws LocalizedString.FormatError {
        String source = "{key} text {secondkey";
        thrown.expect(LocalizedString.FormatError.class);
        thrown.expectMessage("not closed");
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkNotEscapedBracesError() throws LocalizedString.FormatError {
        String source = "{key{}}";
        thrown.expect(LocalizedString.FormatError.class);
        thrown.expectMessage("should be escaped with");
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkNotEscapedBracesError2() throws LocalizedString.FormatError {
        String source = "{key}}";
        thrown.expect(LocalizedString.FormatError.class);
        thrown.expectMessage("should be escaped with");
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkEmptyKeyError() throws LocalizedString.FormatError {
        String source = "{}";
        thrown.expect(LocalizedString.FormatError.class);
        thrown.expectMessage("empty key");
        LocalizedString.checkLocalizedStringFormat(source);
    }
    
    @Test
    public void checkNullAssertion() throws LocalizedString.FormatError {
        thrown.expect(AssertionError.class);
        LocalizedString.checkLocalizedStringFormat(null);
    } 
   
    @Test
    public void checkLastBackslashError() throws LocalizedString.FormatError {
        String source = "{key} text\\";
        thrown.expect(LocalizedString.FormatError.class);
        thrown.expectMessage("the end");
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkWrongEscapeSequenceError() throws LocalizedString.FormatError {
        String source = "\\z";
        thrown.expect(LocalizedString.FormatError.class);
        thrown.expectMessage("wrong escape");
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkWrongEscapeSequenceError2() throws LocalizedString.FormatError {
        String source = "\\n\\r";
        thrown.expect(LocalizedString.FormatError.class);
        thrown.expectMessage("wrong escape");
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkCorrectI18nString5() throws LocalizedString.FormatError {
        String source = "\n\r\t\\{\\}\\\\";
        LocalizedString.checkLocalizedStringFormat(source);
    }
}
