package platform.interop.action;

import platform.interop.form.RemoteFormInterface;

import java.io.IOException;

// такая дебильная схема с Dispatcher'ом чтобы модульность не нарушать
public interface ClientActionDispatcher {

    public void executeForm(RemoteFormInterface remoteForm, boolean isPrintForm);
}
