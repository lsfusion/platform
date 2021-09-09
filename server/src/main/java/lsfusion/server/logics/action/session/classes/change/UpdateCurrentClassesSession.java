package lsfusion.server.logics.action.session.classes.change;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.QueryEnvironment;
import lsfusion.server.data.sql.SQLSession;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.data.sql.lambda.SQLRunnable;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.logics.BusinessLogics;
import lsfusion.server.logics.action.session.DataSession;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.modifier.DataSessionModifier;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.classes.user.BaseClass;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;

import java.sql.SQLException;
import java.util.List;

public class UpdateCurrentClassesSession {
    public final ClassChanges changes;    
    public final Modifier modifier;
    
    public final SQLSession sql;
    public final QueryEnvironment env;
    public final BaseClass baseClass;
    
    private final DataSession session; // чисто для проверки sameSession
    
    private final List<SQLRunnable> rollbackInfo;
    public void addRollbackInfo(SQLRunnable run) {
        rollbackInfo.add(run);
    }

    public ImSet<Property> getChangedProps() {
        return changes.getChangedProps(baseClass);
    }

    private final class DataModifier extends DataSessionModifier {

        public DataModifier() throws SQLException, SQLHandledException {
            super("updateclasses");
            eventDataChanges(getChangedProps());
        }

        @Override
        protected <P extends PropertyInterface> PropertyChange<P> getPropertyChange(Property<P> property) {
            return changes.getPropertyChange(property, baseClass);
        }

        @Override
        protected ImSet<Property> getChangedProps() {
            return UpdateCurrentClassesSession.this.getChangedProps();
        }

        @Override
        public long getMaxCount(Property recDepends) {
            return changes.getMaxDataUsed(recDepends);
        }

        @Override
        public SQLSession getSQL() {
            return sql;
        }

        @Override
        public BaseClass getBaseClass() {
            return baseClass;
        }

        @Override
        public QueryEnvironment getQueryEnv() {
            return env;
        }
    }

    public UpdateCurrentClassesSession(ClassChanges changes, Modifier modifier, SQLSession sql, QueryEnvironment env, BaseClass baseClass, List<SQLRunnable> rollbackInfo, DataSession session) throws SQLException, SQLHandledException {
        this.changes = changes;
        this.sql = sql;
        this.env = env;
        this.baseClass = baseClass;
        
        this.rollbackInfo = rollbackInfo;
        
        this.session = session;
        
        this.modifier = modifier == null ? new DataModifier() : modifier;
    }
    
    public boolean sameSession(DataSession session) {
        return this.session == session;
    }

    public ObjectValue updateCurrentClass(ObjectValue value) throws SQLException, SQLHandledException {
        return changes.updateCurrentClass(sql, env, baseClass, value);
    }

    public <K, T extends ObjectValue> boolean hasClassChanges(ImMap<K, T> objectValues) throws SQLException, SQLHandledException {
        ImMap<K, T> updatedObjectValues = updateCurrentClasses(objectValues); // все равно update попадет в кэш и повторное чтение будет куда быстрее (но эта оптимизация все равно нужна для корреляций)
        return !BaseUtils.hashEquals(objectValues, updatedObjectValues);
    }
    public <K, T extends ObjectValue> ImMap<K, T> updateCurrentClasses(ImMap<K, T> objectValues) throws SQLException, SQLHandledException {
        return changes.updateCurrentClasses(sql, env, baseClass, objectValues);
    }

    public <K, T extends ObjectValue> ImSet<CustomClass> packRemoveClasses(BusinessLogics BL) throws SQLException, SQLHandledException {
        return changes.packRemoveClasses(modifier, BL, sql, env);
    }
}
