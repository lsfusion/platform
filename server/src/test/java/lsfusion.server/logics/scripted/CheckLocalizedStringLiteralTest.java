package lsfusion.server.logics.scripted;

import lsfusion.server.logics.i18n.LocalizedString;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CheckLocalizedStringLiteralTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void checkSimpleString() throws LocalizedString.I18NError {
        String source = "simple test string";
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkCorrectI18nString() throws LocalizedString.I18NError {
        String source = "{string1}simple {inner.value} test string {string2}";
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkSampleString() throws LocalizedString.I18NError {
        String source = "{main.firstID} - \\{Not an ID\\} {main.secondID}";
        LocalizedString.checkLocalizedStringFormat(source);
    }
    
    @Test
    public void checkCorrectI18nString2() throws LocalizedString.I18NError {
        String source = "simple {string1}{inner.value} test{string2} string";
        LocalizedString.checkLocalizedStringFormat(source);
    }
    
    @Test
    public void checkCorrectI18nString3() throws LocalizedString.I18NError {
        String source = "simple {string1\\{\\}}{inner.value} test{str\\\\ing2} string";
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkCorrectI18nString4() throws LocalizedString.I18NError {
        String source = "\\\\simple {string1\\\\=}{inner.value} test{string2} string\\\\";
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkWhiteSpacesInsideKeyError() throws LocalizedString.I18NError {
        String source = "{complex key}";
        thrown.expect(LocalizedString.I18NError.class);
        thrown.expectMessage("whitespace");
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkIncompleteKeyError() throws LocalizedString.I18NError {
        String source = "{complexkey";
        thrown.expect(LocalizedString.I18NError.class);
        thrown.expectMessage("not closed");
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkIncompleteKeyError2() throws LocalizedString.I18NError {
        String source = "{key} text {secondkey";
        thrown.expect(LocalizedString.I18NError.class);
        thrown.expectMessage("not closed");
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkNotEscapedBracesError() throws LocalizedString.I18NError {
        String source = "{key{}}";
        thrown.expect(LocalizedString.I18NError.class);
        thrown.expectMessage("should be escaped with");
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkNotEscapedBracesError2() throws LocalizedString.I18NError {
        String source = "{key}}";
        thrown.expect(LocalizedString.I18NError.class);
        thrown.expectMessage("should be escaped with");
        LocalizedString.checkLocalizedStringFormat(source);
    }

    @Test
    public void checkEmptyKeyError() throws LocalizedString.I18NError {
        String source = "{}";
        thrown.expect(LocalizedString.I18NError.class);
        thrown.expectMessage("empty key");
        LocalizedString.checkLocalizedStringFormat(source);
    }
   
}
