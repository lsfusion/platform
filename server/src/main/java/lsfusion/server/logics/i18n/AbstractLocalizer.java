package lsfusion.server.logics.i18n;

import lsfusion.server.caches.IdentityLazy;

import java.util.Locale;

import static lsfusion.server.logics.i18n.LocalizedString.CLOSE_CH;
import static lsfusion.server.logics.i18n.LocalizedString.OPEN_CH;

public abstract class AbstractLocalizer implements LocalizedString.Localizer {

    @IdentityLazy
    public String localize(String source, Locale locale) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < source.length(); ++i) {
            char ch = source.charAt(i);
            if (ch == '\\' && i+1 < source.length()) {
                builder.append(source.charAt(i+1));
                ++i;
            } else if (ch == OPEN_CH) {
                // не учитывается вариант с экранированной закрывающей скобкой, но для этого должен быть ключ с фигурной скобкой
                int closePos = source.indexOf(CLOSE_CH, i+1);
                if (closePos == -1) {
                    builder.append(source.substring(i));
                    break;
                }

                builder.append(localizeKey(source.substring(i + 1, closePos), locale));
                i = closePos;
            } else {
                builder.append(source.charAt(i));
            }
        }
        return builder.toString();

    }
    
    protected abstract String localizeKey(String key, Locale locale);
}
