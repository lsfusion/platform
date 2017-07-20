
CREATE FUNCTION ${function.name}(${params.declare}) RETURNS 
@ret TABLE (${result.declare})
AS
BEGIN
    DECLARE @inserted INT;
    DECLARE @toggler BIT;

	DECLARE @${rec.table} AS TABLE (${result.declare});
	DECLARE @${rec.nexttable} AS TABLE (${result.declare});
	INSERT INTO @${rec.table} ${initial.select};
	SET @inserted = @@ROWCOUNT;

	WHILE @inserted > 0
	BEGIN
		IF @toggler = 1
		BEGIN
			INSERT INTO @ret SELECT * FROM @${rec.nexttable};
			INSERT INTO @${rec.table} ${step.nextselect};
			SET @inserted = @@ROWCOUNT;
			DELETE FROM @${rec.nexttable};
			SET @toggler = 0;
		END
		ELSE
		BEGIN
			INSERT INTO @ret SELECT * FROM @${rec.table};
			INSERT INTO @${rec.nexttable} ${step.select};
			SET @inserted = @@ROWCOUNT;
			DELETE FROM @${rec.table};
			SET @toggler = 1;
		END
	END

	RETURN
 END
