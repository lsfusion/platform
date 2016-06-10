package lsfusion.server.logics.security;

import lsfusion.interop.action.ConfirmClientAction;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.SecurityLogicsModule;
import lsfusion.server.logics.property.CalcProperty;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingErrorLog;
import lsfusion.server.session.DataSession;

import javax.swing.*;
import java.sql.SQLException;
import java.util.Iterator;

public class CheckCurrentDateDependsActionProperty extends ScriptingActionProperty {

    private final ClassPropertyInterface propertyInterface;

    public CheckCurrentDateDependsActionProperty(SecurityLogicsModule LM, ValueClass... classes) throws ScriptingErrorLog.SemanticErrorException {
        super(LM, classes);
        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        propertyInterface = i.next();
    }

    @Override
    public void executeCustom(final ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {

        BusinessLogics BL = context.getBL();

        DataObject propertyObject = context.getDataKeyValue(propertyInterface);

        boolean allow = true;
        String dbNameProperty = (String) BL.reflectionLM.dbNameProperty.read(context, propertyObject);
        if (dbNameProperty != null) {
            for (Property property : context.getBL().getPropertyList())
                if (dbNameProperty.equals(property.getDBName())) {
                    if (property instanceof CalcProperty && ((CalcProperty) property).getRecDepends().contains(BL.timeLM.currentDate.property)) {
                        allow = JOptionPane.YES_OPTION == (Integer) context.requestUserInteraction(
                                new ConfirmClientAction("Свойство зависит от текущей даты",
                                        String.format("Свойство %s зависит от текущей даты. Вы уверены, что хотите его залогировать?", property.getDBName())));
                    }
                    break;
                }
        }
        if (!allow) {
            try(DataSession session = context.createSession()) {
                BL.reflectionLM.userLoggableProperty.change((Boolean) null, session, propertyObject);
                session.apply(BL);
            }
        }
    }
}