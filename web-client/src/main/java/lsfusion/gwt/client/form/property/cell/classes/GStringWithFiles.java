package lsfusion.gwt.client.form.property.cell.classes;

import java.io.Serializable;

public class GStringWithFiles implements Serializable {
    public String[] prefixes;
    public Serializable[] urls; // String or AppStaticImage


    public GStringWithFiles() {
    }

    public GStringWithFiles(String[] prefixes, Serializable[] urls) {
        this.prefixes = prefixes;
        this.urls = urls;
    }
}
