CREATE OR REPLACE FUNCTION completeBarcode(text)
  RETURNS text AS
$BODY$
	DECLARE
		evenSum INTEGER := 0;
		oddSum INTEGER := 0;
		checkDigit INTEGER := 0;
		is12 BOOLEAN;
		sum INTEGER;
	BEGIN
		IF char_length($1) = 7 THEN
			is12 = FALSE;
		ELSEIF char_length($1) = 12 THEN
			is12 = TRUE;
		ELSE 
			RETURN $1;
		END IF;
		FOR i IN 1..char_length($1) LOOP
			IF mod(i, 2) = 0 THEN
				evenSum = evenSum + CAST(substr($1, i, 1) as INTEGER);
			ELSE
				oddSum = oddSum + CAST(substr($1, i, 1) as INTEGER);
			END IF;
		END LOOP;
		sum = CASE WHEN is12 THEN evenSum * 3 + oddSum ELSE evenSum + oddSum * 3 END;
		IF mod(sum, 10) != 0 THEN
			checkDigit = 10 - mod(sum, 10);
		END IF;
		RETURN $1 || checkDigit;
	END;
$BODY$
  LANGUAGE plpgsql VOLATILE STRICT;
