对于sqlserver的dataPath的特别说明

<dataPath path="INSERT INTO sqlserver_test.dbo.test (a,b,c,d)  values(?,?,?,?)"/>

如上面sqlserver_test.dbo.test 与 (a,b,c,d) 之间要有一个空格,不然会同步失败。