package lsfusion.base.file;

import java.io.Serializable;

public class AppFileDataImage implements Serializable {

    public Serializable data;
    public AppFileDataImage() {
    }

    public AppFileDataImage(Serializable data) {
        this.data = data;
    }
}
