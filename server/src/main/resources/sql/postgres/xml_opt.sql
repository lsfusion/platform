--sql for proper work of xml type

--create functions: compare, less, less equals, equals, greater, greater equals and hash

CREATE OR REPLACE FUNCTION xml_cmp(xml, xml)
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

CREATE OR REPLACE FUNCTION xml_lt(xml, xml)
  RETURNS BOOLEAN
  LANGUAGE sql
  IMMUTABLE
  STRICT
AS $function$
SELECT $1::text < $2::text;
$function$;

CREATE OR REPLACE FUNCTION xml_le(xml, xml)
  RETURNS BOOLEAN
  LANGUAGE sql
  IMMUTABLE
  STRICT
AS $function$
SELECT $1::text <= $2::text;
$function$;

CREATE OR REPLACE FUNCTION xml_equals(xml, xml)
  RETURNS BOOLEAN
  LANGUAGE sql
  IMMUTABLE
  STRICT
AS $function$
SELECT $1::text = $2::text;
$function$;

CREATE OR REPLACE FUNCTION xml_gt(xml, xml)
  RETURNS BOOLEAN
  LANGUAGE sql
  IMMUTABLE
  STRICT
AS $function$
SELECT $1::text > $2::text;
$function$;

CREATE OR REPLACE FUNCTION xml_ge(xml, xml)
  RETURNS BOOLEAN
  LANGUAGE sql
  IMMUTABLE
  STRICT
AS $function$
SELECT $1::text >= $2::text;
$function$;

CREATE OR REPLACE FUNCTION xml_hash(xml)
  RETURNS INTEGER
  LANGUAGE sql
  IMMUTABLE
  STRICT
AS $function$
SELECT hashtext($1::text);
$function$;

--create operators less, less equals, equals, greater, greater equals

DROP OPERATOR IF EXISTS < (xml, xml) CASCADE;
CREATE OPERATOR < (
  PROCEDURE   = xml_lt,
  LEFTARG     = xml,
  RIGHTARG    = xml,
  COMMUTATOR  = >,
  NEGATOR     = >=,
  RESTRICT    = scalarltsel,
  JOIN        = scalarltjoinsel
);

DROP OPERATOR IF EXISTS <= (xml, xml) CASCADE;
CREATE OPERATOR <= (
  PROCEDURE   = xml_le,
  LEFTARG     = xml,
  RIGHTARG    = xml,
  COMMUTATOR  = >=,
  NEGATOR     = >,
  RESTRICT    = scalarlesel,
  JOIN        = scalarlejoinsel
);

DROP OPERATOR IF EXISTS = (xml, xml) CASCADE;
CREATE OPERATOR = (
  PROCEDURE   = xml_equals,
  LEFTARG     = xml,
  RIGHTARG    = xml,
  COMMUTATOR  = =,
  NEGATOR     = <>,
  RESTRICT    = eqsel,
  JOIN        = eqjoinsel,
  HASHES,
  MERGES
);

DROP OPERATOR IF EXISTS > (xml, xml) CASCADE;
CREATE OPERATOR > (
  PROCEDURE   = xml_gt,
  LEFTARG     = xml,
  RIGHTARG    = xml,
  COMMUTATOR  = <,
  NEGATOR     = <=,
  RESTRICT    = scalargtsel,
  JOIN        = scalargtjoinsel
);

DROP OPERATOR IF EXISTS >= (xml, xml) CASCADE;
CREATE OPERATOR >= (
  PROCEDURE   = xml_ge,
  LEFTARG     = xml,
  RIGHTARG    = xml,
  COMMUTATOR  = <=,
  NEGATOR     = <,
  RESTRICT    = scalargesel,
  JOIN        = scalargejoinsel
);

--create operator class hash, use only equals operator and xml_hash function
DROP OPERATOR CLASS IF EXISTS xml_ops USING hash CASCADE;
CREATE OPERATOR CLASS xml_ops
  DEFAULT
  FOR TYPE xml
  USING hash AS
  OPERATOR 1 =,
  FUNCTION 1 xml_hash(xml);

--create operator class btree, use less, less equals, equals, greater, greater equals operators and xml_cmp function
DROP OPERATOR CLASS IF EXISTS xml_ops USING btree CASCADE;
CREATE OPERATOR CLASS xml_ops
  DEFAULT
  FOR TYPE xml
  USING btree AS
   OPERATOR        1       < ,
   OPERATOR        2       <= ,
   OPERATOR        3       = ,
   OPERATOR        4       >= ,
   OPERATOR        5       > ,
   FUNCTION        1       xml_cmp(xml, xml);

--compare xml-text and text-xml

CREATE OR REPLACE FUNCTION xml_equals_text(xml, text)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
STRICT
AS $function$
SELECT $1::text = $2;
$function$;

CREATE OR REPLACE FUNCTION text_equals_xml(text, xml)
RETURNS boolean
LANGUAGE sql
IMMUTABLE
STRICT
AS $function$
SELECT $1 = $2::text;
$function$;

DROP OPERATOR IF EXISTS = (xml, text);
CREATE OPERATOR = (
  PROCEDURE  = xml_equals_text,
  LEFTARG    = xml,
  RIGHTARG   = text,
  COMMUTATOR = =
);

DROP OPERATOR IF EXISTS = (text, xml);
CREATE OPERATOR = (
  PROCEDURE  = text_equals_xml,
  LEFTARG    = text,
  RIGHTARG   = xml,
  COMMUTATOR = =
);