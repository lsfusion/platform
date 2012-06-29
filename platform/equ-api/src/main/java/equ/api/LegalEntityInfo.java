package equ.api;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;

public class LegalEntityInfo implements Serializable {
    public String id;
    public String name;
    public String type;

    public LegalEntityInfo(String id, String name, String type) {
        this.id = id;
        this.name = name;
        this.type = type;
    }
}
