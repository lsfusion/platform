package platform.server.logics.property;

import platform.base.*;
import platform.server.classes.ValueClass;
import platform.server.data.expr.*;
import platform.server.data.expr.where.cases.CaseExpr;
import platform.server.data.where.WhereBuilder;
import platform.server.session.BaseMutableModifier;
import platform.server.session.PropertyChange;
import platform.server.session.PropertyChanges;

import java.util.*;

import static platform.base.BaseUtils.crossJoin;
import static platform.base.BaseUtils.join;
import static platform.base.BaseUtils.merge;

public class SessionDataProperty extends DataProperty {

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

/*    public static class WrapExpr extends VariableClassExpr {
        private final BaseExpr expr;

        public WrapExpr(BaseExpr expr) {
            this.expr = expr;
        }

        public static Expr create(Expr expr) {
            return new ExprPullWheres<Integer>() {
                protected Expr proceedBase(Map<Integer, BaseExpr> map) {
                    return new WrapExpr(map.get(0));
                }
            }.proceed(Collections.singletonMap(0, expr));
        }

        protected VariableClassExpr translate(MapTranslate translator) {
            return new WrapExpr(expr.translateOuter(translator));
        }

        public void fillAndJoinWheres(MapWhere<JoinData> joins, Where andWhere) {
            throw new RuntimeException("not supported");
        }

        public Stat getStatValue(KeyStat keyStat) {
            return expr.getStatValue(keyStat);
        }

        public InnerBaseJoin<?> getBaseJoin() {
            return expr.getBaseJoin();
        }

        public Stat getTypeStat(KeyStat keyStat) {
            return expr.getTypeStat(keyStat);
        }

        public Type getType(KeyType keyType) {
            return expr.getType(keyType);
        }

        public Expr translateQuery(QueryTranslator translator) {
            return create(expr.translateQuery(translator));
        }

        protected int hash(HashContext hash) {
            return expr.hashOuter(hash) + 10;
        }

        @Override
        public AndClassSet getAndClassSet(QuickMap<VariableClassExpr, AndClassSet> and) {
            return expr.getAndClassSet(and);
        }

        public String getSource(CompileSource compile) {
            if(compile instanceof ToString)
                return "WRAP(" + expr.getSource(compile) + ")";
            throw new RuntimeException("not supported");
        }

        @Override
        public Where calculateOrWhere() {
            return expr.getOrWhere();
        }

        @Override
        public Where calculateNotNullWhere() {
            return expr.getNotNullWhere();
        }

        public boolean twins(TwinImmutableInterface o) {
            return expr.equals(((WrapExpr)o).expr);
        }
    }*/

    public static class Modifier extends BaseMutableModifier {

        private final Map<Property, PropertyChange> noValueProps = new HashMap<Property, PropertyChange>();
        public <P extends PropertyInterface> void addProperty(Property<P> property) {
            noValueProps.put(property, property.getClassChange());
            addChange((Property)property);
        }

        /*        protected <P extends PropertyInterface> PropertyChange<P> getChange(OldProperty<P> old) {
            Property<P> mainProperty = old.property;
            Map<P, KeyExpr> mapKeys = mainProperty.getMapKeys();
            Expr expr = mainProperty.getExpr(mapKeys);
            return new PropertyChange<P>(mapKeys, WrapExpr.create(expr), expr.getWhere());
        }*/

        protected boolean isFinal() {
            return true;
        }

        protected Collection<Property> calculateProperties() {
            return BaseUtils.immutableCast(noValueProps.keySet());
        }

        protected <P extends PropertyInterface> PropertyChange<P> getPropertyChange(Property<P> property) {
            return noValueProps.get(property);
        }
    }
    public final static Modifier modifier = new Modifier(); // modifier для noValue
}

