package platform.server.logics.property;

import platform.base.BaseUtils;
import platform.base.OrderedMap;
import platform.base.QuickSet;
import platform.base.TwinImmutableInterface;
import platform.server.caches.AbstractOuterContext;
import platform.server.caches.IdentityLazy;
import platform.server.caches.hash.HashContext;
import platform.server.classes.ValueClass;
import platform.server.data.Field;
import platform.server.data.KeyField;
import platform.server.data.PropertyField;
import platform.server.data.Table;
import platform.server.data.expr.*;
import platform.server.data.expr.query.Stat;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.expr.where.pull.ExprPullWheres;
import platform.server.data.query.CompileSource;
import platform.server.data.query.Join;
import platform.server.data.query.JoinData;
import platform.server.data.query.innerjoins.GroupJoinsWheres;
import platform.server.data.query.stat.InnerBaseJoin;
import platform.server.data.query.stat.KeyStat;
import platform.server.data.query.stat.StatKeys;
import platform.server.data.translator.MapTranslate;
import platform.server.data.translator.QueryTranslator;
import platform.server.data.type.Type;
import platform.server.data.where.DataWhereSet;
import platform.server.data.where.MapWhere;
import platform.server.data.where.Where;
import platform.server.data.where.WhereBuilder;
import platform.server.data.where.classes.ClassExprWhere;
import platform.server.data.where.classes.ClassWhere;
import platform.server.session.BaseMutableModifier;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;

import java.util.*;

import static java.util.Collections.singletonMap;
import static platform.base.BaseUtils.crossJoin;
import static platform.base.BaseUtils.join;
import static platform.base.BaseUtils.merge;

public class SessionDataProperty extends DataProperty implements NoValueProperty {

    public SessionDataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, classes, value);

        modifier.addProperty(this);

        finalizeInit();
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
                SessionDataProperty sessionProperty = (SessionDataProperty) property;
                Map<ClassPropertyInterface,KeyExpr> mapKeys = sessionProperty.getMapKeys();
                ClassTable classTable = sessionProperty.getClassTable();
                Join<PropertyField> classJoin = classTable.join(join(classTable.mapFields, mapKeys));
                return (PropertyChange<P>) new PropertyChange<ClassPropertyInterface>(mapKeys, classJoin.getExpr(classTable.propValue), classJoin.getWhere());
            }
            return null;
        }
    }
    public final static Modifier modifier = new Modifier(); // modifier для noValue

    @IdentityLazy
    public ClassTable getClassTable() {
        return new ClassTable(this);
    }
    
    public static class ClassTable extends Table {
        
        public final Map<KeyField, ClassPropertyInterface> mapFields;
        public final PropertyField propValue;

        public ClassTable(Property<ClassPropertyInterface> property) {
            super(property.getSID());

            mapFields = new HashMap<KeyField, ClassPropertyInterface>();
            for(ClassPropertyInterface propInterface : property.interfaces) {
                KeyField key = new KeyField(propInterface.getSID(), propInterface.interfaceClass.getType());
                keys.add(key); // чтобы порядок сохранить, хотя может и не критично
                mapFields.put(key, propInterface);
            }

            ValueClass valueClass = ((NoValueProperty) property).getValueClass();
            propValue = new PropertyField("value", valueClass.getType());
            properties.add(propValue);

            Map<KeyField, ValueClass> fieldClasses = BaseUtils.join(mapFields, IsClassProperty.getMapClasses(property.interfaces));
            classes = new ClassWhere<KeyField>(fieldClasses, true);
            propertyClasses.put(propValue, new ClassWhere<Field>(BaseUtils.add(fieldClasses, propValue, valueClass), true));
        }

        public StatKeys<KeyField> getStatKeys() {
            return getStatKeys(this, 100);
        }

        public Map<PropertyField, Stat> getStatProps() {
            return getStatProps(this, 100);
        }
    }
}

