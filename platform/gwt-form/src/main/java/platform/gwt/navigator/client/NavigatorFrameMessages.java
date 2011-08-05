package platform.gwt.navigator.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Messages;

public interface NavigatorFrameMessages extends Messages {
    String title();

    public static class Instance {
        private static final NavigatorFrameMessages instance = (NavigatorFrameMessages) GWT.create(NavigatorFrameMessages.class);

        public static NavigatorFrameMessages get() {
            return instance;
        }
    }
}
