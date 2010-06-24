package platform.interop.action;

import platform.interop.form.RemoteFormInterface;

import java.io.IOException;

// такая дебильная схема с Dispatcher'ом чтобы модульность не нарушать
public interface ClientActionDispatcher {

    public ClientActionResult execute(FormClientAction action);

    public RuntimeClientActionResult execute(RuntimeClientAction action);

    public ClientActionResult execute(ExportFileClientAction action);

    public ImportFileClientActionResult execute(ImportFileClientAction action);

    public ClientActionResult execute(SleepClientAction action);

    public ClientActionResult execute(MessageFileClientAction action);

    public ClientActionResult execute(UserChangedClientAction action);

    public ClientActionResult execute(MessageClientAction action);
}
