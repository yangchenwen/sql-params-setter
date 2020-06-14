# sql-params-setter

      A simple Intellji IDEA plugin that helps you to extract a executable sql from mybatis logs like below:
          ==> Preparing: select * from table where name = ?
          ==> Parameters: Tom(String)
      After selecting these two lines of logs above, you can right click your mouse
      and select "SQL Params Setter" in the popup menu, then the result executable sql,
      i.e. "select * from table where name = 'Tom'" will be copied to your clipboard.

      Note:
        The selected area should contain both keyword [Preparing:] in the 1st line and keyword [Parameters:] in the 2nd line.
