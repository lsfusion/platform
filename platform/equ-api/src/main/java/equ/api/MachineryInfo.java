package equ.api;

import java.io.Serializable;
import java.util.List;

public abstract class MachineryInfo implements Serializable {
    public Integer number;
    public String nameModel;
    public String handlerModel;
    public String port;
}
