package lsfusion.gwt.form.shared.view.changes.dto;

import java.io.Serializable;

//используем один класс для GFilterValueDTO для простоты...
//если в будущем действительно понадобится, то можно будет сделать иерархию в соотв. с GFilterValue+
public class GFilterValueDTO implements Serializable {
    public int typeID;
    public Serializable content;

    public GFilterValueDTO() {
    }

    public GFilterValueDTO(int typeID, Serializable content) {
        this.typeID = typeID;
        this.content = content;
    }
}
