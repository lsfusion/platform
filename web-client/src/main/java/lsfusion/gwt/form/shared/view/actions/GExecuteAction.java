package lsfusion.gwt.form.shared.view.actions;

public abstract class GExecuteAction implements GAction {

    public final Object dispatch(GActionDispatcher dispatcher) throws Throwable {
        execute(dispatcher);
        return null;
    }

    public abstract void execute(GActionDispatcher dispatcher) throws Throwable;
}
