package lsfusion.server.logics.property;

import lsfusion.base.lambda.set.FullFunctionSet;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.action.session.IncrementChangeProps;
import lsfusion.server.logics.action.session.IncrementTableProps;
import lsfusion.server.logics.action.session.OverrideIncrementProps;
import lsfusion.server.logics.action.session.SessionModifier;

import java.sql.SQLException;

// Нужен для случаев хранения "долгих" PropertyChange  (то есть на периоды когда источники нужно обновлять)
// modifier - который хранит источники свойств и которые соответственно надо обновлять
public abstract class OverridePropSourceSessionModifier<P extends CalcProperty> extends OverrideSessionModifier {

    public OverridePropSourceSessionModifier (String debugInfo, IncrementChangeProps overrideChange, FunctionSet<CalcProperty> forceDisableHintIncrement, FunctionSet<CalcProperty> forceDisableNoUpdate, FunctionSet<CalcProperty> forceHintIncrement, FunctionSet<CalcProperty> forceNoUpdate, SessionModifier modifier) {
        this(debugInfo, null, overrideChange, forceDisableHintIncrement, forceDisableNoUpdate, forceHintIncrement, forceNoUpdate, modifier);
    }
    
    private final IncrementTableProps overrideTable;
    private final IncrementChangeProps overrideProps;
    
    public OverridePropSourceSessionModifier (String debugInfo, IncrementTableProps overrideTable, IncrementChangeProps overrideChange, FunctionSet<CalcProperty> forceDisableHintIncrement, FunctionSet<CalcProperty> forceDisableNoUpdate, FunctionSet<CalcProperty> forceHintIncrement, FunctionSet<CalcProperty> forceNoUpdate, SessionModifier modifier) {
        super(debugInfo, overrideTable != null ? new OverrideIncrementProps(overrideTable, overrideChange) : overrideChange, forceDisableHintIncrement, forceDisableNoUpdate, forceHintIncrement, forceNoUpdate, modifier);
        
        this.overrideTable = overrideTable;
        this.overrideProps = overrideChange;
        assert overrideProps != null;
    }

    public OverridePropSourceSessionModifier(String debugInfo, IncrementChangeProps overrideChange, boolean disableHintIncrement, SessionModifier modifier) {
        this(debugInfo, null, overrideChange, disableHintIncrement, modifier);
    }
    
    public OverridePropSourceSessionModifier(String debugInfo, IncrementTableProps overrideTable, IncrementChangeProps overrideChange, boolean disableHintIncrement, SessionModifier modifier) {
        this(debugInfo, overrideTable, overrideChange, disableHintIncrement ? FullFunctionSet.<CalcProperty>instance() : SetFact.<CalcProperty>EMPTY(), FullFunctionSet.<CalcProperty>instance(), SetFact.<CalcProperty>EMPTY(), SetFact.<CalcProperty>EMPTY(), modifier);
    }

    // важный assert что getSourceProperties не должен быть depends в обе стороны от property !!! так как потенциально может быть рекурсия
    protected abstract ImSet<CalcProperty> getSourceProperties(P property);
    
    protected abstract void updateSource(P property, boolean dataChanged, boolean forceUpdate) throws SQLException, SQLHandledException;
    
    protected boolean noUpdateInTransaction() {
        return true;
    }
    
    private Boolean isChanged(P changeProp, ImMap<CalcProperty, Boolean> changed) {
        ImSet<CalcProperty> sourceProperties = getSourceProperties(changeProp);
        boolean isChanged = false;
        for(int i=0,size=changed.size();i<size;i++) {
            boolean dataChanged = changed.getValue(i);
            if(isChanged && !dataChanged)
                continue;
            if(CalcProperty.depends(sourceProperties, changed.getKey(i))) {
                isChanged = true;
                if(dataChanged)
                    return true;
            }
        }
            
        if(isChanged)
            return false;
        return null;
    }
    @Override
    protected void notifySourceChange(ImMap<CalcProperty, Boolean> changed, boolean forceUpdate) throws SQLException, SQLHandledException {
        if (overrideProps != null) { // проверка overrideProps != null из-за того что eventChange может идти до конструктора
            for (CalcProperty changeProp : overrideProps.getProperties()) {
                if (overrideTable == null || !overrideTable.contains(changeProp)) { // предполагается что очистка overrideTable, автоматически обновит overrideProps 
                    Boolean isChanged = isChanged(((P) changeProp), changed);
                    if (isChanged != null) {
                        updateSource((P)changeProp, isChanged, forceUpdate);
                    }
                }
            }
        }
    }
}
