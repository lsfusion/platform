package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.server.caches.hash.HashValues;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.cases.CaseExpr;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.WhereBuilder;
import platform.server.session.Changes;
import platform.server.session.Modifier;
import platform.server.session.ExprChanges;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SessionDataProperty extends DataProperty {

    public SessionDataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, classes, value);

        properties.add(this);
    }

    @Override
    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, Modifier<? extends Changes> modifier, WhereBuilder changedWhere) {
        if(modifier == (Modifier<? extends Changes>)defaultModifier)
            return CaseExpr.NULL;
        return super.calculateExpr(joinImplement, modifier, changedWhere);
    }

    public boolean isStored() {
        return false;
    }

    private final static Set<SessionDataProperty> properties = new HashSet<SessionDataProperty>();

    protected static class UsedChanges extends Changes<UsedChanges> {
        private final Set<SessionDataProperty> properties;

        private UsedChanges() {
            properties = new HashSet<SessionDataProperty>();
        }
        public final static UsedChanges EMPTY = new UsedChanges();

        public UsedChanges(Set<SessionDataProperty> properties) {
            this.properties = properties;
        }

        public UsedChanges(SessionDataProperty property) {
            properties = Collections.singleton(property);
        }

        private UsedChanges(UsedChanges changes, Changes merge) {
            super(changes, merge, true);
            properties = changes.properties;
        }
        public UsedChanges addChanges(Changes changes) {
            return new UsedChanges(this, changes);
        }

        private UsedChanges(UsedChanges changes, UsedChanges merge) {
            super(changes, merge);
            properties = BaseUtils.mergeSet(changes.properties, merge.properties);
        }
        public UsedChanges add(UsedChanges changes) {
            return new UsedChanges(this, changes);
        }

        public UsedChanges(UsedChanges changes, MapValuesTranslate mapValues) {
            super(changes, mapValues);
            properties = changes.properties;
        }
        public UsedChanges translate(MapValuesTranslate mapValues) {
            return new UsedChanges(this, mapValues);
        }

        @Override
        public boolean modifyUsed() {
            return !properties.isEmpty();
        }

        @Override
        public int hashValues(HashValues hashValues) {
            return super.hashValues(hashValues) * 31 + properties.hashCode();
        }

        @Override
        protected boolean modifyEquals(UsedChanges changes) {
            return properties.equals(changes.properties);
        }
    }

    // modifier для классов
    public final static Modifier<UsedChanges> modifier = new Modifier<UsedChanges>() {

        public UsedChanges newChanges() {
            return UsedChanges.EMPTY;
        }

        public UsedChanges newFullChanges() {
            return new UsedChanges(properties);
        }

        public UsedChanges used(Property property, UsedChanges usedChanges) {
            if(property instanceof SessionDataProperty)
                return new UsedChanges((SessionDataProperty) property);
            return usedChanges;
        }

        public ExprChanges getSession() {
            return ExprChanges.EMPTY;
        }

        public <P extends PropertyInterface> Expr changed(Property<P> property, Map<P, ? extends Expr> joinImplement, WhereBuilder changedWhere) {
            if(property instanceof SessionDataProperty) {
                changedWhere.add(ValueClassProperty.getIsClassWhere((Map<ClassPropertyInterface, ? extends Expr>) joinImplement, this, changedWhere));
                return ((DataProperty)property).value.getClassExpr();
            }
            return null;
        }

        public boolean neededClass(Changes changes) {
            return changes instanceof UsedChanges;
        }
    };
}

