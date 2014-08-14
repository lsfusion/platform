package lsfusion.server.classes.sets;

import lsfusion.base.ExtraMultiIntersectSetWhere;
import lsfusion.base.ExtraSetWhere;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.classes.CustomClass;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.type.ObjectType;
import lsfusion.server.data.type.Type;
import lsfusion.server.logics.ClassCanonicalNameUtils;
import org.apache.poi.hssf.record.formula.functions.T;

public class ResolveUpClassSet extends AUpClassSet<ResolveUpClassSet> implements ResolveClassSet {
    public ResolveUpClassSet(CustomClass[] customClasses) {
        super(customClasses);
    }

    public ResolveUpClassSet(CustomClass cls) {
        super(cls);
    }

    protected ResolveUpClassSet createThis(CustomClass[] wheres) {
        return new ResolveUpClassSet(wheres);
    }

    protected CustomClass add(CustomClass addWhere, CustomClass[] wheres, int numWheres, CustomClass[] proceeded, int numProceeded) {
        return null;
    }

    public static final ResolveUpClassSet FALSE = new ResolveUpClassSet(new CustomClass[0]);
    protected ResolveUpClassSet FALSETHIS() {
        return FALSE;
    }

    public boolean containsAll(ResolveClassSet set, boolean implicitCast) {
        if(!(set instanceof ResolveUpClassSet))
            return false;
        
        ResolveUpClassSet upSet = ((ResolveUpClassSet)set);
        for(CustomClass upWhere : upSet.wheres)
            if(!has(upWhere))
                return false;
        return true;
    }

    public ValueClass getCommonClass() {
        return OrObjectClassSet.getCommonClass(SetFact.toSet(wheres));
    }

    public ResolveClassSet and(ResolveClassSet set) {
        if(set instanceof ResolveOrObjectClassSet)
            return set.and(this);            
        return and((ResolveUpClassSet)set);
    }

    public ResolveClassSet or(ResolveClassSet set) {
        return or((ResolveUpClassSet) set);
    }

    // мост между логикой вычислений и логикой infer / resolve

    // надо же куда то положить
    // может быть null
    public static ResolveClassSet toResolve(AndClassSet set) {
        if(set == null) return null;
        
        // будем assert'ить что сюда попадет, только то что используется в toAnd 
        return set.toResolve();
    }
    // может быть null
    private static AndClassSet toAnd(ResolveClassSet set) {
        if(set == null) return null;            
        return set.toAnd();
    }

    // могут быть null
    public static <T> ImMap<T, AndClassSet> toAnd(ImMap<T, ResolveClassSet> set) {
        return set.mapValues(new GetValue<AndClassSet, ResolveClassSet>() {
            public AndClassSet getMapValue(ResolveClassSet value) {
                return toAnd(value);
            }
        });
    }

    public UpClassSet toAnd() {
        return new UpClassSet(wheres);
    }

    @Override
    public String getCanonicalName() {
        return ClassCanonicalNameUtils.createName(this);
    }

    public CustomClass[] getCommonClasses() {
        return wheres;
    }
}
