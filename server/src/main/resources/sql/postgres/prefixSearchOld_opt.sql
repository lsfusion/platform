CREATE OR REPLACE FUNCTION prefixSearchOldPrepareQuery(querytext text, separator text) RETURNS text AS
$$
SELECT TRIM(BOTH ' \|' FROM -- 3. trim leading and trailing '|' and spaces
            REPLACE(
                    REGEXP_REPLACE(querytext, CONCAT('(?<!\\)', separator), '|', 'g'), -- 1. replace unquoted separator to '|'
            CONCAT('\', separator), separator) -- 2. replace quoted separator to unquoted
           )
;
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION prefixSearchOld(config regconfig, querytext text) RETURNS tsquery AS
$$
SELECT CASE
           -- use plainto_tsquery if query contains special characters or is empty
           -- use plainto_tsquery for old pgsql (websearch_to_tsquery appeared in pgsql 11)
           WHEN queryText ~ '^.*(\(|\)|\&|\:|\*|\!|<|>).*$' OR querytext = '' IS NOT FALSE THEN plainto_tsquery(config, querytext)
        ELSE to_tsquery(config,
            CONCAT (
                REPLACE(
                    REGEXP_REPLACE(
                        REGEXP_REPLACE(
                                queryText,
                                '[\s\|]*\|[\s\|]*','|', 'g'), -- 1. replace spaces + '|' to '|'
                        '\s+', ':* & ', 'g'), -- 2. replace spaces to '':* & '
                    '|', ':* | '), -- 3. replace '|' to ':* | '
            ':*')) END; -- 4. add ':*' in the end
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION prefixSearchOld(config regconfig, querytext text, separator text) RETURNS tsquery AS
$$
SELECT prefixSearchOld(config, prefixSearchOldPrepareQuery(querytext, separator));
$$ LANGUAGE 'sql' IMMUTABLE;