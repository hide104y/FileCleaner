package tool;

import java.time.LocalDateTime;
import java.util.Locale;
import tool.cmnclslib.cls.ClsLogger;
import tool.cmnclslib.mdl.MdlConst;
import tool.cmnclslib.mdl.MdlDate;

/**
 * FileCleaner アプリケーションのメインクラスです。
 */
public class FileCleaner {

    private FileCleaner() {
        // インスタンス化防止
    }

    /**
     * アプリケーションのエントリーポイントです。
     *
     * @param args コマンドライン引数の配列
     */
    public static void main(String[] args) {
        int returnCode = mainProcess(args);
        if (returnCode != MdlConst.LVL_I) {
            System.exit(returnCode);
        }
    }

    /**
     * コマンドライン引数を解析し、対象ディレクトリの不要ファイルクリーンアップを実行します。
     *
     * @param args コマンドライン引数の配列
     * @return 処理の終了コード（0: 正常終了, 10: 警告, 20: エラー）
     */
    public static int mainProcess(String[] args) {
        final String methodName = "[mainProcess()]";
        long startTimestamp = System.nanoTime();
        LocalDateTime startTime = LocalDateTime.now();

        ClsLogger logger = new ClsLogger();
        ClsAppArg appArg = new ClsAppArg(logger);
        ClsFind fileFinder = new ClsFind(logger, appArg);
        boolean isValid = appArg.parse(args);

        if (appArg.getVerbose() > -2) {
            logger.writeLine(MdlConst.LVL_NONE, "");
            logger.writeLine(MdlConst.LVL_NONE, "===<<< [" + appArg.getExeBaseName() + "] START : " + MdlDate.getFormattedDate(startTime, "yyyy/MM/dd HH:mm:ss") + ">>>===");
        }

        if (isValid && appArg.getVerbose() > 3) {
            logger.writeLine(MdlConst.LVL_NONE, "");
            logger.writeLine(MdlConst.LVL_I, methodName + " MachineName = " + appArg.getMachineName());
            logger.writeLine(MdlConst.LVL_I, methodName + " Verbose     = " + appArg.getVerbose());
            logger.writeLine(MdlConst.LVL_I, methodName + " NumOfDef    = " + appArg.getTargetList().size());
            logger.writeLine(MdlConst.LVL_I, methodName + " IsList      = " + appArg.isList());
        }

        if (isValid && appArg.getUsageFlag() == ClsAppArg.USAGE_NONE) {
            if (appArg.getVerbose() > -2) {
                logger.writeLine(MdlConst.LVL_NONE, "");
            }

            for (ClsBaseDir targetDirectory : appArg.getTargetList()) {
                if (appArg.isList()) {
                    targetDirectory.setExec(false);
                }
                targetDirectory.setDiff(appArg.isDiff());

                String prefix = targetDirectory.isNew() ? "-" : "";
                String message = "<<<=== " + targetDirectory.getPath() + " : " + prefix + targetDirectory.getTerm() + " ===>>>";

                if (targetDirectory.isOk()) {
                    if (appArg.getVerbose() > -2) {
                        logger.writeLine(MdlConst.LVL_NONE, message);
                    }

                    fileFinder.execute(targetDirectory);

                    if (targetDirectory.getReturnCode() == MdlConst.LVL_W && appArg.getReturnCode() == MdlConst.LVL_I) {
                        appArg.setReturnCode(MdlConst.LVL_W);
                    } else if (targetDirectory.getReturnCode() == MdlConst.LVL_E) {
                        appArg.setReturnCode(MdlConst.LVL_E);
                    }

                    if (appArg.getVerbose() > -2) {
                        logger.writeLine(MdlConst.LVL_NONE, "");
                    }
                } else {
                    if (appArg.getVerbose() > 2) {
                        logger.writeLine(MdlConst.LVL_NONE, message);
                    }
                    if (appArg.getVerbose() > 4) {
                        appArg.printDefinition(targetDirectory);
                    }
                    logger.writeLine(MdlConst.LVL_E, "[SKIP] 定義に誤りがあります。");
                    logger.writeLine(MdlConst.LVL_NONE, "");
                    appArg.setReturnCode(MdlConst.LVL_E);
                }
            }
        } else {
            switch (appArg.getUsageFlag()) {
                case ClsAppArg.USAGE_USAGE:
                    appArg.setReturnCode(executeUsage(appArg));
                    break;
                case ClsAppArg.USAGE_SHOW_SAMPLE_CONFIG:
                    appArg.setReturnCode(executeShowSampleConfig(appArg));
                    break;
                default:
                    appArg.setReturnCode(MdlConst.LVL_E);
                    break;
            }
        }

        if (appArg.getVerbose() > -2) {
            LocalDateTime endTime = LocalDateTime.now();
            double elapsedSeconds = (System.nanoTime() - startTimestamp) / 1_000_000_000.0;
            logger.writeLine(MdlConst.LVL_NONE, String.format(Locale.ROOT, "===<<< [%s] EXIT (%d) : %s : %.3f sec>>>===",
                    appArg.getExeBaseName(), appArg.getReturnCode(), MdlDate.getFormattedDate(endTime, "yyyy/MM/dd HH:mm:ss"), elapsedSeconds));
        }

        return appArg.getReturnCode();
    }

    /**
     * アプリケーションの使用方法（ヘルプ画面）を表示します。
     *
     * @param appArg 引数解析管理オブジェクト
     * @return 警告レベルの戻り値コード (MdlConst.LVL_W)
     */
    private static int executeUsage(ClsAppArg appArg) {
        appArg.setReturnCode(MdlConst.LVL_W);
        appArg.usage();
        return appArg.getReturnCode();
    }

    /**
     * サンプル設定ファイルの内容を表示します。
     *
     * @param appArg 引数解析管理オブジェクト
     * @return 処理後の戻り値コード
     */
    private static int executeShowSampleConfig(ClsAppArg appArg) {
        appArg.setReturnCode(MdlConst.LVL_W);
        appArg.showSampleConfig();
        return appArg.getReturnCode();
    }
}
