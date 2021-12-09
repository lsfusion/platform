package lsfusion.server.logics.property.data;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.cases.CaseExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.infer.CalcClassType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class SessionDataProperty extends DataProperty {

    public SessionDataProperty(LocalizedString caption, ValueClass value) {
        this(caption, new ValueClass[0], value);
    }

    public LocalNestedType nestedType;
    public boolean noNestingInNestedSession; // hack for sessionOwners 

    public SessionDataProperty(LocalizedString caption, ValueClass[] classes, ValueClass value) {
        this(caption, classes, value, false);
    }

    private final boolean noClasses;
    public SessionDataProperty(LocalizedString caption, ValueClass[] classes, ValueClass value, boolean noClasses) {
        super(caption, classes, value);

        this.noClasses = noClasses;
        
        finalizeInit();
    }

    @Override
    protected boolean noClasses() {
        return noClasses;
    }

    @Override
    protected boolean isClassVirtualized(CalcClassType calcType) {
        return true; // because can be different from null, if there are changes
    }

    @Override
    public Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(!hasChanges(propChanges))
            return CaseExpr.NULL();
        return super.calculateExpr(joinImplement, calcType, propChanges, changedWhere);
    }

    public boolean isStored() {
        return false;
    }

    @Override
    public String getChangeExtSID() {
        return "sys" + hashCode(); // тут можно было бы сигнатуру вставить
    }
}

