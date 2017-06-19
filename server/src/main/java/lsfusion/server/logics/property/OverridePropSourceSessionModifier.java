package lsfusion.server.logics.property;

import lsfusion.base.FullFunctionSet;
import lsfusion.base.FunctionSet;
import lsfusion.base.col.SetFact;
import lsfusion.base.col.interfaces.immutable.ImSet;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.session.*;

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
    
    protected abstract void updateSource(P property, boolean dataChanged) throws SQLException, SQLHandledException;
    
    @Override
    protected void notifySourceChange(CalcProperty property, boolean dataChanged) throws SQLException, SQLHandledException {
        if (overrideProps != null && !getSQL().isInTransaction()) { // если в транзакции предполагается что все обновится само (в sessionEvent или очистится или откатится, в форме - refresh будет)
            for (CalcProperty changeProp : overrideProps.getProperties()) { // проверка overrideProps != null из-за того что eventChange может идти до конструктора
                if ((overrideTable == null || !overrideTable.contains(changeProp)) && CalcProperty.depends(getSourceProperties((P)changeProp), property)) {
                    updateSource((P)changeProp, dataChanged);
                }
            }
        }
    }
}
