package platform.base;

import java.util.concurrent.Callable;

public interface CallableCallback<T> extends Callable<T>, Callback<T> {
}