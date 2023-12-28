--sql for proper work of json type (JSONTEXT)

--create functions: compare, less, less equals, equals, greater, greater equals and hash

CREATE OR REPLACE FUNCTION json_cmp(json, json)
  RETURNS INTEGER
  LANGUAGE sql
  IMMUTABLE
  STRICT
AS $function$
SELECT CASE
           WHEN $1::text > $2::text THEN 1
           WHEN $1::text < $2::text THEN -1
           ELSE 0
           END;
$function$;

CREATE OR REPLACE FUNCTION json_lt(json, json)
  RETURNS BOOLEAN
  LANGUAGE sql
  IMMUTABLE
  STRICT
AS $function$
SELECT $1::text < $2::text;
$function$;

CREATE OR REPLACE FUNCTION json_le(json, json)
  RETURNS BOOLEAN
  LANGUAGE sql
  IMMUTABLE
  STRICT
AS $function$
SELECT $1::text <= $2::text;
$function$;

CREATE OR REPLACE FUNCTION json_equals(json, json)
  RETURNS BOOLEAN
  LANGUAGE sql
  IMMUTABLE
  STRICT
AS $function$
SELECT $1::text = $2::text;
$function$;

CREATE OR REPLACE FUNCTION json_gt(json, json)
  RETURNS BOOLEAN
  LANGUAGE sql
  IMMUTABLE
  STRICT
AS $function$
SELECT $1::text > $2::text;
$function$;

CREATE OR REPLACE FUNCTION json_ge(json, json)
  RETURNS BOOLEAN
  LANGUAGE sql
  IMMUTABLE
  STRICT
AS $function$
SELECT $1::text >= $2::text;
$function$;

CREATE OR REPLACE FUNCTION json_hash(json)
  RETURNS INTEGER
  LANGUAGE sql
  IMMUTABLE
  STRICT
AS $function$
SELECT hashtext($1::text);
$function$;

--create operators less, less equals, equals, greater, greater equals

DROP OPERATOR IF EXISTS < (json, json) CASCADE;
CREATE OPERATOR < (
  PROCEDURE   = json_lt,
  LEFTARG     = json,
  RIGHTARG    = json,
  COMMUTATOR  = >,
  NEGATOR	  = >=,
  RESTRICT    = scalarltsel,
  JOIN        = scalarltjoinsel
);

DROP OPERATOR IF EXISTS <= (json, json) CASCADE;
CREATE OPERATOR <= (
  PROCEDURE   = json_le,
  LEFTARG     = json,
  RIGHTARG    = json,
  COMMUTATOR  = >=,
  NEGATOR	  = >,
  RESTRICT    = scalarlesel,
  JOIN        = scalarlejoinsel
);

DROP OPERATOR IF EXISTS = (json, json) CASCADE;
CREATE OPERATOR = (
  PROCEDURE   = json_equals,
  LEFTARG     = json,
  RIGHTARG    = json,
  COMMUTATOR  = =,
  NEGATOR 	  = <>,
  RESTRICT    = eqsel,
  JOIN        = eqjoinsel,
  HASHES,
  MERGES
);

DROP OPERATOR IF EXISTS > (json, json) CASCADE;
CREATE OPERATOR > (
  PROCEDURE   = json_gt,
  LEFTARG     = json,
  RIGHTARG    = json,
  COMMUTATOR  = <,
  NEGATOR     = <=,
  RESTRICT    = scalargtsel,
  JOIN        = scalargtjoinsel
);

DROP OPERATOR IF EXISTS >= (json, json) CASCADE;
CREATE OPERATOR >= (
  PROCEDURE   = json_ge,
  LEFTARG     = json,
  RIGHTARG    = json,
  COMMUTATOR  = <=,
  NEGATOR     = <,
  RESTRICT    = scalargesel,
  JOIN        = scalargejoinsel
);

--create operator class hash, use only equals operator and json_hash function
CREATE OPERATOR CLASS json_ops
  DEFAULT
  FOR TYPE json
  USING hash AS
  OPERATOR 1 =,
  FUNCTION 1 json_hash(json);

--create operator class btree, use less, less equals, equals, greater, greater equals operators and json_cmp function
CREATE OPERATOR CLASS json_ops
  DEFAULT
  FOR TYPE json
  USING btree AS
   OPERATOR        1       < ,
   OPERATOR        2       <= ,
   OPERATOR        3       = ,
   OPERATOR        4       >= ,
   OPERATOR        5       > ,
   FUNCTION        1       json_cmp(json, json);