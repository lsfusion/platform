CREATE OR REPLACE FUNCTION prefixSearchPrepareQuery(querytext text, separator text) RETURNS text AS
$$
SELECT TRIM(BOTH e' \r\n\t\|' FROM -- 3. trim leading and trailing spaces, new lines, tabs and '|'
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
           WHEN queryText ~ '^.*(\(|\)|\&|\:|\*|\!|''|<|>).*$' OR querytext = '' IS NOT FALSE THEN websearch_to_tsquery(config, querytext)
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

CREATE OR REPLACE FUNCTION prefixSearchExact(config regconfig, querytext text) RETURNS tsquery AS
$$
SELECT phraseto_tsquery(config, queryText);
$$ LANGUAGE 'sql' IMMUTABLE;


CREATE OR REPLACE FUNCTION prefixSearchExact(config regconfig, querytext text, separator text) RETURNS tsquery AS
$$
SELECT prefixSearchExact(config, prefixSearchPrepareQuery(querytext, separator));
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION prefixSearchLikePrepareQuery(query text) RETURNS text AS
$$
SELECT regexp_replace(
               regexp_replace(query, '[^[:alnum:][:space:]]+', '', 'g'),  -- remove all except letters, numbers and spaces
               '[[:space:]]+', ' ', 'g'                                   -- replace multi spaces to single spaces
       );
$$
LANGUAGE sql IMMUTABLE;

CREATE OR REPLACE FUNCTION prefixSearchLike(search text, match text) RETURNS numeric AS
$$
SELECT CASE WHEN prefixSearchLikePrepareQuery(search) ILIKE prefixSearchLikePrepareQuery(match) || '%' THEN 0.3 ELSE 0 END;
$$ LANGUAGE sql IMMUTABLE;