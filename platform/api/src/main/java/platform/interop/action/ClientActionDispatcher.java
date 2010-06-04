package platform.interop.action;

import platform.interop.form.RemoteFormInterface;

import java.io.IOException;

// такая дебильная схема с Dispatcher'ом чтобы модульность не нарушать
public interface ClientActionDispatcher {

    public ClientActionResult executeForm(FormClientAction action);

    public RuntimeClientActionResult executeRuntime(RuntimeClientAction action);

    public ClientActionResult executeExportFile(ExportFileClientAction action);

    public ImportFileClientActionResult executeImportFile(ImportFileClientAction action);
}
