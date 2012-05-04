package platform.client.form;

import com.google.common.base.Preconditions;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.client.logics.classes.ClientType;

public class SimpleEditPropertyDispatcher extends EditPropertyDispatcher {
    private final Handler handler;
    private final ClientFormController form;

    private Object value = null;

    public SimpleEditPropertyDispatcher(ClientFormController form) {
        super(new Handler());

        this.form = form;
        this.handler = (Handler) super.handler;

        handler.setDispatcher(this);
    }

    @Override
    public final boolean executePropertyEditAction(ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) {
        Preconditions.checkState(false, "SimpleEditPropertyDispatcher.executePropertyEditAction(..) shouldn't be directly called");
        return false;
    }

    public boolean executePropertyEditAction(Object value, ClientPropertyDraw property, ClientGroupObjectValue columnKey, String actionSID) {
        this.value = value;
        return super.executePropertyEditAction(property, columnKey, actionSID);
    }

    private void commitValue() {
        commitValue(value);
    }

    private ClientFormController getForm() {
        return form;
    }

    private static class Handler implements EditPropertyHandler {
        private SimpleEditPropertyDispatcher dispatcher;

        @Override
        public boolean requestValue(ClientType valueType, Object oldValue) {
            dispatcher.commitValue();
            return true;
        }

        @Override
        public ClientFormController getForm() {
            return dispatcher.getForm();
        }

        public void setDispatcher(SimpleEditPropertyDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }
    }
}
