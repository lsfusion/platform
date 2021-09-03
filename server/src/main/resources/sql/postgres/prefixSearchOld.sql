-- use plainto_tsquery (websearch_to_tsquery appeared in pgsql 11)
CREATE OR REPLACE FUNCTION prefixSearchOld(config regconfig, querytext text) RETURNS tsquery AS
$$
BEGIN
    querytext := trim(querytext);
BEGIN
RETURN to_tsquery(config,
    CONCAT (
        REPLACE(
            REGEXP_REPLACE(
                REGEXP_REPLACE(
                    TRIM(TRAILING ' \|' FROM querytext),
                '[\s\|]*\|[\s\|]*','|', 'g'),
            '\s+', ':* & ', 'g'),
        '|', ':* | '),
    ':*')
END);
EXCEPTION WHEN OTHERS THEN
        RETURN plainto_tsquery(config, querytext);
END;
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;