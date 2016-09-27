package lsfusion.server.logics.tasks.impl.simple;

import com.google.common.base.Throwables;
import lsfusion.server.logics.tasks.SimpleBLTask;

import java.io.IOException;
import java.util.Locale;

public class CreateModulesTask extends SimpleBLTask {
    public String getCaption() {
        return "Creating modules";
    }

    public void run() {
        try {
            System.out.println(Locale.getDefault());
            getBL().createModules();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
}
