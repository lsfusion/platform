package lsfusion.server.logics.property.actions.flow;

import com.google.common.base.Throwables;
import lsfusion.base.FunctionSet;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.*;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class NewSessionActionProperty extends AroundAspectActionProperty {
    private final FunctionSet<SessionDataProperty> migrateSessionProperties; // актуальны и для nested, так как иначе будет отличаться поведение от NEW SESSION
    private final boolean isNested;
    private final boolean singleApply;
    private final boolean newSQL; 

    public <I extends PropertyInterface> NewSessionActionProperty(LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                                  ActionPropertyMapImplement<?, I> action, boolean singleApply,
                                                                  boolean newSQL,
                                                                  FunctionSet<SessionDataProperty> migrateSessionProperties,
                                                                  boolean isNested) {
        super(caption, innerInterfaces, action);

        this.singleApply = singleApply;
        this.newSQL = newSQL;

        this.isNested = isNested;
        this.migrateSessionProperties = DataSession.adjustKeep(false, migrateSessionProperties);


        finalizeInit();
    }

    @Override
    protected ImMap<CalcProperty, Boolean> aspectChangeExtProps() {
        return super.aspectChangeExtProps().replaceValues(true);
    }

    @Override
    public ImMap<CalcProperty, Boolean> aspectUsedExtProps() {
        return super.aspectUsedExtProps().replaceValues(true);
    }

    @Override
    public CalcPropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return IsClassProperty.getMapProperty(
                super.calcWhereProperty().mapInterfaceClasses(ClassType.wherePolicy)); // нет смысла делать mapOld и нарушать , все равно весь механизм во многом эвристичен
    }

    protected ExecutionContext<PropertyInterface> beforeAspect(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        DataSession session = context.getSession();
        if(session.isInTransaction()) { // если в транзацкции
            session.addRecursion(aspectActionImplement.getValueImplement(context.getKeys(), context.getObjectInstances(), context.getFormAspectInstance()), migrateSessionProperties, singleApply);
            return null;
        }

        ExecutionContext.NewSession<PropertyInterface> newContext;
        if(newSQL) {
            SQLSession sql;
            try {
                sql = context.getDbManager().createSQL();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                throw Throwables.propagate(e);
            }
            newContext = context.newSession(sql);
        } else {
            newContext = context.newSession();
            DataSession newSession = newContext.getSession();
            if (isNested) {
                context.executeSessionEvents();

                newSession.setParentSession(session);
            } else {
                migrateSessionProperties(session, newSession);
            }
        }

        return newContext;
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        if (type == ChangeFlowType.NEWSESSION) 
            return true;
        if (type == ChangeFlowType.APPLY)
            return false;
        if (type == ChangeFlowType.CANCEL)
            return false;
        if (type == ChangeFlowType.FORMCHANGE && !isNested)
            return false;
        return super.hasFlow(type);
    }

    protected void afterAspect(FlowResult result, ExecutionContext<PropertyInterface> context, ExecutionContext<PropertyInterface> innerContext) throws SQLException, SQLHandledException {
        if (!context.getSession().isInTransaction() && !newSQL) {
            migrateSessionProperties(innerContext.getSession(), context.getSession());
        }
    }

    private void migrateSessionProperties(DataSession migrateFrom, DataSession migrateTo) throws SQLException, SQLHandledException {
        assert !newSQL;
        migrateFrom.copyDataTo(migrateTo, migrateSessionProperties);
    }

    protected void finallyAspect(ExecutionContext<PropertyInterface> context, ExecutionContext<PropertyInterface> innerContext) throws SQLException, SQLHandledException {
        try {
            ((ExecutionContext.NewSession<PropertyInterface>)innerContext).close(); // по сути и есть аналог try with resources ()
        } finally {
            if(newSQL) // тут конечно нюанс, что делать если newSession продолжит жить своей жизнью (скажем NEWSESSION NEWTHREAD, а не наоборот), реализуем потом (по аналогии с NEWSESSION) вместе с NESTED
                innerContext.getSession().sql.close();
        }
    }

    @Override
    public CustomClass getSimpleAdd() {
        return null; // нет смысла, так как все равно в другой сессии выполнение
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        return null; // aspectActionImplement.property.getSimpleDelete();
    }
}
