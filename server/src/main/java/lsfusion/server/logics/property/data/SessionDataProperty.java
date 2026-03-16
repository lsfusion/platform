package lsfusion.server.logics.property.data;

import lsfusion.base.Pair;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImList;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.lambda.set.SFunctionSet;
import lsfusion.server.data.expr.Expr;
import lsfusion.server.data.expr.where.cases.CaseExpr;
import lsfusion.server.data.where.WhereBuilder;
import lsfusion.server.logics.action.session.LocalNestedType;
import lsfusion.server.logics.action.session.change.PropertyChanges;
import lsfusion.server.logics.action.session.change.StructChanges;
import lsfusion.server.logics.classes.ValueClass;
import lsfusion.server.logics.property.CalcType;
import lsfusion.server.logics.property.Property;
import lsfusion.server.logics.property.classes.ClassPropertyInterface;
import lsfusion.server.logics.property.classes.infer.CalcClassType;
import lsfusion.server.physics.dev.i18n.LocalizedString;

public class SessionDataProperty extends DataProperty {

    private static final ValueClass[] NO_CLASSES = new ValueClass[0];

    public static SessionDataProperty createChange(LocalizedString caption, ValueClass value, LocalNestedType nestedType) {
        return createChange(caption, NO_CLASSES, value, false, nestedType);
    }

    public static SessionDataProperty createChange(LocalizedString caption, ValueClass[] classes, ValueClass value, boolean noClasses, LocalNestedType nestedType) {
        return new SessionDataProperty(caption, classes, value, noClasses, nestedType);
    }

    public static SessionDataProperty createModifier(LocalizedString caption, ValueClass value) {
        return createModifier(caption, NO_CLASSES, value, false);
    }

    public static SessionDataProperty createModifier(LocalizedString caption, ValueClass[] classes, ValueClass value, boolean noClasses) {
        return new SessionDataProperty(caption, classes, value, noClasses, null);
    }

    public final LocalNestedType nestedType;
    public final static SFunctionSet<SessionDataProperty> NONESTING = element -> element.noNestingInNestedSession;

    private final boolean noClasses;

    private SessionDataProperty(LocalizedString caption, ValueClass[] classes, ValueClass value, boolean noClasses, LocalNestedType nestedType) {
        super(caption, classes, value);

        this.noClasses = noClasses;
        this.nestedType = nestedType;
        
        finalizeInit();
    }
    public boolean noNestingInNestedSession; // hack for sessionOwners
    public ImOrderSet<ClassPropertyInterface> returnInterfaces; // hack for return
    public Pair<ValueClass, ImList<ValueClass>> returnClasses; // hack for return

    public static FunctionSet<SessionDataProperty> keepNested(boolean manageSession) {
        return (SFunctionSet<SessionDataProperty>) element -> element.nestedType != null && (element.nestedType == LocalNestedType.ALL || (element.nestedType == LocalNestedType.MANAGESESSION) == manageSession);
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
    public ImSet<Property> calculateUsedChanges(StructChanges propChanges) {
        if(event == null) // optimization, if there are no events we don't need classes either
            return SetFact.EMPTY();
        return super.calculateUsedChanges(propChanges);
    }

    @Override
    public Expr calculateExpr(ImMap<ClassPropertyInterface, ? extends Expr> joinImplement, CalcType calcType, PropertyChanges propChanges, WhereBuilder changedWhere) {
        if(!hasChanges(propChanges)) // optimization
            return CaseExpr.NULL();
        return super.calculateExpr(joinImplement, calcType, propChanges, changedWhere);
    }

    public boolean isStored() {
        return false;
    }

    @Override
    public String getChangeExtSID() {
        return "SESSIONDATA"; // + hashCode(); // тут можно было бы сигнатуру вставить
    }
}

