package platform.server.logics.property;

import platform.base.FunctionSet;
import platform.server.classes.ValueClass;

public class StoredDataProperty extends DataProperty {

    public StoredDataProperty(String sID, String caption, ValueClass[] classes, ValueClass value) {
        super(sID, caption, classes, value);

        finalizeInit();
    }

    public boolean isStored() {
        return true;
    }
    
    public static FunctionSet<CalcProperty> set = new FunctionSet<CalcProperty>() {
        public boolean contains(CalcProperty element) {
            return element instanceof StoredDataProperty;
        }
        public boolean isEmpty() {
            return false;
        }
        public boolean isFull() {
            return false;
        }
    };
}
