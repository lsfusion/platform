package lsfusion.server.data.query;

public interface DynamicExecEnvOuter<OE, S extends DynamicExecEnvSnapshot<OE, S>> {

    OE getOuter();

    S getSnapshot();
}
