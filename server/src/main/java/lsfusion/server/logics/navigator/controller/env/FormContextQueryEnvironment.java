package lsfusion.server.logics.navigator.controller.env;

import lsfusion.server.data.OperationOwner;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.type.TypeObject;
import lsfusion.server.data.type.parse.ValueParseInterface;
import lsfusion.server.data.value.NullValue;
import lsfusion.server.logics.classes.data.StringClass;
import lsfusion.server.logics.form.struct.FormEntity;

import java.util.Locale;

public class FormContextQueryEnvironment implements QueryEnvironment {

    FormEntity form;
    ContextQueryEnvironment env;

    public FormContextQueryEnvironment(FormEntity form, ContextQueryEnvironment env) {
        this.form = form;
        this.env = env;
    }

    public ValueParseInterface getSQLUser() {
        return env.getSQLUser();
    }

    @Override
    public ValueParseInterface getSQLAuthToken() {
        return env.getSQLAuthToken();
    }

    public ValueParseInterface getSQLComputer() {
        return env.getSQLComputer();
    }

    public ValueParseInterface getSQLConnection() {
        return env.getSQLConnection();
    }

    public OperationOwner getOpOwner() {
        return env.getOpOwner();
    }

    public ValueParseInterface getSQLForm() {
        return env.getSQLForm();
    }

    public ValueParseInterface getActiveForm() {
        String activeForm = form.getCustomizeForm().getCanonicalName();
        if(activeForm != null) {
            return new TypeObject(activeForm, StringClass.text);
        } else {
            return NullValue.instance.getParse(StringClass.text);
        }
    }

    @Override
    public Locale getLocale() {
        return env.getLocale();
    }

    public int getTransactTimeout() {
        return env.getTransactTimeout();
    }

    public ValueParseInterface getIsServerRestarting() {
        return env.getIsServerRestarting();
    }
}