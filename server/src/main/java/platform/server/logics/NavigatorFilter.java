package platform.server.logics;

import platform.server.form.navigator.RemoteNavigator;

public abstract class NavigatorFilter {
    public final static NavigatorFilter FALSE = new NavigatorFilter() {
        public boolean accept(RemoteNavigator navigator) {
            return false;
        }
    };

    public final static NavigatorFilter EXPIRED = new NavigatorFilter() {
        public boolean accept(RemoteNavigator navigator) {
            long suspendTime = System.currentTimeMillis() - navigator.getLastUsedTime();
            return suspendTime > NavigatorsManager.MAX_FREE_NAVIGATOR_LIFE_TIME;
        }
    };

    public static NavigatorFilter single(final RemoteNavigator single) {
        return new NavigatorFilter() {
            public boolean accept(RemoteNavigator navigator) {
                return navigator == single;
            }
        };
    }

    public abstract boolean accept(RemoteNavigator navigator);
}
