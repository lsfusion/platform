CREATE OR REPLACE FUNCTION ${function.name}(anyelement) RETURNS ${param.type} AS
$$
    SELECT CASE WHEN $1 BETWEEN ${param.minvalue} AND ${param.maxvalue} THEN CAST($1 AS ${param.type}) ELSE NULL END;
$$ LANGUAGE 'sql' IMMUTABLE;

