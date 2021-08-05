package lsfusion.server.logics.action.session.changed;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.data.where.classes.ClassWhere;
import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.StructChanges;
import lsfusion.server.logics.event.PrevScope;
import lsfusion.server.logics.property.AggregateProperty;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.cases.CaseUnionProperty;
import lsfusion.server.logics.property.classes.infer.CalcClassType;
import lsfusion.server.logics.property.classes.infer.ExClassSet;
import lsfusion.server.logics.property.classes.infer.InferType;
import lsfusion.server.logics.property.classes.infer.Inferred;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.physics.admin.Settings;
import lsfusion.server.physics.admin.drilldown.form.DrillDownFormEntity;
import lsfusion.server.physics.admin.drilldown.form.OldDrillDownFormEntity;
import lsfusion.server.physics.dev.i18n.LocalizedString;

import java.util.Set;

public class OldProperty<T extends PropertyInterface> extends SessionProperty<T> {

    public OldProperty(Property<T> property, PrevScope scope) {
        super(LocalizedString.concat("(" + scope.getSID() + ",в БД) ", property.localizedToString()), property, scope);
        
        drawOptions.inheritDrawOptions(property.drawOptions);
    }

/*    @Override
    protected Collection<Pair<Property<?>, LinkType>> calculateLinks() {
        return BaseUtils.add(super.calculateLinks(), new Pair<Property<?>, LinkType>(property, LinkType.EVENTACTION)); // чтобы лексикографику для applied была
    }*/

    public OldProperty<T> getOldProperty() {
        return this;
    }

    public ChangedProperty<T> getChangedProperty() {
        return property.getChanged(IncrementType.CHANGED, scope);
    }

    protected Expr calculateExpr(ImMap<T, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        return property.getPrevExpr(joinImplement, calcType, propChanges);
    }

    @Override
    public ImSet<Property> calculateUsedChanges(StructChanges propChanges) {
        return property.getUsedChanges(propChanges.getPrev());
    }

    @Override
    protected boolean calculateHasGlobalPreread(boolean events) {
        return property.hasGlobalPreread(false); // we have to break a recursion
    }

    @Override
    protected boolean calculateHasPreread(StructChanges structChanges) {
        return property.hasPreread(structChanges.getPrev());
    }

    @Override
    public boolean calculateCheckRecursions(ImSet<CaseUnionProperty> abstractPath, ImSet<Property> path, Set<Property> marks) {
        return property.checkRecursions(abstractPath, path, marks);
    }

    @Override
    protected boolean isClassVirtualized(CalcClassType calcType) {
        return true;
    }

    @Override
    public ClassWhere<Object> calcClassValueWhere(CalcClassType type) {
        ClassWhere<Object> classValueWhere = super.calcClassValueWhere(type);
        if(type == CalcClassType.prevBase())
            return classValueWhere.getBase();
        return classValueWhere;
//        return super.getClassValueWhere(type);
    }

    public Inferred<T> calcInferInterfaceClasses(ExClassSet commonValue, InferType inferType) {
        Inferred<T> result = property.inferInterfaceClasses(commonValue, inferType);
        if(inferType == InferType.prevBase())
            result = result.getBase(inferType);
        return result;
    }

    @Override
    public boolean calcNeedInferredForValueClass(InferType inferType) {
        return property.needInferredForValueClass(inferType);
    }

    public ExClassSet calcInferValueClass(ImMap<T, ExClassSet> inferred, InferType inferType) {
        ExClassSet exClassSet = property.inferValueClass(inferred, inferType);
        if(inferType == InferType.prevBase())
            exClassSet = ExClassSet.getBase(exClassSet);
        return exClassSet;
    }

    @Override
    public boolean supportsDrillDown() {
        return property != null;
    }

    @Override
    public boolean drillDownInNewSession() {
        return true;
    }

    @Override
    public DrillDownFormEntity createDrillDownForm(LogicsModule LM) {
        return new OldDrillDownFormEntity(LocalizedString.create("{logics.property.drilldown.form.old}"), this, LM
        );
    }

    @Override
    public void finalizeAroundInit() {
        super.finalizeAroundInit();
        hideOlds(); // для multi-threading'а, возможно правильнее подсчитывать getLinks в ActionProperty, но похоже что разницы никакой, а там небольшая проблема с кжшами (links)
    }

    @Override
    public ImSet<SessionProperty> getSessionCalcDepends(boolean events) {
        if(hideOlds())
            return SetFact.EMPTY();
        return super.getSessionCalcDepends(events);
    }

    private boolean hideOlds() {
        return Settings.get().isUseEventValuePrevHeuristic() && property instanceof AggregateProperty && ((AggregateProperty)property).hasAlotKeys();
    }
}
