package lsfusion.gwt.form.client.dispatch;

import net.customware.gwt.dispatch.client.DefaultExceptionHandler;

public class NavigatorDispatchAsync extends DispatchAsyncWrapper {
    public static class Instance {
        private static final NavigatorDispatchAsync instance = new NavigatorDispatchAsync();

        public static NavigatorDispatchAsync get() {
            return instance;
        }
    }

    private NavigatorDispatchAsync() {
        super(new DefaultExceptionHandler());
    }
}
