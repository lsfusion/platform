package lsfusion.server.logics.property;

import lsfusion.base.FunctionSet;
import lsfusion.server.classes.ValueClass;

public class StoredDataProperty extends DataProperty {

    public StoredDataProperty(String caption, ValueClass[] classes, ValueClass value) {
        super(caption, classes, value);

        finalizeInit();
    }

    public boolean isStored() {
        return true;
    }

    // нет
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
