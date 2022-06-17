CREATE OR REPLACE FUNCTION prefixSearch(config regconfig, querytext text) RETURNS tsquery AS
$$
BEGIN
    querytext := trim(querytext);
BEGIN
RETURN to_tsquery(config,
    CONCAT (
        REPLACE(
            REGEXP_REPLACE(
                REGEXP_REPLACE(
                        TRIM(TRAILING ' \|' FROM
                            REPLACE(
                                REGEXP_REPLACE(querytext, '(?<!\\),', '|', 'g'),
                            '\,', ',')),
                '[\s\|]*\|[\s\|]*','|', 'g'),
            '\s+', ':* & ', 'g'),
        '|', ':* | '),
    ':*'));
EXCEPTION WHEN OTHERS THEN
        RETURN websearch_to_tsquery(config, querytext);
END;
END;
$$ LANGUAGE 'plpgsql' IMMUTABLE;