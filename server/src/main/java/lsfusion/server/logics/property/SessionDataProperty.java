package lsfusion.server.logics.property;

import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.cases.CaseExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.session.PropertyChanges;

import java.util.Arrays;

public class SessionDataProperty extends DataProperty {

    public SessionDataProperty(String caption, ValueClass value) {
        this(caption, new ValueClass[0], value);
    }

    public boolean isNested = false;

    public SessionDataProperty(String caption, ValueClass[] classes, ValueClass value) {
        this(caption, classes, value, false);
    }

    private final boolean noClasses;
    public SessionDataProperty(String caption, ValueClass[] classes, ValueClass value, boolean noClasses) {
        super(caption, classes, value);

        this.noClasses = noClasses;
        
        finalizeInit();
    }

    @Override
    protected boolean noClasses() {
        return noClasses;
    }

    @Override
    public Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(calcType instanceof CalcClassType)
            return getVirtualTableExpr(joinImplement, (CalcClassType) calcType);
        if(propChanges.isEmpty())
            return CaseExpr.NULL;
        return super.calculateExpr(joinImplement, calcType, propChanges, changedWhere);
    }

    public boolean isStored() {
        return false;
    }

    @Override
    public boolean ignoreReadOnlyPolicy() {
        return true;
    }

    @Override
    public String getChangeExtSID() {
        return "sys" + hashCode(); // тут можно было бы сигнатуру вставить
    }
}

