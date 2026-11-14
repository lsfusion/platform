package lsfusion.gwt.client.action;

// base of the terminal actions delivering a form-controller exec/eval/change outcome to the JS callback registered
// correlated by the dispatching response's requestIndex (see GFORM-CONTROLLER-EXEC-EVAL-PLAN §12.3/§12.4); converted from the interop
// ControllerCallbackClientAction subtree. Concrete: GControllerResultAction / GControllerExceptionAction.
public abstract class GControllerCallbackAction extends GExecuteAction {
    public GControllerCallbackAction() {}
}
