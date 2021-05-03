package lsfusion.server.logics.property.oraction;

import lsfusion.base.BaseUtils;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.*;
import lsfusion.base.col.interfaces.mutable.MSet;
import lsfusion.base.col.interfaces.mutable.add.MAddSet;
import lsfusion.base.identity.IdentityObject;
import lsfusion.server.base.caches.LazyInit;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.PullExpr;
import lsfusion.server.data.expr.query.GroupType;
import lsfusion.server.data.value.ObjectValue;
import lsfusion.server.data.where.Where;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.action.controller.context.ExecutionContext;
import lsfusion.server.logics.action.implement.ActionMapImplement;
import lsfusion.server.logics.action.session.change.CalcDataType;
import lsfusion.server.logics.action.session.change.DataChanges;
import lsfusion.server.logics.action.session.change.PropertyChange;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.modifier.Modifier;
import lsfusion.server.logics.action.session.changed.OldProperty;
import lsfusion.server.logics.classes.user.CustomClass;
import lsfusion.server.logics.classes.user.set.AndClassSet;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.cases.CalcCase;
import lsfusion.server.logics.property.cases.graph.Graph;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.data.DataProperty;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.property.implement.PropertyMapImplement;

public class PropertyInterface<P extends PropertyInterface<P>> extends IdentityObject implements lsfusion.server.logics.property.implement.PropertyInterfaceImplement<P>, Comparable<P> {

    public PropertyInterface() {
        this(-1);
    }

    public PropertyInterface(int ID) {
        super(ID, "PropInt" + ID);
    }

    public static <T, P extends PropertyInterface> ImRevMap<T, P> getIdentityMap(ImMap<T, PropertyInterfaceImplement<P>> mapping) {
        MAddSet<PropertyInterface> checked = SetFact.mAddSet();
        for(PropertyInterfaceImplement<P> propImplement : mapping.valueIt())
            if(!(propImplement instanceof PropertyInterface && !checked.add((PropertyInterface) propImplement)))
                return null;
        return BaseUtils.immutableCast(mapping.toRevExclMap());
    }

    public String toString() {
        return "I/"+ID;
    }

    public Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, PropertyChanges changes, WhereBuilder changedWhere) {
        return mapExpr(joinImplement);
    }

    public Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges changes, WhereBuilder changedWhere) {
        return mapExpr(joinImplement);
    }

    public Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, PropertyChanges propChanges) {
        return mapExpr(joinImplement);
    }

    public Expr mapExpr(ImMap<P, ? extends Expr> joinImplement, Modifier modifier) {
        return mapExpr(joinImplement);
    }

    public Expr mapExpr(ImMap<P, ? extends Expr> joinImplement) {
        return joinImplement.get((P) this);
    }

    public Object read(ExecutionContext context, ImMap<P, ? extends ObjectValue> interfaceValues) {
        return interfaceValues.get((P) this).getValue();
    }

    public ObjectValue readClasses(ExecutionContext context, ImMap<P, ? extends ObjectValue> interfaceValues) {
        return interfaceValues.get((P) this);
    }

    public void mapFillDepends(MSet<Property> depends) {
    }

    public int mapHashCode() {
        return hashCode();
    }
    public boolean mapEquals(PropertyInterfaceImplement<P> implement) {
        return equals(implement);
    }

    public ImSet<OldProperty> mapOldDepends() {
        return SetFact.EMPTY();
    }

    public int compareTo(P o) {
        return ID-o.ID;
    }

    // для того чтобы "попробовать" изменения (на самом деле для кэша)
    @LazyInit
    public Expr getChangeExpr() {
        if(changeExpr==null)
            changeExpr = new PullExpr(ID);
        return changeExpr;
    }

    public Expr changeExpr;

    public DataChanges mapJoinDataChanges(ImMap<P, ? extends Expr> mapKeys, Expr expr, Where where, GroupType groupType, WhereBuilder changedWhere, PropertyChanges propChanges, CalcDataType type) {
        return DataChanges.EMPTY;
    }

    public void fill(MSet<P> interfaces, MSet<PropertyMapImplement<?, P>> properties) {
        interfaces.add((P) this);
    }

    public ImCol<P> getInterfaces() {
        return SetFact.singleton((P) this);
    }

    public <K extends PropertyInterface> PropertyInterfaceImplement<K> map(ImRevMap<P, K> remap) {
        return remap.get((P)this);
    }

    public ActionMapImplement<?, P> mapEventAction(String eventSID, ImList<Property> viewProperties) {
        return null;
    }

    public Property<?> mapViewProperty(CustomClass customClass, ImList<Property> viewProperties) {
        return null;
    }

    public Inferred<P> mapInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        return new Inferred<>((P) this, commonValue);
    }
    public boolean mapNeedInferredForValueClass(InferType inferType) {
        return true;
    }
    public ExClassSet mapInferValueClass(ImMap<P, ExClassSet> inferred, InferType inferType) {
        return inferred.get((P)this);
    }

    public AndClassSet mapValueClassSet(ClassWhere<P> interfaceClasses) {
        return interfaceClasses.getCommonClass((P)this);
    }

    public ImSet<DataProperty> mapChangeProps() {
        return SetFact.EMPTY();
    }

    public boolean mapHasAlotKeys() {
        return true;
    }

    public int mapEstComplexity() {
        return 0;
    }

    public boolean mapHasPreread(PropertyChanges propertyChanges) {
        return false;
    }
    public boolean mapHasPreread(Modifier modifier) {
        return false;
    }

    public long mapComplexity() {
        return 0;
    }

    public DataChanges mapJoinDataChanges(PropertyChange<P> change, CalcDataType type, GroupType groupType, WhereBuilder changedWhere, PropertyChanges propChanges) {
        return DataChanges.EMPTY;
    }

    public Graph<CalcCase<P>> mapAbstractGraph() {
        return null;
    }

    public boolean equalsMap(ActionOrPropertyInterfaceImplement object) {
        return equals(object);
    }

    public int hashMap() {
        return hashCode();
    }

    public boolean mapIsFull(ImSet<P> interfaces) {
        return false;
    }
}
