-- пока не используются, вместо них используются multi-level props

DROP TYPE IF EXISTS winddistr CASCADE;

CREATE TYPE winddistr AS (
    this       double precision,
   running      double precision
);

CREATE OR REPLACE FUNCTION resultDistr(winddistr) RETURNS double precision AS
$$
    SELECT  $1.this;
$$ LANGUAGE 'sql' IMMUTABLE;

-- 2 parameter - distr, 3 parameter max, assert all are not nulls
CREATE OR REPLACE FUNCTION nextDistr(winddistr, double precision, double precision) RETURNS winddistr AS
$$
    DECLARE result winddistr;
    BEGIN
                IF $1.running >= $2 THEN
                               -- if no need
                               result.this = NULL;
                               result.running = $1.running;
                ELSE
                               IF $2-$1.running >= $3 THEN
                                               result.this = $3;
                                               result.running = $1.running + $3;
                               ELSE
                                               result.this = $2-$1.running;
                                               result.running = $2;
                               END IF;
                END IF;

                RETURN result;
    END
$$ LANGUAGE 'plpgsql' IMMUTABLE;

DROP AGGREGATE IF EXISTS DISTR_RESTRICT(double precision, double precision) CASCADE;

CREATE AGGREGATE DISTR_RESTRICT (double precision, double precision) (
    sfunc = nextDistr,
    stype = winddistr,
    finalfunc = resultDistr
);

-- 2 parameter - distr, 3 parameter max, 4 - totalsum, assert all are not nulls
CREATE OR REPLACE FUNCTION nextDistrOver(winddistr, double precision, double precision, double precision) RETURNS winddistr AS
$$
    DECLARE result winddistr;
    BEGIN
                IF $1.running >= $2 THEN
                               -- if no need
                               result.this = NULL;
                               result.running = $1.running;
                ELSE
                               IF $2-$1.running >= $3 THEN
                                               result.this = $3;
                                               result.running = $1.running + $3;

                                               IF result.running = $4 THEN -- all 've run out
                                                    result.this = result.this + $2 - result.running;
                                                    result.running = $2;
                                               END IF;
                               ELSE
                                               result.this = $2-$1.running;
                                               result.running = $2;
                               END IF;
                END IF;

                RETURN result;
    END
$$ LANGUAGE 'plpgsql' IMMUTABLE;

DROP AGGREGATE IF EXISTS DISTR_RESTRICT_OVER(double precision, double precision, double precision) CASCADE;

CREATE AGGREGATE DISTR_RESTRICT_OVER (double precision, double precision, double precision) (
    sfunc = nextDistrOver,
    stype = winddistr,
    finalfunc = resultDistr
);

DROP TYPE IF EXISTS propdistr CASCADE;

CREATE TYPE propdistr AS (
   this       double precision,
   running      double precision,
   trunning double precision
);

CREATE OR REPLACE FUNCTION resultProp(propdistr) RETURNS double precision AS
$$
    SELECT  $1.this;
$$ LANGUAGE 'sql' IMMUTABLE;

-- 2 parameter - distr, 3 parameter max, 4 - totalsum, assert all are not nulls
CREATE OR REPLACE FUNCTION nextCumProp(propdistr, double precision, double precision, double precision) RETURNS propdistr AS
$$
    DECLARE result propdistr;
    BEGIN
      result.this = round(CAST(($3 * ($2 - COALESCE($1.running, 0)) / ($4 - COALESCE($1.trunning, 0))) as numeric), 0);
      result.running = COALESCE($1.running, 0) + result.this;
      result.trunning = COALESCE($1.trunning, 0) + $3;

        RETURN result;
    END
$$ LANGUAGE 'plpgsql' IMMUTABLE;

DROP AGGREGATE IF EXISTS DISTR_CUM_PROPORTION(double precision, double precision, double precision) CASCADE;

CREATE AGGREGATE DISTR_CUM_PROPORTION (double precision, double precision, double precision) (
    sfunc = nextCumProp,
    stype = propdistr,
    finalfunc = resultProp
);

CREATE OR REPLACE FUNCTION array_setadd(anyarray, anyarray) RETURNS anyarray AS
$$
	DECLARE length1 int;
	DECLARE length2 int;
	DECLARE i int;
	DECLARE j int;
BEGIN
	length1 = array_upper($1,1);
	length2 = array_upper($2,1);
	IF length1 IS NULL OR length1 = 0 THEN
		RETURN $2;
	END IF;
	IF length2 IS NULL OR length2 = 0 THEN
		RETURN $1;
	END IF;

	j=1;
	i=1;
	IF length1 < length2 THEN
		WHILE i<=length1 LOOP
			IF NOT ($1[i] = ANY ($2)) THEN
				$2[length2+j] = $1[i];
				j=j+1;
			END IF;
			i=i+1;
		END LOOP;
		RETURN $2;
	ELSE
		WHILE i<=length2 LOOP
			IF NOT ($2[i] = ANY ($1)) THEN
				$1[length1+j] = $2[i];
				j=j+1;
			END IF;
			i=i+1;
		END LOOP;
		RETURN $1;
	END IF;
END
$$ LANGUAGE 'plpgsql' IMMUTABLE;

DROP AGGREGATE IF EXISTS AGGAR_SETADD(anyarray) CASCADE;

CREATE AGGREGATE AGGAR_SETADD (anyarray) (
    sfunc = array_setadd,
    stype = anyarray,
    initcond = '{}'
);

CREATE OR REPLACE FUNCTION notZero(anyelement) RETURNS anyelement AS
$$
    SELECT CASE WHEN $1 > -0.000005 AND $1 < 0.000005 THEN NULL ELSE $1 END;
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION MIN(anyelement, anyelement) RETURNS anyelement AS
$$
    SELECT CASE WHEN $1 IS NULL OR $1 > $2 THEN $2 ELSE $1 END;
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION MAX(anyelement, anyelement) RETURNS anyelement AS
$$
    SELECT CASE WHEN $1 IS NULL OR $1 < $2 THEN $2 ELSE $1 END;
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION STRINGC(varchar, varchar, varchar) RETURNS varchar AS
$$
        SELECT CASE WHEN $1 IS NOT NULL THEN $1 || (CASE WHEN $2 IS NOT NULL THEN $3 || $2 ELSE '' END) ELSE $2 END
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION jsonb_recursive_merge(a jsonb, b jsonb) RETURNS jsonb AS
$$
    SELECT jsonb_object_agg(
            COALESCE(ka, kb), CASE
                WHEN va ISNULL THEN vb
                WHEN vb ISNULL THEN va
                WHEN jsonb_typeof(va) <> 'object' or jsonb_typeof(vb) <> 'object' THEN vb
                ELSE jsonb_recursive_merge(va, vb) END)
    FROM jsonb_each(a) e1(ka, va)
             FULL JOIN jsonb_each(b) e2(kb, vb) ON ka = kb
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION notEmpty(jsonb) RETURNS jsonb AS
$$
SELECT CASE WHEN $1 = jsonb_build_object() THEN NULL ELSE $1 END;
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION json_recursive_merge(a json, b json) RETURNS json AS
$$
SELECT json_object_agg(
               COALESCE(ka, kb), CASE
                                     WHEN va ISNULL THEN vb
                                     WHEN vb ISNULL THEN va
                                     WHEN json_typeof(va) <> 'object' or json_typeof(vb) <> 'object' THEN vb
                                     ELSE json_recursive_merge(va, vb) END)
FROM json_each(a) e1(ka, va)
         FULL JOIN json_each(b) e2(kb, vb) ON ka = kb
    $$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION notEmpty(json) RETURNS json AS
$$
SELECT CASE WHEN $1::text = json_build_object()::text THEN NULL ELSE $1 END;
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION first_agg ( anyelement, anyelement )
RETURNS anyelement LANGUAGE sql IMMUTABLE AS $$
        SELECT $1;
$$;

DROP AGGREGATE IF EXISTS first(anyelement) CASCADE;

CREATE AGGREGATE first (
        sfunc    = first_agg,
        basetype = anyelement,
        stype    = anyelement
);

CREATE OR REPLACE FUNCTION last_agg ( anyelement, anyelement )
RETURNS anyelement LANGUAGE sql IMMUTABLE AS $$
        SELECT $2;
$$;

DROP AGGREGATE IF EXISTS last(anyelement) CASCADE;

CREATE AGGREGATE last (
        sfunc    = last_agg,
        basetype = anyelement,
        stype    = anyelement
);

DROP AGGREGATE IF EXISTS maxc(anyelement) CASCADE;

CREATE AGGREGATE maxc (
        sfunc    = MAX,
        basetype = anyelement,
        stype    = anyelement
);

DROP AGGREGATE IF EXISTS minc(anyelement) CASCADE;

CREATE AGGREGATE minc (
        sfunc    = MIN,
        basetype = anyelement,
        stype    = anyelement
);

CREATE OR REPLACE FUNCTION convert_to_integer(v_input text)
RETURNS INTEGER AS $$
DECLARE v_int_value INTEGER DEFAULT NULL;
BEGIN
    BEGIN
        v_int_value := v_input::INTEGER;
    EXCEPTION WHEN OTHERS THEN
        RETURN 0;
    END;
RETURN v_int_value;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION convert_to_numeric(v_input text)
RETURNS NUMERIC AS $$
DECLARE v_int_value NUMERIC DEFAULT NULL;
BEGIN
    BEGIN
        v_int_value := v_input::NUMERIC;
    EXCEPTION WHEN OTHERS THEN
        RETURN 0;
    END;
RETURN v_int_value;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION convert_to_numeric_null(v_input text)
RETURNS NUMERIC AS $$
DECLARE v_int_value NUMERIC DEFAULT NULL;
BEGIN
    BEGIN
        v_int_value := v_input::NUMERIC;
    EXCEPTION WHEN OTHERS THEN
        RETURN NULL;
    END;
RETURN v_int_value;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION convert_numeric_to_string(num IN NUMERIC)
RETURNS VARCHAR AS $$
  DECLARE
v_result_field VARCHAR(42);

BEGIN
IF (num = NULL) THEN RETURN NULL;
END IF;
v_result_field = TRIM(TO_CHAR(num, 'FM99999999999999999999D99999999999999999999'));
IF(SUBSTR(v_result_field, LENGTH(v_result_field), 1) = ',' OR SUBSTR(v_result_field, LENGTH(v_result_field), 1) = '.') THEN
  v_result_field = SUBSTR(v_result_field, 0, LENGTH(v_result_field));
END IF;
IF((SUBSTR(v_result_field, 1, 1) = ',' OR SUBSTR(v_result_field, 1, 1) = '.')) THEN
  v_result_field = '0' || v_result_field;
END IF;

RETURN v_result_field;
END;

$$ LANGUAGE plpgsql;
