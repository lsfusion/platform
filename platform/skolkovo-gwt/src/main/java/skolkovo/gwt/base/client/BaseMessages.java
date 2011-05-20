package skolkovo.gwt.base.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface BaseMessages extends Messages {
    String internalServerErrorMessage();

    String yes();

    String no();

    String here();

    String showProfile();

    String logoffNotice();

    @Key("locale.ru")
    String localeRu();

    @Key("locale.en")
    String localeEn();

    String loading();

    String logoff();

    public static class Instance {
        private static final BaseMessages instance = (BaseMessages) GWT.create(BaseMessages.class);

        public static BaseMessages get() {
            return instance;
        }
    }
}
