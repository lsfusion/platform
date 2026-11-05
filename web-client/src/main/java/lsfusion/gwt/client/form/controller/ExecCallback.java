package lsfusion.gwt.client.form.controller;

import java.util.function.LongConsumer;

// what a call that makes a request - a property's event run, a view's call (GFormController.requestPromise) - reports
// to its caller, at once or later (a NEW asks the server for the new object's id first, a close the user to confirm it):
// the request it made, whose index the caller keys what waits for the request by - a loading value, predictions, a
// Promise -, or that it made none - cancelled, with no request to make (a DELETE with no row, a close or an input not
// confirmed, a view's call that changes nothing), or failing to make it (a NEW whose id is not given, a request due
// once the form is closed), with the reason -, which only a caller waiting for the request needs to know
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
