package lsfusion.server.logics.property.actions.flow;

import com.google.common.base.Throwables;
import lsfusion.base.FunctionSet;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.server.Settings;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.data.SQLSession;
import lsfusion.server.form.instance.FormInstance;
import lsfusion.server.logics.i18n.LocalizedString;
import lsfusion.server.logics.property.*;
import lsfusion.server.session.DataSession;

import java.sql.SQLException;

public class NewSessionActionProperty extends AroundAspectActionProperty {
    private final FunctionSet<SessionDataProperty> migrateSessionProperties; // актуальны и для nested, так как иначе будет отличаться поведение от NEW SESSION
    private final boolean isNested;
    private final boolean singleApply;
    private final boolean newSQL; 
    private final boolean doApply;

    public <I extends PropertyInterface> NewSessionActionProperty(LocalizedString caption, ImOrderSet<I> innerInterfaces,
                                                                  ActionPropertyMapImplement<?, I> action, boolean singleApply,
                                                                  boolean newSQL, boolean doApply,
                                                                  FunctionSet<SessionDataProperty> migrateSessionProperties,
                                                                  boolean isNested) {
        super(caption, innerInterfaces, action);

        this.singleApply = singleApply;
        this.newSQL = newSQL;

        this.isNested = isNested;
        this.doApply = doApply;
        this.migrateSessionProperties = DataSession.adjustKeep(migrateSessionProperties);


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
            session.addRecursion(aspectActionImplement.getValueImplement(context.getKeys()), migrateSessionProperties, singleApply);
            return null;
        }


        DataSession newSession;
        if(newSQL) {
            SQLSession sql;
            try {
                sql = context.getDbManager().createSQL();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                throw Throwables.propagate(e);
            }
            newSession = session.createSession(sql);
        } else {
            newSession = session.createSession();
            if (isNested) {
                session.executeSessionEvents(context.getFormInstance(), context.stack);

                newSession.setParentSession(session);
            } else {
                migrateSessionProperties(session, newSession);
            }
        }

        return context.override(newSession);
    }

    @Override
    public boolean hasFlow(ChangeFlowType type) {
        return type == ChangeFlowType.NEWSESSION || (!(type == ChangeFlowType.APPLY || type == ChangeFlowType.CANCEL) && super.hasFlow(type));
    }

    protected void afterAspect(FlowResult result, ExecutionContext<PropertyInterface> context, ExecutionContext<PropertyInterface> innerContext) throws SQLException, SQLHandledException {
        if (!context.getSession().isInTransaction() && !newSQL) {
            migrateSessionProperties(innerContext.getSession(), context.getSession());
        }

        if (doApply) {
            innerContext.apply();
        }

        FormInstance<?> formInstance = context.getFormInstance();
        if (formInstance != null && !Settings.get().getUseUserChangesSync()) {
            formInstance.refreshData();
        }
    }

    private void migrateSessionProperties(DataSession migrateFrom, DataSession migrateTo) throws SQLException, SQLHandledException {
        assert !newSQL;
        migrateFrom.copyDataTo(migrateTo, migrateSessionProperties);
    }

    protected void finallyAspect(ExecutionContext<PropertyInterface> context, ExecutionContext<PropertyInterface> innerContext) throws SQLException, SQLHandledException {
        DataSession session = innerContext.getSession();
        try {
            session.close(); // по сути и есть аналог try with resources ()
        } finally {
            if(newSQL) // тут конечно нюанс, что делать если newSession продолжит жить своей жизнью (скажем NEWSESSION NEWTHREAD, а не наоборот), реализуем потом (по аналогии с NEWSESSION) вместе с NESTED
                session.sql.close();
        }
    }

    @Override
    public CustomClass getSimpleAdd() {
        return null; // нет смысла, так как все равно в другой сессии выполнение
    }

    @Override
    public PropertyInterface getSimpleDelete() {
        return aspectActionImplement.property.getSimpleDelete();
    }
}
