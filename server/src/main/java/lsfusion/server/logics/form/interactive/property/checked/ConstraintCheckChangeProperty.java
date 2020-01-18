package lsfusion.server.logics.form.interactive.property.checked;

import lsfusion.base.Pair;
import lsfusion.base.Result;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.query.GroupExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.expr.value.ValueExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.function.Function;

public class ConstraintCheckChangeProperty<T extends PropertyInterface,P extends PropertyInterface> extends PullChangeProperty<T, P, ConstraintCheckChangeProperty.Interface<P>> {

    public abstract static class Interface<P extends PropertyInterface> extends PropertyInterface<Interface<P>> {

        Interface(int ID) {
            super(ID);
        }

        public abstract Expr getExpr();
    }

    public static class KeyInterface<P extends PropertyInterface> extends Interface<P> {

        P propertyInterface;

        public KeyInterface(P propertyInterface) {
            super(propertyInterface.ID);

            this.propertyInterface = propertyInterface;
        }

        public Expr getExpr() {
            return propertyInterface.getChangeExpr();
        }
    }

    public static class ValueInterface<P extends PropertyInterface> extends Interface<P> {

        Property<P> toChange;

        public ValueInterface(Property<P> toChange) {
            super(1000);

            this.toChange = toChange;  
        }

        public Expr getExpr() {
            return toChange.getChangeExpr();
        }
    }

    public static <P extends PropertyInterface> ImOrderSet<Interface<P>> getInterfaces(Property<P> property) {
        return property.getFriendlyOrderInterfaces().mapOrderSetValues((Function<P, Interface<P>>) KeyInterface::new).addOrderExcl(new ValueInterface<>(property));
    }
    
    public Pair<ImRevMap<Interface<P>, P>, Interface<P>> getMapInterfaces() {
        Result<ImSet<Interface<P>>> valueInterfaces = new Result<>();
        ImSet<Interface<P>> keyInterfaces = interfaces.split(interf -> interf instanceof KeyInterface, valueInterfaces);
        return new Pair<>(keyInterfaces.mapRevValues((Function<Interface<P>, P>) interf -> ((KeyInterface<P>) interf).propertyInterface), valueInterfaces.result.single());
    }

    public ConstraintCheckChangeProperty(Property<T> onChange, Property<P> toChange) {
        super(LocalizedString.concatList(onChange.caption, " по (", toChange.caption, ")"), getInterfaces(toChange), onChange, toChange);

        finalizeInit();
    }

    protected Expr calculateExpr(ImMap<Interface<P>, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(!calcType.isExpr()) // пока так
            calcType = CalcType.EXPR;

        ImMap<Interface<P>, Expr> mapExprs = interfaces.mapValues((Function<Interface<P>, Expr>) Interface::getExpr);

        WhereBuilder onChangeWhere = new WhereBuilder();
        Expr resultExpr = GroupExpr.create(mapExprs, onChange.getExpr(onChange.getMapKeys(),
                calcType, toChange.getChangeModifier(propChanges, false), onChangeWhere), onChangeWhere.toWhere(), GroupType.LOGICAL(), joinImplement); // constraints (filters)
        if(changedWhere!=null) changedWhere.add(resultExpr.getWhere());
        return ValueExpr.TRUE.and(resultExpr.getWhere().not());
    }
}
