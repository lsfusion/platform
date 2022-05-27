package lsfusion.base.file;

import java.io.Serializable;

public class StringWithFiles implements Serializable {
    public String[] prefixes; //prefixes always exceed names and files by 1
    public String[] names;
    public RawFileData[] files;

    public StringWithFiles(String[] prefixes, String[] names, RawFileData[] files) {
        this.prefixes = prefixes;
        this.names = names;
        this.files = files;
    }
}