
-- пока не используются, вместо них используются multi-level props

DROP TYPE IF EXISTS winddistr CASCADE;

CREATE TYPE winddistr AS (
    this       double precision,
   running      double precision
);

CREATE OR REPLACE FUNCTION resultDistr(winddistr) RETURNS double precision AS
$$
    SELECT  $1.this;
$$ LANGUAGE 'sql';

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
$$ LANGUAGE 'plpgsql';

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
$$ LANGUAGE 'plpgsql';

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
$$ LANGUAGE 'sql';

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
$$ LANGUAGE 'plpgsql';

DROP AGGREGATE IF EXISTS DISTR_CUM_PROPORTION(double precision, double precision, double precision) CASCADE;

CREATE AGGREGATE DISTR_CUM_PROPORTION (double precision, double precision, double precision) (
    sfunc = nextCumProp,
    stype = propdistr,
    finalfunc = resultProp
);
