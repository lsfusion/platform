package lsfusion.gwt.client.form.controller;

import java.util.function.LongConsumer;

// what running a property's event reports to its caller: the request it made, whose index the caller keys its loading
// value and predictions by, or that it made none - cancelled, with no request to make (a DELETE with no row, a close or
// an input not confirmed), or failing to make it (a NEW whose id is not given, a request due once the form is closed),
// with the reason -, which only a caller waiting for the request needs to know
@FunctionalInterface
public interface ExecCallback {
    void onSuccess(long requestIndex);

    default void onCancel() {
    }

    default void onFailure(String reason) {
    }

    // this callback, then after with the request's index; a cancel and a failure passed on as they are
    default ExecCallback andThen(LongConsumer after) {
        ExecCallback callback = this;
        return new ExecCallback() {
            @Override
            public void onSuccess(long requestIndex) {
                callback.onSuccess(requestIndex);
                after.accept(requestIndex);
            }

            @Override
            public void onCancel() {
                callback.onCancel();
            }

            @Override
            public void onFailure(String reason) {
                callback.onFailure(reason);
            }
        };
    }
}
