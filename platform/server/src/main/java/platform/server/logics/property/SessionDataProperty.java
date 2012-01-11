package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.ImmutableObject;
import platform.base.QuickMap;
import platform.base.SimpleMap;
import platform.server.caches.IdentityLazy;
import platform.server.classes.ValueClass;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.translator.MapValuesTranslate;
import platform.server.data.where.WhereBuilder;
import platform.server.session.BaseMutableModifier;
import platform.server.session.ModifyChange;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;

import java.util.*;

public class SessionDataProperty extends DataProperty implements NoValueProperty{

    public SessionDataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, classes, value);

        modifier.addProperty(this);
    }

    @Override
    public Expr calculateExpr(Map<ClassPropertyInterface, ? extends Expr> joinImplement, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return CaseExpr.NULL;
    }

    public boolean isStored() {
        return false;
    }


    public static class Modifier extends BaseMutableModifier {

        public final Set<NoValueProperty> noValueProps = new HashSet<NoValueProperty>();
        public void addProperty(NoValueProperty property) {
            noValueProps.add(property);
            addChange((Property)property);
        }

        protected boolean isFinal() {
            return true;
        }

        protected Collection<Property> calculateProperties() {
            return BaseUtils.immutableCast(noValueProps);
        }

        protected <P extends PropertyInterface> PropertyChange<P> getPropertyChange(Property<P> property) {
            if(noValueProps.contains(property)) {
                Property<ClassPropertyInterface> classProperty = (Property)property;
                Map<ClassPropertyInterface,KeyExpr> mapKeys = classProperty.getMapKeys();
                return (PropertyChange<P>) new PropertyChange<ClassPropertyInterface>(mapKeys, ((NoValueProperty)property).getValueClass().getClassExpr(),
                        ClassProperty.getIsClassWhere(mapKeys, PropertyChanges.EMPTY, null));
            }
            return null;
        }
    }
    public final static Modifier modifier = new Modifier(); // modifier для noValue
}

