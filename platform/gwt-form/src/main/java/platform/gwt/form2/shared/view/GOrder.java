package platform.gwt.form2.shared.view;

import java.io.IOException;

public enum GOrder {
    REPLACE, ADD, REMOVE, DIR;

    public byte serialize() throws IOException {
        switch(this) {
            case REPLACE:
                return 0;
            case ADD:
                return 1;
            case REMOVE:
                return 2;
            case DIR:
                return 3;
        }
        throw new RuntimeException("Serialize Scroll");
    }
}
