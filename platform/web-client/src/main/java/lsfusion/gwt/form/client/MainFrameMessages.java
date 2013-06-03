package lsfusion.gwt.form.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface MainFrameMessages extends Messages {
    String title();

    String choseClass();

    String ok();

    String close();

    String cancel();

    public static class Instance {
        private static final MainFrameMessages instance = (MainFrameMessages) GWT.create(MainFrameMessages.class);

        public static MainFrameMessages get() {
            return instance;
        }
    }
}
