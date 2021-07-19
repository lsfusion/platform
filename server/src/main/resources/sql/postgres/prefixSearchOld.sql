-- use plainto_tsquery (websearch_to_tsquery appeared in pgsql 11)
CREATE OR REPLACE FUNCTION prefixSearchOld(config regconfig, querytext text) RETURNS tsquery AS
$$
BEGIN
    querytext := trim(querytext);
BEGIN
RETURN to_tsquery(config, CASE WHEN querytext = '' THEN '' ELSE CONCAT(array_to_string(regexp_split_to_array(querytext, E'\\s+'), ':* & '), ':*') END);
EXCEPTION WHEN OTHERS THEN
        RETURN plainto_tsquery(config, querytext);
END;
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;