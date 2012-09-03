package platform.gwt.form2.client.dispatch;

import net.customware.gwt.dispatch.client.DefaultExceptionHandler;
import net.customware.gwt.dispatch.client.ExceptionHandler;
import net.customware.gwt.dispatch.client.standard.StandardDispatchAsync;

public class NavigatorDispatchAsync extends StandardDispatchAsync {
    public static class Instance {
        private static final NavigatorDispatchAsync instance = new NavigatorDispatchAsync();

        public static NavigatorDispatchAsync get() {
            return instance;
        }
    }

    public NavigatorDispatchAsync() {
        this(new DefaultExceptionHandler());
    }

    public NavigatorDispatchAsync(ExceptionHandler exceptionHandler) {
        super(exceptionHandler);
    }
}
