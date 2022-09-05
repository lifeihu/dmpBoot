package com.sw.bi.scheduler.util;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 文件读取类
 * 
 * @author  mshifeng
 * @version  [1.0, 2019-1-9]
 * 
 */
public class ReadFile
{
    //Main函数，程序入口
    public static void main(String[] args)
    {
        //调用读取方法，定义文件以及读取行数
        readLastNLine(new File("f:/4072.log"),8L);
    }
    
    /**
     * 读取文件最后N行 
     * 
     * 根据换行符判断当前的行数，
     * 使用统计来判断当前读取第N行
     * 
     * PS:输出的List是倒叙，需要对List反转输出
     * 
     * @param file 待文件
     * @param numRead 读取的行数
     * @return List<String>
     */
    public static List<String> readLastNLine(File file, long numRead)
    {
    	// 定义结果集
        List<String> result = new ArrayList<String>();
        //行数统计
        long count = 0;
        
        // 排除不可读状态״̬
        if (!file.exists() || file.isDirectory() || !file.canRead())
        {
            return null;
        }
        
        // 使用随机读取
        RandomAccessFile fileRead = null;
        try
        {
        	//使用读模式
            fileRead = new RandomAccessFile(file, "r");
            //读取文件长度
            long length = fileRead.length();
            //如果是0，代表是空文件，直接返回空结果
            if (length == 0L)
            {
                return result;
            }
            else
            {
                //初始化游标
                long pos = length - 1;
                while (pos > 0)
                {
                    pos--;
                    //开始读取
                    fileRead.seek(pos);
                    //如果读取到\n代表是读取到一行
                    if (fileRead.readByte() == '\n')
                    {
                    	//使用readLine获取当前行
                        String line = fileRead.readLine();
                        //保存结果
                        result.add(line);
                        
                        //打印当前行
                        System.out.println(line);
                        
                        //行数统计，如果到达了numRead指定的行数，就跳出循环
                        count++;
                        if (count == numRead)
                        {
                            break;
                        }
                    }
                }
                if (pos == 0)
                {
                    fileRead.seek(0);
                    result.add(fileRead.readLine());
                }
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (fileRead != null)
            {
                try
                {
                    //关闭资源
                    fileRead.close();
                }
                catch (Exception e)
                {
                }
            }
        }
        
        return result;
    }
}
