CREATE OR REPLACE FUNCTION ${function.name}(anyelement) RETURNS ${param.type} AS
$$
BEGIN
    RETURN CAST($1 AS ${param.type});
EXCEPTION
    WHEN OTHERS THEN
        RETURN NULL;
END
$$ LANGUAGE 'plpgsql' IMMUTABLE;

