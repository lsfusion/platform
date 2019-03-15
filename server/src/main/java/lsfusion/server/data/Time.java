package lsfusion.server.data;

import lsfusion.server.data.query.CompileSource;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.DateTimeClass;
import lsfusion.server.logics.classes.data.IntegerClass;
import lsfusion.server.logics.classes.data.LongClass;

public enum Time {
    EPOCH, HOUR, MINUTE, DATETIME;

    public DataClass getConcreteValueClass() {
        switch (this) {
            case HOUR:
                return IntegerClass.instance;
            case MINUTE:
                return IntegerClass.instance;
            case EPOCH:
                return LongClass.instance;
            case DATETIME:
                return DateTimeClass.instance;
        }
        throw new RuntimeException("Unknown time");
    }

    public String getSource(CompileSource compile) {
        switch(this) {
            case HOUR:
                return compile.syntax.getHour();
            case MINUTE:
                return compile.syntax.getMinute();
            case EPOCH:
                return compile.syntax.getEpoch();
            case DATETIME:
                return compile.syntax.getDateTime();
        }
        throw new RuntimeException("Unknown time");
    }
}
