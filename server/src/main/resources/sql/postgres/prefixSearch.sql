CREATE OR REPLACE FUNCTION prefixSearchPrepareQuery(querytext text) RETURNS text AS
$$
SELECT
    TRIM( -- 4. trim spaces
            TRIM(BOTH '\|' FROM -- 3. remove leading and trailing '|'
                 REPLACE(
                         REGEXP_REPLACE(querytext, '(?<!\\),', '|', 'g'), -- 1. replace unquoted ',' to '|'
                         '\,', ',') -- 2. replace quoted ',' to unquoted ','
                )
        )
;
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION prefixSearch(config regconfig, querytext text, isOld boolean) RETURNS tsquery AS
$$
SELECT CASE
           -- use websearch if query contains special characters or is empty
           -- use plainto_tsquery for old pgsql (websearch_to_tsquery appeared in pgsql 11)
           WHEN queryText ~ '^.*(\(|\)|\&|\:|\*|\!).*$' OR querytext = '' IS NOT FALSE THEN
			(CASE WHEN isOld THEN plainto_tsquery(config, querytext) ELSE websearch_to_tsquery(config, querytext) END)
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


CREATE OR REPLACE FUNCTION prefixSearch(config regconfig, querytext text) RETURNS tsquery AS
$$
SELECT prefixSearch(config, prefixSearchPrepareQuery(querytext), false);
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION prefixSearchOld(config regconfig, querytext text) RETURNS tsquery AS
$$
SELECT prefixSearch(config, prefixSearchPrepareQuery(querytext), true);
$$ LANGUAGE 'sql' IMMUTABLE;