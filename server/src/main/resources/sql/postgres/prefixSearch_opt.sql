CREATE OR REPLACE FUNCTION prefixSearchPrepareQuery(querytext text, separator text) RETURNS text AS
$$
SELEC TRIM(BOTH ' \|' FROM -- 3. trim leading and trailing '|' and spaces
         REPLACE(
                 REGEXP_REPLACE(querytext, CONCAT('(?<!\\)', separator), '|', 'g'), -- 1. replace unquoted separator to '|'
         CONCAT('\', separator), separator) -- 2. replace quoted separator to unquoted
       )
;
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION prefixSearch(config regconfig, querytext text) RETURNS tsquery AS
$$
SELECT CASE
           -- use websearch if query contains special characters or is empty
           WHEN queryText ~ '^.*(\(|\)|\&|\:|\*|\!).*$' OR querytext = '' IS NOT FALSE THEN websearch_to_tsquery(config, querytext)
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


CREATE OR REPLACE FUNCTION prefixSearch(config regconfig, querytext text, separator text) RETURNS tsquery AS
$$
SELECT prefixSearch(config, prefixSearchPrepareQuery(querytext, separator));
$$ LANGUAGE 'sql' IMMUTABLE;