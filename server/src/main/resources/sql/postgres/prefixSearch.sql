CREATE OR REPLACE FUNCTION prefixSearch(config regconfig, querytext text) RETURNS tsquery AS
$$
BEGIN
    querytext := trim(querytext);
BEGIN
RETURN to_tsquery(config, CASE WHEN querytext = '' THEN '' ELSE CONCAT(array_to_string(regexp_split_to_array(querytext, E'\\s+'), ':* & '), ':*') END);
EXCEPTION WHEN OTHERS THEN
        RETURN websearch_to_tsquery(config, querytext);
END;
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;