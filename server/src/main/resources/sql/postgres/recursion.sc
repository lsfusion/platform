CREATE OR REPLACE FUNCTION ${function.name}(rectable text, initial text, step text ${params.declare}) RETURNS SETOF RECORD AS
$$
    DECLARE inserted INT;
    DECLARE toggler BOOLEAN;
    DECLARE stepnext TEXT;
    DECLARE nextrectable TEXT;
    BEGIN

    nextrectable = 'nt' || rectable || 'it';
	stepnext = replace(step, rectable, nextrectable);
	EXECUTE 'CREATE TEMP TABLE ' || rectable || ' AS ' || initial ${params.usage};
	GET DIAGNOSTICS inserted = ROW_COUNT;
	EXECUTE 'CREATE TEMP TABLE ' || nextrectable || ' AS SELECT * FROM ' || rectable || ' LIMIT 0';

	WHILE inserted > 0 LOOP
		IF toggler THEN
			RETURN QUERY EXECUTE 'SELECT * FROM ' || nextrectable;
			EXECUTE 'INSERT INTO ' || rectable || ' ' || stepnext ${params.usage};
			GET DIAGNOSTICS inserted = ROW_COUNT;
			EXECUTE 'DELETE FROM ' || nextrectable;
			toggler = FALSE;
		ELSE
			RETURN QUERY EXECUTE 'SELECT * FROM ' || rectable;
			EXECUTE 'INSERT INTO ' || nextrectable || ' ' || step ${params.usage};
			GET DIAGNOSTICS inserted = ROW_COUNT;
			EXECUTE 'DELETE FROM ' || rectable;
			toggler = TRUE;
		END IF;
	END LOOP;

	EXECUTE 'DROP TABLE ' || nextrectable;
	EXECUTE 'DROP TABLE ' || rectable;
    END
 $$ LANGUAGE 'plpgsql' VOLATILE COST 1000000;
