using System;
using System.Diagnostics;
using CmnClsLib.Class;
using CmnClsLib.Module;
using FileCleaner.Class;

// 2026/08/08 Gemini 3.6 Flash (High) Review & Modified

namespace FileCleaner;

/// <summary>
/// FileCleaner アプリケーションのメインクラスです。
/// </summary>
public class Program
{
    /// <summary>
    /// アプリケーションのエントリーポイントです。
    /// コマンドライン引数を解析し、対象ディレクトリの不要ファイルクリーンアップを実行します。
    /// </summary>
    /// <param name="args">コマンドライン引数の配列</param>
    /// <returns>処理の終了コード（0: 正常終了, 1: 警告, 2: エラー）</returns>
    /// <example>
    /// <code>
    /// string[] args = ["-c", "FileClean.conf", "-v"];
    /// int returnCode = Program.Main(args);
    /// </code>
    /// </example>
    public static int Main(string[] args)
    {
        const string MethodName = "[Main()]";
        long startTimestamp = Stopwatch.GetTimestamp();
        DateTime startTime = DateTime.Now;

        ClsLogger logger = new();
        ClsAppArg appArg = new(logger);
        ClsFind fileFinder = new(logger, appArg);
        bool isValid = appArg.Parse(args);

        if (appArg.Verbose > -2)
        {
            logger.WriteLine(MdlConst.LVL_NONE, "");
            logger.WriteLine(MdlConst.LVL_NONE, $"===<<< [{appArg.ExeBaseName}] START : {MdlDate.GetFormattedDate(startTime, "yyyy/MM/dd HH:mm:ss")}>>>===");
        }

        if (isValid && appArg.Verbose > 3)
        {
            logger.WriteLine(MdlConst.LVL_NONE, "");
            logger.WriteLine(MdlConst.LVL_I, $"{MethodName} MachineName = {appArg.MachineName}");
            logger.WriteLine(MdlConst.LVL_I, $"{MethodName} Verbose     = {appArg.Verbose}");
            logger.WriteLine(MdlConst.LVL_I, $"{MethodName} NumOfDef    = {appArg.TargetList.Count}");
            logger.WriteLine(MdlConst.LVL_I, $"{MethodName} IsList      = {appArg.IsList}");
        }

        if (isValid && appArg.UsageFlag == ClsAppArg.USAGE_NONE)
        {
            if (appArg.Verbose > -2)
            {
                logger.WriteLine(MdlConst.LVL_NONE, "");
            }

            foreach (ClsBaseDir targetDirectory in appArg.TargetList)
            {
                if (appArg.IsList)
                {
                    targetDirectory.IsExec = false;
                }
                targetDirectory.IsDiff = appArg.IsDiff;

                string prefix = targetDirectory.IsNew ? "-" : "";
                string message = $"<<<=== {targetDirectory.Path} : {prefix}{targetDirectory.Term} ===>>>";

                if (targetDirectory.IsOk)
                {
                    if (appArg.Verbose > -2)
                    {
                        logger.WriteLine(MdlConst.LVL_NONE, message);
                    }

                    fileFinder.Execute(targetDirectory);

                    if (targetDirectory.ReturnCode == MdlConst.LVL_W && appArg.ReturnCode == MdlConst.LVL_I)
                    {
                        appArg.ReturnCode = MdlConst.LVL_W;
                    }
                    else if (targetDirectory.ReturnCode == MdlConst.LVL_E)
                    {
                        appArg.ReturnCode = MdlConst.LVL_E;
                    }

                    if (appArg.Verbose > -2)
                    {
                        logger.WriteLine(MdlConst.LVL_NONE, "");
                    }
                }
                else
                {
                    if (appArg.Verbose > 2)
                    {
                        logger.WriteLine(MdlConst.LVL_NONE, message);
                    }
                    if (appArg.Verbose > 4)
                    {
                        appArg.PrintDefinition(targetDirectory);
                    }
                    logger.WriteLine(MdlConst.LVL_E, "[SKIP] 定義に誤りがあります。");
                    logger.WriteLine(MdlConst.LVL_NONE, "");
                    appArg.ReturnCode = MdlConst.LVL_E;
                }
            }
        }
        else
        {
            appArg.ReturnCode = appArg.UsageFlag switch
            {
                ClsAppArg.USAGE_USAGE => ExecuteUsage(appArg),
                ClsAppArg.USAGE_SHOW_SAMPLE_CONFIG => ExecuteShowSampleConfig(appArg),
                _ => MdlConst.LVL_E
            };
        }

        if (appArg.Verbose > -2)
        {
            DateTime endTime = DateTime.Now;
            double elapsedSeconds = Stopwatch.GetElapsedTime(startTimestamp).TotalSeconds;
            logger.WriteLine(MdlConst.LVL_NONE, $"===<<< [{appArg.ExeBaseName}] EXIT ({appArg.ReturnCode}) : {MdlDate.GetFormattedDate(endTime, "yyyy/MM/dd HH:mm:ss")} : {elapsedSeconds:F3} sec>>>===");
        }

        return appArg.ReturnCode;
    }

    /// <summary>
    /// アプリケーションの使用方法（ヘルプ画面）を表示します。
    /// </summary>
    /// <param name="appArg">引数解析管理オブジェクト</param>
    /// <returns>警告レベルの戻り値コード (MdlConst.LVL_W)</returns>
    /// <example>
    /// <code>
    /// int returnCode = ExecuteUsage(appArg);
    /// </code>
    /// </example>
    private static int ExecuteUsage(ClsAppArg appArg)
    {
        appArg.ReturnCode = MdlConst.LVL_W;
        appArg.Usage();
        return appArg.ReturnCode;
    }

    /// <summary>
    /// サンプル設定ファイルの内容を表示します。
    /// </summary>
    /// <param name="appArg">引数解析管理オブジェクト</param>
    /// <returns>処理後の戻り値コード</returns>
    /// <example>
    /// <code>
    /// int returnCode = ExecuteShowSampleConfig(appArg);
    /// </code>
    /// </example>
    private static int ExecuteShowSampleConfig(ClsAppArg appArg)
    {
        appArg.ReturnCode = MdlConst.LVL_W;
        appArg.ShowSampleConfig();
        return appArg.ReturnCode;
    }
}

