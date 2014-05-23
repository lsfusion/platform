
CREATE FUNCTION STRINGC(@prm1 nvarchar(max), @prm2 nvarchar(max) , @sep nvarchar(max)) RETURNS nvarchar(max) AS
BEGIN
        RETURN CASE WHEN @prm1 IS NOT NULL THEN @prm1 + (CASE WHEN @prm2 IS NOT NULL THEN @sep + @prm2 ELSE '' END) ELSE @prm2 END
END