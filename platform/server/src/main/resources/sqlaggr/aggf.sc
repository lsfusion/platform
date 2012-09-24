
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

CREATE OR REPLACE FUNCTION recursion(rectable text, initial text, step text, VARIADIC params char[]) RETURNS SETOF RECORD AS
$$
    DECLARE inserted INT;
    DECLARE toggler BOOLEAN;
    DECLARE stepnext TEXT;
    DECLARE nextrectable TEXT;
    BEGIN

    nextrectable = 'nt' || rectable || 'it';
	stepnext = replace(step, rectable, nextrectable);
	EXECUTE 'CREATE TEMP TABLE ' || rectable || ' AS ' || initial USING params;
	GET DIAGNOSTICS inserted = ROW_COUNT;
	EXECUTE 'CREATE TEMP TABLE ' || nextrectable || ' AS SELECT * FROM ' || rectable || ' LIMIT 0';

	WHILE inserted > 0 LOOP
		IF toggler THEN
			RETURN QUERY EXECUTE 'SELECT * FROM ' || nextrectable;
			EXECUTE 'INSERT INTO ' || rectable || ' ' || stepnext USING params;
			GET DIAGNOSTICS inserted = ROW_COUNT;
			EXECUTE 'DELETE FROM ' || nextrectable;
			toggler = FALSE;
		ELSE
			RETURN QUERY EXECUTE 'SELECT * FROM ' || rectable;
			EXECUTE 'INSERT INTO ' || nextrectable || ' ' || step USING params;
			GET DIAGNOSTICS inserted = ROW_COUNT;
			EXECUTE 'DELETE FROM ' || rectable;
			toggler = TRUE;
		END IF;
	END LOOP;

	EXECUTE 'DROP TABLE ' || nextrectable;
	EXECUTE 'DROP TABLE ' || rectable;
    END
 $$ LANGUAGE 'plpgsql' VOLATILE COST 1000000;

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
    SELECT CASE WHEN $1 > -0.0005 AND $1 < 0.0005 THEN NULL ELSE $1 END;
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION MIN(anyelement, anyelement) RETURNS anyelement AS
$$
    SELECT CASE WHEN $1 > $2 THEN $2 ELSE $1 END;
$$ LANGUAGE 'sql' IMMUTABLE;

CREATE OR REPLACE FUNCTION MAX(anyelement, anyelement) RETURNS anyelement AS
$$
    SELECT CASE WHEN $1 < $2 THEN $2 ELSE $1 END;
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