package lsfusion.server.logics.action.session.change.modifier;

import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImMap;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.base.lambda.set.FullFunctionSet;
import lsfusion.base.lambda.set.FunctionSet;
import lsfusion.server.data.sql.exception.SQLHandledException;
import lsfusion.server.logics.action.session.change.increment.IncrementChangeProps;
import lsfusion.server.logics.action.session.change.increment.IncrementTableProps;
import lsfusion.server.logics.action.session.change.increment.OverrideIncrementProps;
import lsfusion.server.logics.property.Property;

import java.sql.SQLException;

// Нужен для случаев хранения "долгих" PropertyChange  (то есть на периоды когда источники нужно обновлять)
// modifier - который хранит источники свойств и которые соответственно надо обновлять
public abstract class OverridePropSourceSessionModifier<P extends Property> extends OverrideSessionModifier {

    public OverridePropSourceSessionModifier (String debugInfo, IncrementChangeProps overrideChange, FunctionSet<Property> forceDisableHintIncrement, FunctionSet<Property> forceDisableNoUpdate, FunctionSet<Property> forceHintIncrement, FunctionSet<Property> forceNoUpdate, SessionModifier modifier) {
        this(debugInfo, null, overrideChange, forceDisableHintIncrement, forceDisableNoUpdate, forceHintIncrement, forceNoUpdate, modifier);
    }
    
    private final IncrementTableProps overrideTable;
    private final IncrementChangeProps overrideProps;
    
    public OverridePropSourceSessionModifier (String debugInfo, IncrementTableProps overrideTable, IncrementChangeProps overrideChange, FunctionSet<Property> forceDisableHintIncrement, FunctionSet<Property> forceDisableNoUpdate, FunctionSet<Property> forceHintIncrement, FunctionSet<Property> forceNoUpdate, SessionModifier modifier) {
        super(debugInfo, overrideTable != null ? new OverrideIncrementProps(overrideTable, overrideChange) : overrideChange, forceDisableHintIncrement, forceDisableNoUpdate, forceHintIncrement, forceNoUpdate, modifier);
        
        this.overrideTable = overrideTable;
        this.overrideProps = overrideChange;
        assert overrideProps != null;
    }

    public OverridePropSourceSessionModifier(String debugInfo, IncrementChangeProps overrideChange, boolean disableHintIncrement, SessionModifier modifier) {
        this(debugInfo, null, overrideChange, disableHintIncrement, modifier);
    }
    
    public OverridePropSourceSessionModifier(String debugInfo, IncrementTableProps overrideTable, IncrementChangeProps overrideChange, boolean disableHintIncrement, SessionModifier modifier) {
        this(debugInfo, overrideTable, overrideChange, disableHintIncrement ? FullFunctionSet.<Property>instance() : SetFact.<Property>EMPTY(), FullFunctionSet.<Property>instance(), SetFact.<Property>EMPTY(), SetFact.<Property>EMPTY(), modifier);
    }

    // важный assert что getSourceProperties не должен быть depends в обе стороны от property !!! так как потенциально может быть рекурсия
    protected abstract ImSet<Property> getSourceProperties(P property);
    
    protected abstract void updateSource(P property, boolean dataChanged, boolean forceUpdate) throws SQLException, SQLHandledException;
    
    protected boolean noUpdateInTransaction() {
        return true;
    }
    
    private Boolean isChanged(P changeProp, ImMap<Property, Boolean> changed) {
        ImSet<Property> sourceProperties = getSourceProperties(changeProp);
        boolean isChanged = false;
        for(int i=0,size=changed.size();i<size;i++) {
            boolean dataChanged = changed.getValue(i);
            if(isChanged && !dataChanged)
                continue;
            if(Property.depends(sourceProperties, changed.getKey(i))) {
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
    protected void notifySourceChange(ImMap<Property, Boolean> changed, boolean forceUpdate) throws SQLException, SQLHandledException {
        if (overrideProps != null) { // проверка overrideProps != null из-за того что eventChange может идти до конструктора
            for (Property changeProp : overrideProps.getProperties()) {
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
