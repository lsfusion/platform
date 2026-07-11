package lsfusion.server.base.controller.lifecycle;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.util.DefaultPropertiesPersister;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.Properties;

// loads startup .properties (lsfusion.properties, conf/settings.properties) as UTF-8 and strips a leading UTF-8 BOM.
// plain Properties.load reads ISO-8859-1 (so non-ASCII values become mojibake) and never skips a BOM (so the BOM glues to
// the first key, e.g. db.name -> "<BOM>db.name", which is then silently ignored and the hard-coded default wins).
public class BOMPropertiesFactoryBean extends PropertiesFactoryBean {

    public BOMPropertiesFactoryBean() {
        setFileEncoding("UTF-8");
        setPropertiesPersister(new DefaultPropertiesPersister() {
            @Override
            public void load(Properties props, Reader reader) throws IOException {
                super.load(props, skipBOM(reader));
            }
        });
    }

    private static Reader skipBOM(Reader reader) throws IOException {
        PushbackReader pushback = new PushbackReader(reader, 1);
        int first = pushback.read();
        if (first != -1 && first != '\uFEFF')
            pushback.unread(first);
        return pushback;
    }
}
