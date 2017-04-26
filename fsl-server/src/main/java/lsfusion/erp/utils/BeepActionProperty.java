package lsfusion.erp.utils;

import com.google.common.base.Throwables;
import lsfusion.base.BaseUtils;
import lsfusion.server.classes.ValueClass;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.ClassPropertyInterface;
import lsfusion.server.logics.property.ExecutionContext;
import lsfusion.server.logics.scripted.ScriptingActionProperty;
import lsfusion.server.logics.scripted.ScriptingLogicsModule;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

public class BeepActionProperty extends ScriptingActionProperty {
    private final ClassPropertyInterface fileInterface;
    private final ClassPropertyInterface asyncInterface;


    public BeepActionProperty(ScriptingLogicsModule LM, ValueClass... valueClasses) {
        super(LM, valueClasses);

        Iterator<ClassPropertyInterface> i = interfaces.iterator();
        fileInterface = i.next();
        asyncInterface = i.next();
    }

    @Override
    protected void executeCustom(ExecutionContext<ClassPropertyInterface> context) throws SQLException, SQLHandledException {
        //do not use large files
        final byte[] inputFile = (byte[]) context.getKeyValue(fileInterface).getValue();
        boolean async = context.getKeyValue(asyncInterface).getValue() != null;
        if(inputFile != null) {
            if (async) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        beep(inputFile);
                    }
                }).run();
            } else {
                beep(inputFile);
            }
        }
    }

    private void beep(byte[] inputFile) {
        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(BaseUtils.getFile(inputFile)));
            clip.open(inputStream);
            clip.start();
        } catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    protected boolean allowNulls() {
        return true;
    }
}