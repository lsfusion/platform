package platform.gwt.view.changes.dto;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

public class ObjectDTO implements Serializable {
    //нужно, чтобы об этих типах знал механизм сериализации GWT
    private Integer exposeInteger;
    private Long exposeLong;
    private Double exposeDouble;
    private Boolean exposeBoolean;
    private String exposeString;
    private Date exposeDate;
    private Timestamp exposeTimestamp;
    private byte[] exposeByteArray;

    private transient Object value;

    public ObjectDTO() {
    }

    public ObjectDTO(Object value) {
        this.value = value;
    }

    public Object getValue() {
        return value;
    }
}
