package platform.server.data;

import platform.server.classes.*;
import platform.server.data.query.CompileSource;

public enum Time {
    EPOCH, HOUR, DATETIME;

    public ConcreteValueClass getConcreteValueClass() {
        switch (this) {
            case HOUR:
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
            case EPOCH:
                return compile.syntax.getEpoch();
            case DATETIME:
                return compile.syntax.getDateTime();
        }
        throw new RuntimeException("Unknown time");
    }
}
