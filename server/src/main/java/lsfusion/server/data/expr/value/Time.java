package lsfusion.server.data.expr.value;

import lsfusion.server.data.query.compile.CompileSource;
import lsfusion.server.logics.classes.data.DataClass;
import lsfusion.server.logics.classes.data.integral.IntegerClass;
import lsfusion.server.logics.classes.data.integral.LongClass;
import lsfusion.server.logics.classes.data.time.DateTimeClass;

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
                return DateTimeClass.dateTime;
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
