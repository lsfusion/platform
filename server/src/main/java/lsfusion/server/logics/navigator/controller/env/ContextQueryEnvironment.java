package lsfusion.server.logics.navigator.controller.env;

import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.TypeObject;
import lsfusion.server.data.type.parse.LogicalParseInterface;
import lsfusion.server.data.type.parse.ValueParseInterface;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.classes.data.StringClass;

import java.util.Locale;


public class ContextQueryEnvironment implements QueryEnvironment {

    private final SQLSessionContextProvider contextProvider;
    private final OperationOwner owner;

    public final IsServerRestartingController isServerRestarting;
    public final TimeoutController timeout;
    public final FormController form;
    public final LocaleController locale;

    public ContextQueryEnvironment(SQLSessionContextProvider contextProvider, OperationOwner owner, IsServerRestartingController isServerRestarting, TimeoutController timeout, FormController form, LocaleController locale) {
        this.contextProvider = contextProvider;
        this.owner = owner;

        this.isServerRestarting = isServerRestarting;
        this.timeout = timeout;
        this.form = form;
        this.locale = locale;
    }

    public ValueParseInterface getSQLUser() {
        Long currentUser = contextProvider.getCurrentUser();
        if (currentUser != null) {
            return new TypeObject(currentUser, ObjectType.instance);
        } else {
            return NullValue.instance.getParse(ObjectType.instance);
        }
    }

    @Override
    public ValueParseInterface getSQLAuthToken() {
        String currentAuthToken = contextProvider.getCurrentAuthToken();
        if (currentAuthToken != null) {
            return new TypeObject(currentAuthToken, StringClass.text);
        } else {
            return NullValue.instance.getParse(StringClass.text);
        }
    }

    public ValueParseInterface getSQLComputer() {
        Long currentComputer = contextProvider.getCurrentComputer();
        if (currentComputer != null) {
            return new TypeObject(currentComputer, ObjectType.instance);
        } else {
            return NullValue.instance.getParse(ObjectType.instance);
        }
    }

    public ValueParseInterface getSQLConnection() {
        Long currentConnection = contextProvider.getCurrentConnection();
        if(currentConnection != null) {
            return new TypeObject(currentConnection, ObjectType.instance);
        } else {
            return NullValue.instance.getParse(ObjectType.instance);
        }
    }

    public OperationOwner getOpOwner() {
        return owner;
    }

    public ValueParseInterface getSQLForm() {
        String currentForm = form.getCurrentForm();
        if(currentForm != null) {
            return new TypeObject(currentForm, StringClass.text);
        } else {
            return NullValue.instance.getParse(StringClass.text);
        }
    }

    @Override
    public Locale getLocale() {
        return locale.getLocale();
    }

    public int getTransactTimeout() {
        return timeout.getTransactionTimeout();
    }

    public ValueParseInterface getIsServerRestarting() {
        return new LogicalParseInterface() {
            public boolean isTrue() {
                return isServerRestarting.isServerRestarting();
            }
        };
    }
}
