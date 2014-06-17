
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

GO

IF OBJECT_ID('jumpWorkdays', 'FN') IS NOT NULL DROP FUNCTION jumpWorkdays

GO

CREATE FUNCTION jumpworkdays(@inputCountry int, @inputDate date, @inputQuantity int)
  RETURNS date AS
	BEGIN
	DECLARE @daysOff table(d date, rn int)
	DECLARE
		@quantity int = @inputQuantity,
		@bufferSize int = CASE WHEN @inputQuantity > 1 THEN @inputquantity / 2 ELSE 1 END,
		@counter int = 0,
		@date date = @inputDate,
		@add int,
		@lasdt date,
		@lastOff date = @inputDate,
		@forward bit = CASE WHEN @inputQuantity > 0 THEN 1 ELSE 0 END

		WHILE 1=1
			BEGIN			
			DELETE FROM @daysOff
			INSERT INTO @daysOff(d, rn) SELECT TOP (@bufferSize) key1, ROW_NUMBER() OVER (ORDER BY CASE WHEN @forward = 1 THEN key1 END ASC, CASE WHEN @forward = 0 THEN key1 END DESC) AS rn FROM Country_countrydate 
			WHERE key0 = @inputCountry
			AND Country_isdayoffcountrydate = 1 
			AND ((@forward = 1 AND key1 > @lastOff) OR (@forward = 0 AND key1 < @lastOff)) 
			ORDER BY CASE WHEN @forward = 1 THEN key1 END ASC, CASE WHEN @forward = 0 THEN key1 END DESC

			DECLARE @dataCount int = (SELECT COUNT(1) FROM @daysOff)
			IF @dataCount = 0	--if there's no days off for the specified country OR date > last day off in the table
				RETURN CASE WHEN @inputQuantity = 0 THEN @date ELSE NULL END

			DECLARE @i int = 1
			WHILE @i<=@dataCount
				BEGIN
				DECLARE @idaysOff date = (SELECT d FROM @daysOff WHERE rn=@i) 
				set @add = CASE WHEN @forward = 1 THEN DATEDIFF(DAY, @lastOff, @idaysOff) - 1 ELSE DATEDIFF(DAY, @idaysOff, @lastOff) - 1 END
				set @add = CASE WHEN (@quantity - @counter) >= @add THEN @add ELSE (@quantity - @counter) END;
				set @date = CASE WHEN @forward = 1 THEN DATEADD(DAY, @add, @lastOff) ELSE DATEADD(DAY, -@add, @lastOff) END;
				set @counter = @counter + @add
				IF @counter = @quantity
					RETURN @date
				set @lastOff = @idaysOff
				set @i = (@i + 1)
				END
			END
		RETURN @date
	END

GO

IF OBJECT_ID('firstWord', 'FN') IS NOT NULL DROP FUNCTION firstWord

GO

CREATE FUNCTION firstWord(@input nvarchar(max)) RETURNS nvarchar(max) AS
BEGIN
 IF CHARINDEX('-', @input) > 0 
    RETURN SUBSTRING(@input, 0, CHARINDEX('-', @input)) 
 RETURN @input
END

GO

IF OBJECT_ID('secondWord', 'FN') IS NOT NULL DROP FUNCTION secondWord

GO

CREATE FUNCTION secondWord(@input nvarchar(max)) RETURNS nvarchar(max) AS
BEGIN
 IF CHARINDEX('-', @input) = 0 
    RETURN NULL
 DECLARE @secondPart nvarchar(max) = SUBSTRING(@input, CHARINDEX('-', @in put) + 1, LEN(@input))
  
 IF CHARINDEX('-', @secondPart) > 0 
    RETURN SUBSTRING(@secondPart, 0, CHARINDEX('-', @secondPart))
     
 RETURN @secondPart
END

GO

IF OBJECT_ID('lastWord', 'FN') IS NOT NULL DROP FUNCTION lastWord

GO

CREATE FUNCTION lastWord(@input nvarchar(max)) RETURNS nvarchar(max) AS
BEGIN
 IF CHARINDEX('-', @input) > 0 
    RETURN SUBSTRING(@input, LEN(@input) - CHARINDEX('-', REVERSE(@input)) + 2, LEN(@input)) 
    
 RETURN @input
END