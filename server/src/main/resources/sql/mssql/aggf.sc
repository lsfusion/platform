
IF OBJECT_ID('STRINGC', 'FN') IS NOT NULL DROP FUNCTION STRINGC

GO

CREATE FUNCTION STRINGC(@prm1 nvarchar(max), @prm2 nvarchar(max) , @sep nvarchar(max)) RETURNS nvarchar(max) AS
BEGIN
        RETURN CASE WHEN @prm1 IS NOT NULL THEN @prm1 + (CASE WHEN @prm2 IS NOT NULL THEN @sep + @prm2 ELSE '' END) ELSE @prm2 END
END

GO

IF OBJECT_ID('convert_to_integer', 'FN') IS NOT NULL DROP FUNCTION convert_to_integer

GO

CREATE FUNCTION convert_to_integer(@prm1 nvarchar(max)) RETURNS numeric(38,19) AS
BEGIN
	declare @result numeric(38,19)
	set @prm1 = REPLACE(@prm1, ',', '.')
	IF ISNUMERIC(@prm1) > 0
		set @result = CAST(@prm1 AS numeric(38,19))
	ELSE 
		set @result = 0
	RETURN @result
END

GO

IF OBJECT_ID('convert_numeric_to_string', 'FN') IS NOT NULL DROP FUNCTION convert_numeric_to_string

GO

CREATE FUNCTION convert_numeric_to_string(@prm1 numeric(38,19)) RETURNS nvarchar(max) AS
BEGIN
	RETURN FORMAT(@prm1, 'g38')
END

GO

IF OBJECT_ID('completeBarcode', 'FN') IS NOT NULL DROP FUNCTION completeBarcode

GO

CREATE FUNCTION completeBarcode(@prm1 nvarchar(max)) RETURNS nvarchar(max) AS
BEGIN
	declare @evenSum int = 0, @oddSum int = 0, @checkDigit int = 0, @is12 bit, @sum int, @len int 
	set @len = len(@prm1)
	IF @len = 7
		BEGIN
		set @is12 = 0
		END
	ELSE 
		BEGIN
		IF @len = 12
			set @is12 = 1
		ELSE 
			RETURN @prm1
		END
	
	declare @i int = 1
	WHILE (@i <=@len)
		BEGIN
		IF @i%2 = 0
			set @evenSum = @evenSum + CAST(substring(@prm1, @i, 1) as int)
		ELSE
			set @oddSum = @oddSum + CAST(substring(@prm1, @i, 1) as int)	 
		set @i = (@i + 1)
		END
	set @sum = CASE WHEN @is12=1 THEN @evenSum * 3 + @oddSum ELSE @evenSum + @oddSum * 3 END;
	
	IF @sum%10 != 0
		set @checkDigit = 10 - @sum%10
	RETURN CONCAT(@prm1, CAST(@checkDigit AS VARCHAR(1)))
END

GO

IF OBJECT_ID('currentTransID', 'FN') IS NOT NULL DROP FUNCTION currentTransID

GO

CREATE FUNCTION currentTransID() RETURNS int AS
BEGIN
	RETURN(SELECT transaction_id FROM sys.dm_tran_current_transaction)
END
