package lsfusion.gwt.client.form.property.cell.classes;

import java.io.Serializable;

public class GStringWithFiles implements Serializable {
    public String[] prefixes;
    public String[] urls;


    public GStringWithFiles() {
    }

    public GStringWithFiles(String[] prefixes, String[] urls) {
        this.prefixes = prefixes;
        this.urls = urls;
    }
}
