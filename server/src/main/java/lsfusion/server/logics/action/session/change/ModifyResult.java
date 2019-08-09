package lsfusion.server.logics.action.session.change;

import lsfusion.base.col.interfaces.mutable.mapvalue.GetValue;
import lsfusion.server.logics.action.session.changed.UpdateResult;

public enum ModifyResult implements UpdateResult {
    NO, // нет изменений
    DATA, // только данные в таблице (нет смысла пересчитывать выражения)
    DATA_SOURCE; // данные и метаданные

    
    public boolean dataChanged() {
        return this == DATA || this == DATA_SOURCE;
    }
    public boolean sourceChanged() {
        return this == DATA_SOURCE;
    }

    @Override
    public UpdateResult or(UpdateResult result) {
        if(result instanceof ModifyResult)
            return or((ModifyResult)result);
        
        assert result == UpdateResult.SOURCE;
        if(this == NO)
            return result;
        assert this == DATA || this == DATA_SOURCE;
        return DATA_SOURCE;
    }

    public ModifyResult or(ModifyResult result) {
        if(this == DATA_SOURCE || result == DATA_SOURCE)
            return DATA_SOURCE;
        if(this == NO && result == NO)
            return NO;
        return DATA;
    }
    
    public <T> GetValue<ModifyResult, T> fnGetValue() {
        return value -> ModifyResult.this;
    }  
}
