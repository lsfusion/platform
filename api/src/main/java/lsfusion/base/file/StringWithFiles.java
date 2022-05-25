package lsfusion.base.file;

import java.io.Serializable;

public class StringWithFiles implements Serializable {
    public String[] names;
    public RawFileData[] files;
    public String[] postfixes;

    public StringWithFiles(String[] names, RawFileData[] files, String[] postfixes) {
        this.names = names;
        this.files = files;
        this.postfixes = postfixes;
    }
}