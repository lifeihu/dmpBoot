package com.sw.bi.scheduler.background.taskexcuter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.sw.bi.scheduler.model.Task;
import com.sw.bi.scheduler.util.DateUtil;

public class ShellExcuter extends AbExcuter {

    public ShellExcuter(Task task, String logFolder) {
        super(task, logFolder);
    }

    @Override
    public boolean excuteCommand() throws InterruptedException, IOException {
        String path = currentJob.getProgramPath();

        Process process;
        BufferedReader ireader = null;
        BufferedReader ereader = null;
        String line = null;

        // 如果shell脚本  path不存在,在没有将错误信息记录到日志里. 需要将e.printStackTrace();记录到日志里
        try {
            process = programeRun(path);

            // 从process获取子进程日志输出

            // edit by whl 2016-04-28
            // 因为出现shell脚本一直卡着不退出的情况，但是shell里面的sql已经运行完了
            // 这样这个日志是一直不会打印出来的，所以采用重定向的方式，而不是代码去打印这个日志了
            /*ireader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			ereader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			while ((line = ireader.readLine()) != null) {
				log(line);
			}

			while ((line = ereader.readLine()) != null) {
				//log("ERROR: " + line);
				log(line);
			}*/

			/*InputStreamReader ir = new InputStreamReader(process.getInputStream());
			LineNumberReader input = new LineNumberReader(ir);
			String line;
			while ((line = input.readLine()) != null) {
				this.logFileWriter.write(line + "\r\n");
			}

			input.close();
			ir.close();*/

            process.waitFor();

            return process.exitValue() == 0;
			/*boolean result = process.exitValue() == 0;
			if (!result) {
				log("exit value: " + process.exitValue());

				log("error stream: " + IOUtils.toString(process.getErrorStream()));
			}

			return result;*/
        } catch (IOException e) {
            throw e;
        } catch (InterruptedException e) {
            throw e;
        } finally {
            if (ireader != null) {
                ireader.close();
            }

            if (ereader != null) {
                ereader.close();
            }
        }
    }

    //默认给shell脚本2个参数  $1 $2 $3  $1是天.   $2是小时		$3是月
    private Process programeRun(String path) throws IOException {
        // Map<String, String> paramsMapping = Parameters.getRunTimeParamter(currentTask);

        //java调用shell脚本并且传入了两个参数. $1表示yyyyMMdd  $2表示yyyyMMddHH  $3表示yyyyMM  $4表示settingTime(yyyy-MM-dd HH:mm:ss)  $5表示作业ID
        String[] commands = new String[]{"/bin/bash", "/home/tools/shell-exec.sh", path, this.getLogPathName(), runtimeParamters.get("${date_desc}"), runtimeParamters.get("${hour_desc}"), runtimeParamters.get("${month_desc}"),
                DateUtil.format(currentTask.getSettingTime(), "yyyy-MM-dd HH:mm:ss"), String.valueOf(currentTask.getJobId())};
        Process process = Runtime.getRuntime().exec(commands);

        return process;
    }

    public static void main(String[] args) {
        String path = "/home/whl/start";

//        String com = "/bin/bash /home/whl/start 2012 2012 2012 2012 12 123  > /home/whl/log 2>&1";
//        String com = "/bin/bash /home/whl/shellexec.sh /home/whl/start /home/whl/log 1 2 3 4";

//        String[] commands = new String[] { "/bin/bash", path, "2012", "2012", "2012",
//                "2012 12", "123", " > /home/whl/log 2>&1 "};

        String[] commands = new String[]{"/bin/bash", "/home/whl/shell-exec.sh", "/home/whl/start", "/home/whl/log", "1", "2", "3 4"};
        try {
            System.out.println();
            Process process = Runtime.getRuntime().exec(commands);
            process.waitFor();
            System.out.println(process.exitValue());

            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();

            br = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
