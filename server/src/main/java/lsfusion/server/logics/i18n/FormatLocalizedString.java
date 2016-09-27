package lsfusion.server.logics.i18n;

import java.text.MessageFormat;
import java.util.Locale;

public class FormatLocalizedString extends LocalizedString {
    private Object[] params;
    
    public FormatLocalizedString(String source, Object... params) {
        super(source);
        this.params = params;
    }
    
    @Override
    public String getString(Locale locale, Localizer localizer) {
        String res = super.getString(locale, localizer);
        return MessageFormat.format(res, params);
    }
}
