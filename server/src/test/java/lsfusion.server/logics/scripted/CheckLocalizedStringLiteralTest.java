package lsfusion.server.logics.scripted;

import lsfusion.server.logics.i18n.LocalizedString;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CheckLocalizedStringLiteralTest {
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
    public void checkSampleString() throws LocalizedString.FormatError {
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
   
}
