package lsfusion.server.logics.debug;

import lsfusion.server.logics.DataObject;
import lsfusion.server.logics.ObjectValue;

import java.util.List;
import java.util.Map;

public class ActionWatchEntry {
    
    public static class Param {
        private String paramName;
        private ObjectValue value;

        public Param(String paramName, ObjectValue value) {
            this.paramName = paramName;
            this.value = value;
        }

        public String getShortName() {
            return paramName + " : " + value.getShortName();
        }

        public String toString() {
            return paramName + " : " + value;
        }
    }
    
    public final List<Param> params;
    private final ObjectValue value;

    public ActionWatchEntry(List<Param> params, ObjectValue value) {
        this.params = params;
        this.value = value;
    }

    @Override
    public String toString() {
        String result = "";
        for(Param param : params)
            result = (result.length() == 0 ? "" : result + ", ") + param.getShortName();
        if(value != null)
            result = value.getShortName() + " <- " + result + "";
        return result;
    }
}
