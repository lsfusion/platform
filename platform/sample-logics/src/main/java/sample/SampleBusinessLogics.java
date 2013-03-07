package sample;

import platform.server.logics.BusinessLogics;

import java.io.IOException;

public class SampleBusinessLogics extends BusinessLogics<SampleBusinessLogics> {
    private SampleLogicsModule sampleLM;

    @Override
    protected void createModules() throws IOException {
        super.createModules();
        sampleLM = addModule(new SampleLogicsModule(LM));
    }
}
