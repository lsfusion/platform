CREATE FUNCTION jumpworkdays(@inputCountry int, @inputDate date, @inputQuantity int)
  RETURNS date AS
	BEGIN
	DECLARE @daysOff table(rowID int IDENTITY(1, 1), d date)
	DECLARE
		@quantity int = @inputQuantity,
		@bufferSize int = CASE WHEN @inputQuantity > 1 THEN @inputquantity / 2 ELSE 1 END,
		@counter int = 0,
		@date date = @inputDate,
		@add int,
		@lastOff date = @inputDate,
		@forward bit = CASE WHEN @inputQuantity > 0 THEN 1 ELSE 0 END
		WHILE 1=1
			BEGIN			
			INSERT INTO @daysOff(d) SELECT TOP (@bufferSize) key1 FROM Country_countrydate 
			WHERE key0 = @inputCountry
			AND Country_isdayoffcountrydate = 1 
			AND ((@forward = 1 AND DATEDIFF(DAY, key1, @lastOff) > 0) OR (@forward = 1 AND key1 < @lastOff)) 
			ORDER BY CASE WHEN @forward = 1 THEN key1 END ASC, CASE WHEN @forward = 0 THEN key1 END DESC

			DECLARE @dataCount int = (SELECT COUNT(1) FROM @daysOff)
			IF @dataCount = 0	--if there's no days off for the specified country OR date > last day off in the table
				RETURN CASE WHEN @inputQuantity = 0 THEN @date ELSE NULL END
	
			DECLARE @i int = 1
			WHILE @i<@dataCount
				BEGIN
				DECLARE @idaysOff date = (SELECT d FROM @daysOff WHERE rowID=@i) 
				set @add = CASE WHEN @forward = 1 THEN DATEDIFF(DAY, @idaysOff, @lastOff) - 1 ELSE DATEDIFF(DAY, @lastOff, @idaysOff) - 1 END
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