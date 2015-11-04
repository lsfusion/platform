CREATE OR REPLACE FUNCTION ${function.name}(rectable text, initial text, step text, stepsm text, smlimit int ${params.declare}) RETURNS SETOF RECORD AS
$$
    DECLARE inserted INT;
    DECLARE smtoggler BOOLEAN;
    DECLARE toggler BOOLEAN;
    DECLARE stepnext TEXT;
    DECLARE stepsmnext TEXT;
    DECLARE nextrectable TEXT;
    DECLARE itstep TEXT;
    DECLARE ittable TEXT;
    DECLARE itntable TEXT;
    BEGIN

    nextrectable = 'nt' || rectable || 'it';
	stepnext = replace(step, rectable, nextrectable);
	stepsmnext = replace(stepsm, rectable, nextrectable);
	EXECUTE 'CREATE TEMP TABLE ' || rectable || ' AS ' || initial ${params.usage};
	GET DIAGNOSTICS inserted = ROW_COUNT;
	EXECUTE 'CREATE TEMP TABLE ' || nextrectable || ' AS SELECT * FROM ' || rectable || ' LIMIT 0';

	WHILE inserted > 0 LOOP
		IF toggler THEN
			ittable = nextrectable;
			itntable = rectable;
			IF inserted < smlimit THEN
				itstep = stepsmnext;
			ELSE
				itstep = stepnext;
			END IF;
			toggler = FALSE;
		ELSE
			ittable = rectable;
			itntable = nextrectable;
			IF inserted < smlimit THEN
				itstep = stepsm;
			ELSE
				itstep = step;
			END IF;
			toggler = TRUE;
		END IF;

		RETURN QUERY EXECUTE 'SELECT * FROM ' || ittable;
		EXECUTE 'INSERT INTO ' || itntable || ' ' || itstep ${params.usage};
		GET DIAGNOSTICS inserted = ROW_COUNT;
		EXECUTE 'DELETE FROM ' || ittable;
	END LOOP;

	EXECUTE 'DROP TABLE ' || nextrectable;
	EXECUTE 'DROP TABLE ' || rectable;
    END
 $$ LANGUAGE 'plpgsql' VOLATILE COST 1000000;
