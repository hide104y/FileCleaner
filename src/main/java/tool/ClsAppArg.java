package tool;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import tool.cmnclslib.cls.ClsCmmnArgs;
import tool.cmnclslib.cls.ClsLogger;
import tool.cmnclslib.mdl.MdlArg;
import tool.cmnclslib.mdl.MdlConst;
import tool.cmnclslib.mdl.MdlDate;
import tool.cmnclslib.mdl.MdlFile;
import tool.cmnclslib.mdl.MdlUtil;

/**
 * コマンドライン引数の解析およびアプリケーションパラメータの管理を行うクラスです。
 */
public class ClsAppArg {

    public static final int USAGE_NONE = 0;
    public static final int USAGE_USAGE = 1;
    public static final int USAGE_SHOW_SAMPLE_CONFIG = 2;

    private final ClsLogger logger;
    private final ClsCmmnArgs cmmnArgs;
    private final ClsCfgFile configFile;
    private ClsBaseDir baseDir = new ClsBaseDir();
    private String exeDir = "";
    private String exeBaseName = "";
    private long pid = 0;
    private int verbose = 0;
    private int returnCode = MdlConst.LVL_I;
    private int usageFlag = 0;
    private boolean isStackTrace = false;
    private int timeout = 3600;
    private boolean isList = true;
    private boolean isDiff = true;
    private List<ClsBaseDir> targetList = new ArrayList<>();
    private String delimiter = ";|";
    private String configPath = "";
    private boolean isExample = false;
    private int sortType = MdlFile.SORT_BY_NONE;
    private boolean isAscending = true;
    private boolean isShowDirList = false;
    private boolean isShowFileList = false;

    /**
     * {@link ClsAppArg} クラスの新しいインスタンスを初期化します。
     *
     * @param logger ログ出力に使用する {@link ClsLogger} オブジェクト
     */
    public ClsAppArg(ClsLogger logger) {
        if (logger == null) {
            throw new NullPointerException("logger must not be null");
        }
        this.logger = logger;

        this.cmmnArgs = new ClsCmmnArgs(logger);
        this.cmmnArgs.getModuleInfo();
        this.exeDir = this.cmmnArgs.getExeDir();
        this.exeBaseName = this.cmmnArgs.getExeBaseName();
        this.pid = this.cmmnArgs.getPid();

        this.configFile = new ClsCfgFile(logger);
        this.configFile.setTargetList(this.targetList);
    }

    /**
     * コマンドライン引数の配列を解析し、アプリケーションの設定パラメータを取得・保持します。
     *
     * @param args コマンドライン引数の配列
     * @return 引数の解析およびパラメータ取得が正常に完了した場合は {@code true}、失敗した場合は {@code false}
     */
    public boolean parse(String[] args) {
        if (args == null) {
            throw new NullPointerException("args must not be null");
        }

        final String methodName = "[ClsAppArg.parse()]";
        boolean isOk = true;
        String stringValue = "";
        boolean hasValue = false;

        // -----------------------------------------------------------------
        // ClsCmmnParams処理
        // -----------------------------------------------------------------
        Map<String, String> namedArgs = MdlArg.getNamedArgs(args);
        cmmnArgs.setNamedArgs(namedArgs);
        isOk = cmmnArgs.getCommonArgs();

        // -----------------------------------------------------------------
        // ClsCmmnParams引数取得：ETC
        // -----------------------------------------------------------------
        usageFlag = cmmnArgs.isUsage() ? USAGE_USAGE : USAGE_NONE;
        verbose = cmmnArgs.getVerbose();
        isStackTrace = cmmnArgs.isStackTrace();
        timeout = cmmnArgs.getTimeout();

        configFile.setVerbose(verbose);

        // -----------------------------------------------------------------
        // Option：
        // -----------------------------------------------------------------
        for (String key : List.of("help-conf", "show-sample-config")) {
            if (MdlArg.containsKey(namedArgs, key)) {
                usageFlag = USAGE_SHOW_SAMPLE_CONFIG;
                return true;
            }
        }

        for (String key : List.of("h-sample")) {
            if (MdlArg.containsKey(namedArgs, key)) {
                usageFlag = USAGE_USAGE;
                isExample = true;
            }
        }

        // 削除フラグ
        for (String key : List.of("clean", "exec", "list")) {
            if (MdlArg.containsKey(namedArgs, key)) {
                switch (key) {
                    case "clean":
                    case "exec":
                        isList = false;
                        break;
                    case "list":
                        isList = true;
                        break;
                    default:
                        break;
                }
            }
        }

        // 設定ファイル
        hasValue = false;
        boolean isHitArg = false;
        for (String key : List.of("c", "conf", "cnf")) {
            if (MdlArg.containsKey(namedArgs, key)) {
                isHitArg = true;
                stringValue = MdlArg.getValue(namedArgs, key);
                if (stringValue != null && !stringValue.isBlank()) {
                    configPath = cmmnArgs.getPathParam(key, MdlFile.PATH_IS_FILE, false);
                    if (configPath != null && !configPath.isBlank()) {
                        hasValue = true;
                    }
                    break;
                }
            }
        }
        if (isHitArg && !hasValue) {
            isOk = false;
            consoleWriteLine(MdlConst.LVL_NONE, "INVALID ARGUMENT : -c|-conf path_to_conf");
        }

        for (String key : List.of("delimiter")) {
            if (MdlArg.containsKey(namedArgs, key)) {
                stringValue = MdlArg.getValue(namedArgs, key);
                if (stringValue != null && !stringValue.isBlank()) {
                    delimiter = stringValue.strip();
                    break;
                }
            }
        }

        // -----------------------------------------------------------------
        // 設定ファイルが指定されなかった場合
        // -----------------------------------------------------------------
        if (configPath == null || configPath.isEmpty()) {
            ClsBaseDir baseDir = new ClsBaseDir();
            baseDir.setVerbose(verbose);
            baseDir.setTimeout(timeout);

            // パスが存在しない場合の終了コード
            if (MdlArg.containsKey(namedArgs, "na-mkdir")) {
                baseDir.setMkRmBaseDir(true);
            }
            if (MdlArg.containsKey(namedArgs, "na-i")) {
                baseDir.setNaRetCode(MdlConst.LVL_I);
            }
            if (MdlArg.containsKey(namedArgs, "na-w")) {
                baseDir.setNaRetCode(MdlConst.LVL_W);
            }
            if (MdlArg.containsKey(namedArgs, "na-e")) {
                baseDir.setNaRetCode(MdlConst.LVL_E);
            }

            // 削除失敗時の終了コード
            if (MdlArg.containsKey(namedArgs, "err-i")) {
                baseDir.setErrRetCode(MdlConst.LVL_I);
            }
            if (MdlArg.containsKey(namedArgs, "err-w")) {
                baseDir.setErrRetCode(MdlConst.LVL_W);
            }
            if (MdlArg.containsKey(namedArgs, "err-e")) {
                baseDir.setErrRetCode(MdlConst.LVL_E);
            }

            // ACTION
            for (String key : List.of("a", "action")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    stringValue = MdlArg.getValue(namedArgs, key);
                    if (stringValue != null && !stringValue.isBlank()) {
                        switch (stringValue.toLowerCase(Locale.ROOT)) {
                            case "gendel":
                            case "gen":
                            case "g":
                                if (verbose > 4 && logger != null) {
                                    logger.writeLine(MdlConst.LVL_NONE, "ARG -" + key + " " + stringValue);
                                }
                                baseDir.setActionCode(ClsBaseDir.ACTION_GEN_DELETE);
                                baseDir.setMaxDepth(0);
                                break;
                            default:
                                break;
                        }
                    }
                }
            }

            // パスの取得
            for (String key : List.of("path", "f")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    stringValue = MdlArg.getFullPath(namedArgs, key);
                    if (stringValue != null && !stringValue.isBlank()) {
                        stringValue = MdlFile.removeTrailingPathSeparator(MdlFile.getAbsolutePath(stringValue));
                        baseDir.setPath(stringValue);

                        if (MdlFile.getDirectoryPath(baseDir.getPath()).isEmpty()) {
                            baseDir.setPath(baseDir.getPath() + "\\.");
                        }
                        switch (MdlFile.getPathType(stringValue)) {
                            case MdlFile.PATH_IS_DIRECTORY:
                                baseDir.setBaseDir(true);
                                break;
                            case MdlFile.PATH_IS_FILE:
                                baseDir.setBaseDir(false);
                                break;
                            default:
                                boolean isExist = false;
                                if (baseDir.isMkRmBaseDir()) {
                                    if (MdlFile.createDirectory(stringValue) <= MdlFile.OK_MKDIR_HANTEI) {
                                        baseDir.setBaseDir(true);
                                        isExist = true;
                                    } else if (logger != null) {
                                        logger.writeLine(baseDir.getNaRetCode(), methodName + "FAILED TO MKDIR : " + stringValue);
                                    }
                                }
                                if (!isExist) {
                                    isOk = false;
                                    baseDir.setExec(false);
                                    baseDir.setReturnCode(baseDir.getNaRetCode());
                                    if (logger != null) {
                                        logger.writeLine(baseDir.getNaRetCode(), methodName + "NO SUCH A FILE OR DIRECTORY : " + stringValue);
                                    }
                                }
                                break;
                        }
                        break;
                    }
                }
            }
            if (isOk && baseDir.getPath().isEmpty()) {
                baseDir.setReturnCode(baseDir.getNaRetCode());
                consoleWriteLine(MdlConst.LVL_NONE, "INVALID ARGUMENT : -path path_to_target");
                isOk = false;
            }

            // 削除対象判別経過日数の取得
            for (String key : List.of("term", "days")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    switch (key) {
                        case "term":
                            baseDir.setTerm(true);
                            break;
                        case "days":
                            baseDir.setTerm(true);
                            baseDir.setDays(true);
                            break;
                        default:
                            break;
                    }
                    stringValue = MdlArg.getValue(namedArgs, key);
                    if (stringValue != null && !stringValue.isBlank()) {
                        double parsedDouble = MdlUtil.parseDouble(stringValue, MdlConst.DBL_NULL);
                        if (parsedDouble != MdlConst.DBL_NULL) {
                            baseDir.setTerm(parsedDouble);
                        }
                    }
                }
            }

            // 削除対象は判別経過日数より新しい更新日付とするか否か
            for (String key : List.of("new")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    baseDir.setTerm(true);
                    baseDir.setNew(true);
                }
            }

            // 日付閾値の取得
            long termNanos = (long) (baseDir.getTerm() * 86400_000_000_000L);
            if (baseDir.isDays()) {
                baseDir.setThresholdDate(LocalDate.now().atStartOfDay().minusNanos(termNanos));
            } else {
                baseDir.setThresholdDate(LocalDateTime.now().minusNanos(termNanos));
            }

            // 日付閾値の指定 (-date)
            for (String key : List.of("date")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    stringValue = MdlArg.getValue(namedArgs, key);
                    if (stringValue != null && !stringValue.isBlank()) {
                        LocalDateTime parsedDateTime = MdlDate.parseDateTime(stringValue);
                        if (parsedDateTime != null) {
                            baseDir.setTerm(true);
                            baseDir.setThresholdDate(parsedDateTime);
                        }
                    }
                }
            }

            // 削除対象ディレクトリ階層(MIN)
            for (String key : List.of("min")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    stringValue = MdlArg.getValue(namedArgs, key);
                    if (stringValue != null && !stringValue.isBlank()) {
                        baseDir.setMinDepth(MdlUtil.parseLong(stringValue, 0));
                        break;
                    }
                }
            }

            // 削除対象ディレクトリ階層(MAX)
            for (String key : List.of("max")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    stringValue = MdlArg.getValue(namedArgs, key);
                    if (stringValue != null && !stringValue.isBlank()) {
                        baseDir.setMaxDepth(MdlUtil.parseLong(stringValue, MdlConst.LNG_MAX));
                        break;
                    }
                }
            }

            // ディレクトリ階層の整合性チェック
            if (baseDir.getMinDepth() > baseDir.getMaxDepth()) {
                isOk = false;
                consoleWriteLine(MdlConst.LVL_E, methodName + " INVALID ARGUMENT : -min " + baseDir.getMinDepth() + " > -max : " + baseDir.getMaxDepth());
            }

            // 更新日付ではなく作成日で評価
            for (String key : List.of("ctime")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    baseDir.setCreationTime(true);
                }
            }

            // LIST表示時のダブルクォーテーションフラグ
            for (String key : List.of("dq")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    baseDir.setDq(true);
                }
            }

            // ファイル削除前コマンド
            for (String key : List.of("precmd")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    stringValue = MdlArg.getValue(namedArgs, key);
                    if (stringValue != null && !stringValue.isBlank()) {
                        baseDir.setPreRmCmd(stringValue);
                        baseDir.setPreRmCmd(true);
                        break;
                    }
                }
            }

            // ファイル削除前作成ディレクトリ
            for (String key : List.of("mkdir")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    stringValue = MdlArg.getValue(namedArgs, key);
                    if (stringValue != null && !stringValue.isBlank()) {
                        baseDir.setMkdirPath(stringValue);
                        baseDir.setMkDir(true);
                        break;
                    }
                }
            }

            // 更新のみ表示フラグ
            for (String key : List.of("no-diff")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    isDiff = false;
                    break;
                }
            }

            // TYPE
            stringValue = MdlArg.getValue(namedArgs, "type");
            if (stringValue == null || stringValue.isBlank()) {
                baseDir.setTargetType("f");
                baseDir.setRmFile(true);
            } else {
                baseDir.setTargetType(stringValue);
                String lowerType = stringValue.toLowerCase(Locale.ROOT);
                if (lowerType.contains("f")) {
                    baseDir.setRmFile(true);
                }
                if (lowerType.contains("d")) {
                    baseDir.setRmDir(true);
                }
                if (lowerType.contains("e")) {
                    baseDir.setRmEmptyDir(true);
                }
                if (lowerType.contains("s")) {
                    baseDir.setRmSymlink(true);
                }
                if (lowerType.contains("b")) {
                    baseDir.setRmFile(true);
                    baseDir.setRmEmptyDir(true);
                }
                if (lowerType.contains("a")) {
                    baseDir.setRmFile(true);
                    baseDir.setRmDir(true);
                    baseDir.setRmEmptyDir(true);
                    baseDir.setRmSymlink(true);
                }
            }

            // シンボリックリンク判定フラグ
            for (String key : List.of("sym")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    baseDir.setSymLink(true);
                    break;
                }
            }

            // フィルタ設定
            cmmnArgs.getFilterLists();
            baseDir.setIncFilesList(cmmnArgs.getIncFilesList());
            baseDir.setIncDirsList(cmmnArgs.getIncDirsList());
            baseDir.setExcFilesList(cmmnArgs.getExcFilesList());
            baseDir.setExcDirsList(cmmnArgs.getExcDirsList());
            baseDir.setRegIncBasename(cmmnArgs.isRegIncBasename());
            baseDir.setRegExcBasename(cmmnArgs.isRegExcBasename());
            baseDir.setDirFilterOr(cmmnArgs.isDirFilterOr());
            baseDir.setIncHitRecursive(cmmnArgs.isIncHitRecursive());
            baseDir.setExcHitRecursive(cmmnArgs.isExcHitRecursive());

            // 保存世代削除：保存世代
            for (String key : List.of("gen")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    stringValue = MdlArg.getValue(namedArgs, key);
                    if (stringValue != null && !stringValue.isBlank()) {
                        int parsedInt = MdlUtil.parseInt(stringValue, MdlConst.INT_NULL);
                        if (parsedInt != MdlConst.INT_NULL) {
                            baseDir.setGeneration(parsedInt);
                        }
                        break;
                    }
                }
            }

            // 日付取得フラグ
            for (String key : List.of("name")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    baseDir.setDateByName(true);
                }
            }

            // 非削除フラグ
            for (String key : List.of("no-rm")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    baseDir.setRm(false);
                }
            }

            // 日付設定先の取得
            for (String key : List.of("ts-to")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    stringValue = MdlArg.getValue(namedArgs, key);
                    if (stringValue != null && !stringValue.isBlank()) {
                        baseDir.setSetDateTime(true);
                        baseDir.setSetDateTimeTo(stringValue);
                        break;
                    }
                }
            }

            // 日付設定先の取得
            for (String key : List.of("ts")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    stringValue = MdlArg.getValue(namedArgs, key);
                    if (stringValue != null && !stringValue.isBlank()) {
                        baseDir.setDateTimeMode(baseDir.parseDateTimeMode(stringValue));
                        baseDir.setSetDateTimeTo(stringValue);
                        break;
                    }
                }
            }

            // -cwd
            hasValue = false;
            for (String key : List.of("cwd")) {
                if (MdlArg.containsKey(namedArgs, key)) {
                    stringValue = MdlArg.getValue(namedArgs, key);
                    if (stringValue != null && !stringValue.isBlank()) {
                        hasValue = true;
                        baseDir.setWorkDir(stringValue);
                        break;
                    }
                    if (!hasValue) {
                        baseDir.setWorkDir(baseDir.getPath());
                    }
                }
            }

            // リスト追加
            targetList.add(baseDir);
            this.baseDir = baseDir;
        } else {
            // -----------------------------------------------------------------
            // 設定ファイルが指定された場合
            // -----------------------------------------------------------------
            configFile.setDelimiter(delimiter);
            configFile.setTimeout(timeout);
            isOk = configFile.readConfig(configPath, true);
            if (isOk) {
                if (targetList.isEmpty()) {
                    consoleWriteLine(MdlConst.LVL_E, methodName + " NO DEF-LINES FOUND : " + configPath);
                }
            } else {
                consoleWriteLine(MdlConst.LVL_E, methodName + " FAILED TO configFile.readConfig() : " + configPath);
            }
        }

        // -----------------------------------------------------------------
        // Sort Option：
        // -----------------------------------------------------------------
        for (String key : List.of("sort")) {
            if (MdlArg.containsKey(namedArgs, key)) {
                stringValue = MdlArg.getValue(namedArgs, key);
                if (stringValue != null && !stringValue.isBlank()) {
                    sortType = MdlFile.getSortTypeNum(stringValue);
                    break;
                }
            }
        }
        for (String key : List.of("desc")) {
            if (MdlArg.containsKey(namedArgs, key)) {
                isAscending = false;
            }
        }
        for (String key : List.of("asc")) {
            if (MdlArg.containsKey(namedArgs, key)) {
                isAscending = true;
            }
        }
        for (String key : List.of("show-dirs")) {
            if (MdlArg.containsKey(namedArgs, key)) {
                isShowDirList = true;
            }
        }
        for (String key : List.of("show-files")) {
            if (MdlArg.containsKey(namedArgs, key)) {
                isShowFileList = true;
            }
        }

        // -----------------------------------------------------------------
        // 掃除
        // -----------------------------------------------------------------
        namedArgs.clear();

        return isOk;
    }

    /**
     * アプリケーションの使用方法（コマンドラインオプションおよび各設定項目の現在値または設定例）をログに出力します。
     */
    public void usage() {
        String label = (isExample ? "例" : "現在値");
        logger.writeLine(MdlConst.LVL_NONE, "");
        logger.writeLine(MdlConst.LVL_NONE, "Usage : " + exeDir + File.separator + exeBaseName + ".exe [Option] [Option]...");
        logger.writeLine(MdlConst.LVL_NONE, "");
        logger.writeLine(MdlConst.LVL_NONE, "■Execution Option：");
        logger.writeLine(MdlConst.LVL_NONE, "   -clean|-exec   ：対象一覧の削除                          （" + label + "=" + (isExample ? "true" : !isList) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -list          ：対象一覧の表示（実行取消）              （" + label + "=" + (isExample ? "false" : isList) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -no-rm         ：非削除モード・削除前実行コマンド実行    （" + label + "=" + (isExample ? "false" : !baseDir.isRm()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "■Config File Option：");
        logger.writeLine(MdlConst.LVL_NONE, "   -c|-conf path  ：設定ファイルのパス                      （" + label + "=" + (isExample ? "C:\\Tool\\Infra\\conf\\FileClean.conf" : configPath) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -delimiter str ：設定区切り文字                          （" + label + "=" + (isExample ? "|" : delimiter) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "■Non Config File Option：");
        logger.writeLine(MdlConst.LVL_NONE, "  Basic Option：");
        logger.writeLine(MdlConst.LVL_NONE, "   -action|-a act ：delete | gendel                         （" + label + "=" + (isExample ? "delete" : baseDir.getActionString(baseDir.getActionCode()) + " CODE=" + baseDir.getActionCode()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -path|-f path  ：対象ディレクトリパス                    （" + label + "=" + (isExample ? "C:\\Log" : baseDir.getPath()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "  Filter Option：");
        logger.writeLine(MdlConst.LVL_NONE, "   -type fdes     ：対象：f=file|d=dir|e=emptydir|s=symdir  （" + label + "=" + (isExample ? "f" : baseDir.getTargetType()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -sym           ：シンボリックリンク判定フラグ            （" + label + "=" + (isExample ? "true" : baseDir.isSymLink()) + "）");
        String termStr = isExample ? "30" : String.format(Locale.ROOT, "%.1f", baseDir.getTerm()) + "：" + (baseDir.getThresholdDate() != null ? MdlDate.getFormattedDate(baseDir.getThresholdDate(), "yyyy/MM/dd HH:mm:ss") : "");
        logger.writeLine(MdlConst.LVL_NONE, "   -term|-days val：削除対象ファイル更新経過日数            （" + label + "=" + termStr + "）");
        String dateStr = isExample ? MdlDate.getFormattedDate(LocalDateTime.now(), "yyyyMMdd") : (baseDir.getThresholdDate() != null ? MdlDate.getFormattedDate(baseDir.getThresholdDate(), "yyyyMMdd") : "");
        logger.writeLine(MdlConst.LVL_NONE, "   -date yyyyMMdd ：削除対象ファイル更新日付                （" + label + "=" + dateStr + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -new           ：経過日数(-term)以内を削除する場合       （" + label + "=" + (isExample ? "false" : baseDir.isNew()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -min  value    ：最小ディレクトリ階層                    （" + label + "=" + (isExample ? "0" : baseDir.getMinDepth()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -max  value    ：最大ディレクトリ階層                    （" + label + "=" + (isExample ? "3" : baseDir.getMaxDepth()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -id 正規表現   ：絞り込みディレクトリ名(カンマ区切り)    （" + label + "=" + (isExample ? "^log$,^tmp$" : String.join("|", baseDir.getIncDirsList())) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -xd 正規表現   ：除外ディレクトリ名(カンマ区切り)        （" + label + "=" + (isExample ? "^bin$,^conf$" : String.join("|", baseDir.getExcDirsList())) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -if 正規表現   ：絞り込みファイル名(カンマ区切り)        （" + label + "=" + (isExample ? "\\.log$,\\.dat$" : String.join("|", baseDir.getIncFilesList())) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -xf 正規表現   ：除外ファイル名(カンマ区切り)            （" + label + "=" + (isExample ? "\\.exe$,\\.dll$" : String.join("|", baseDir.getExcFilesList())) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -idorxd        ：-id or -xdフラグ                        （" + label + "=" + (isExample ? "false" : baseDir.isDirFilterOr()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -no-id-rec     ：-id結果の階層下への非適用フラグ         （" + label + "=" + (isExample ? "false" : !baseDir.isIncHitRecursive()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -no-xd-rec     ：-xd結果の階層下への非適用フラグ         （" + label + "=" + (isExample ? "false" : !baseDir.isExcHitRecursive()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -dq            ：対象一覧表示時ダブルクォーテーション囲み（" + label + "=" + (isExample ? "true" : !baseDir.isDq()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "  Generation Delete Option：");
        logger.writeLine(MdlConst.LVL_NONE, "   -gen int       ：保存世代数                              （" + label + "=" + (isExample ? "10" : baseDir.getGeneration()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "  DateTime Option：");
        logger.writeLine(MdlConst.LVL_NONE, "   -ctime         ：更新日付ではなく作成日で評価            （" + label + "=" + (isExample ? "false" : baseDir.isCreationTime()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -name          ：ファイル名に含む日付で評価              （" + label + "=" + (isExample ? "false" : baseDir.isDateByName()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "  Command Option：");
        logger.writeLine(MdlConst.LVL_NONE, "   -mkdir path    ：削除前ディレクトリ作成                  （" + label + "=" + (isExample ? "D:\\Backup\\TargetName\\_RELPATH_" : baseDir.getMkdirPath()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -precmd cmd    ：削除前実行コマンド                      （" + label + "=" + (isExample ? "C:\\Progra~1\\7-Zip\\7z.exe a -y -snl {}.zip {}" : baseDir.getPreRmCmd()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -cwd [path]    ：ワーキングディレクトリ                  （" + label + "=" + (isExample ? "C:\\Tool\\Work" : baseDir.getWorkDir()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -ts-to path    ：日付設定先パス                          （" + label + "=" + (isExample ? "{}.zip" : baseDir.getSetDateTimeTo()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -ts n|t|y|f    ：日付：n=今、t=今日、y=昨日、f=FILE属性  （" + label + "=" + (isExample ? "f" : baseDir.getDateTimeModeString(baseDir.getDateTimeMode())) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "  Subfolder Sorting Option：");
        logger.writeLine(MdlConst.LVL_NONE, "   -sort type     ：ソート=none|name|ctime|mtime            （" + label + "=" + (isExample ? "none" : MdlFile.getSortTypeName(sortType)) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -desc          ：降順フラグ                              （" + label + "=" + (isExample ? "false" : !isAscending) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "■Exit Code Option：");
        logger.writeLine(MdlConst.LVL_NONE, "   -na-mkdir      ：-pathが存在しない場合のMKDIRフラグ      （" + label + "=" + (isExample ? "false" : baseDir.isMkRmBaseDir()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -na-i|w|e      ：-pathが存在しない場合の終了コード       （" + label + "=" + (isExample ? "false" : baseDir.getNaRetCode()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -err-i|w|e     ：削除失敗時の終了コード                  （" + label + "=" + (isExample ? "false" : baseDir.getErrRetCode()) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "■Other Option      ：");
        logger.writeLine(MdlConst.LVL_NONE, "   -v|-vv|-brief  ：冗長表示|簡素表示                       （" + label + "=" + (isExample ? "2" : verbose) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -no-diff       ：更新のみ表示の取消                       (" + label + "=" + (isExample ? "false" : !isDiff) + "）");
        logger.writeLine(MdlConst.LVL_NONE, "   -console mode  ：メッセージ表示 off|stdout|stderr");
        logger.writeLine(MdlConst.LVL_NONE, "   -ldir path     ：ログ出力先ディレクトリパス（日付付ファイル名で出力）");
        logger.writeLine(MdlConst.LVL_NONE, "   -log  path     ：ログ出力ファイルパス      （-ldirより優先）");
        logger.writeLine(MdlConst.LVL_NONE, "   -h-sample      ：Usageの表示時に値例表示フラグ");
        logger.writeLine(MdlConst.LVL_NONE, "   --show-sample-config|-help-conf：サンプル設定ファイル表示フラグ");
        logger.writeLine(MdlConst.LVL_NONE, "");
        logger.writeLine(MdlConst.LVL_NONE, "Return Code : " + MdlConst.LVL_I + ":SUCCESS / " + MdlConst.LVL_W + ":WARN / " + MdlConst.LVL_E + ":ERROR");
        logger.writeLine(MdlConst.LVL_NONE, "");
    }

    /**
     * 指定されたエラーレベルとメッセージをログに書き込みます。
     *
     * @param errorLevel エラーレベル
     * @param message ログに書き込むメッセージ文字列
     */
    public void consoleWriteLine(int errorLevel, String message) {
        if (logger != null) {
            logger.writeLine(errorLevel, message);
        }
    }

    /**
     * 指定された {@link ClsBaseDir} オブジェクトの定義情報（動作条件、対象ディレクトリ、削除条件等）をコンソール・ログに出力します。
     *
     * @param baseDir 出力対象の {@link ClsBaseDir} オブジェクト
     */
    public void printDefinition(ClsBaseDir baseDir) {
        if (baseDir == null) {
            throw new NullPointerException("baseDir must not be null");
        }

        consoleWriteLine(MdlConst.LVL_NONE, String.format(Locale.ROOT, "# 定義    ：行番号= %04d ACTION=%s / フラグ：実行=%s 削除=%s 期間評価=%s Verbose=%d",
                baseDir.getLineNo(), baseDir.getActionString(baseDir.getActionCode()), baseDir.isExec(), baseDir.isRm(), baseDir.isTerm(), baseDir.getVerbose()));
        consoleWriteLine(MdlConst.LVL_NONE, String.format(Locale.ROOT, "# 削除対象：ファイル=%s ディレクトリ=%s 空フォルダ=%s SYMLINK=%s",
                baseDir.isRmFile(), baseDir.isRmDir(), baseDir.isRmEmptyDir(), baseDir.isRmSymlink()));
        if (baseDir.isBaseDir()) {
            consoleWriteLine(MdlConst.LVL_NONE, "# DIR パス：" + baseDir.getPath());
        } else {
            consoleWriteLine(MdlConst.LVL_NONE, "# FILEパス：= " + baseDir.getPath());
        }
        switch (baseDir.getActionCode()) {
            case ClsBaseDir.ACTION_GEN_DELETE:
                consoleWriteLine(MdlConst.LVL_NONE, String.format(Locale.ROOT, "# 保存世代：%,d", baseDir.getGeneration()));
                break;
            default:
                String dateStr = baseDir.getThresholdDate() != null ? MdlDate.getFormattedDate(baseDir.getThresholdDate(), "yyyy/MM/dd HH:mm:ss") : "";
                if (baseDir.isNew()) {
                    consoleWriteLine(MdlConst.LVL_NONE, String.format(Locale.ROOT, "# 保存期間：%.1f DAYS[%s]より新しいファイルを対象", baseDir.getTerm(), dateStr));
                } else {
                    consoleWriteLine(MdlConst.LVL_NONE, String.format(Locale.ROOT, "# 保存期間：%.1f DAYS[%s]より古いファイルを対象", baseDir.getTerm(), dateStr));
                }
                break;
        }
        consoleWriteLine(MdlConst.LVL_NONE, "# 検索階層：" + baseDir.getMinDepth() + " ～ " + baseDir.getMaxDepth());
        consoleWriteLine(MdlConst.LVL_NONE, "# 絞込条件： DIR = [" + String.join("|", baseDir.getIncDirsList()) + "] / FILE = [" + String.join("|", baseDir.getIncFilesList()) + "] / idrec = " + baseDir.isIncHitRecursive());
        consoleWriteLine(MdlConst.LVL_NONE, "# 除外条件： DIR = [" + String.join("|", baseDir.getExcDirsList()) + "] / FILE = [" + String.join("|", baseDir.getExcFilesList()) + "] / xdrec = " + baseDir.isExcHitRecursive());
    }

    /**
     * サンプル設定ファイルの内容と各項目の解説をコンソール・ログに出力します。
     */
    public void showSampleConfig() {
        if (logger == null) {
            return;
        }
        logger.writeLine(MdlConst.LVL_NONE, "################################################################################");
        logger.writeLine(MdlConst.LVL_NONE, "# 00：対象ホスト名CSVリスト：ALL、または正規表現（例：^SRVAP\\d+$,^SRVBT\\d+$）");
        logger.writeLine(MdlConst.LVL_NONE, "# 01：処理実行フラグ：0=実行しない、1=実行する");
        logger.writeLine(MdlConst.LVL_NONE, "# 02：処理対象タイプ：削除対象    ：F=ファイル削除、E=空フォルダ削除、D=フォルダ削除、SRM=シンボリックリンク削除、S：シンボリックリンク判定");
        logger.writeLine(MdlConst.LVL_NONE, "#                     日数判定方法：C=作成日付で判定、NAME=ファイル名で判定");
        logger.writeLine(MdlConst.LVL_NONE, "#                     経過日数    ：DAYS=TODAY、TERM=NOW");
        logger.writeLine(MdlConst.LVL_NONE, "#                     削除モード  ：GEN=世代保存削除、NORM=削除しない");
        logger.writeLine(MdlConst.LVL_NONE, "#                     日付設定    ：TSX（TSN=NOW、TST=TODAY、TSY=YESTERDAY、TSF=FILE）");
        logger.writeLine(MdlConst.LVL_NONE, "#                     削除エラー時：ERR-I=正常終了、ERR-W=警告終了(初期値)、削除ERR-W=異常終了");
        logger.writeLine(MdlConst.LVL_NONE, "#                     パス非存在時：NA-I=正常終了(初期値)、NA-W=警告終了、NA-W=異常終了、NA-MKDIR=パス作成");
        logger.writeLine(MdlConst.LVL_NONE, "#                     事前コマンド：NORMAL=常に正常終了、NEGATIVE=負値はエラー、W数値=警告閾値、E数値=異常閾値、CWD=パスにCHDIR、SHOW-PARAM=情報表示");
        logger.writeLine(MdlConst.LVL_NONE, "#                                   P数値=優先度、PRERM=タイムスタンプ設定先存在時事前削除有無、TIMEOUT数値=タイムアウト（秒）");
        logger.writeLine(MdlConst.LVL_NONE, "#                                   M-CMD=cmd.exe /c 事前コマンド、M-PS=powershell -command 事前コマンド; exit $LASTEXITCODE");
        logger.writeLine(MdlConst.LVL_NONE, "# 03：経過日数|保存世代");
        logger.writeLine(MdlConst.LVL_NONE, "# 04：パス");
        logger.writeLine(MdlConst.LVL_NONE, "# 05：最小階層数");
        logger.writeLine(MdlConst.LVL_NONE, "# 06：最大階層数");
        logger.writeLine(MdlConst.LVL_NONE, "# 07：絞込：フォルダ名");
        logger.writeLine(MdlConst.LVL_NONE, "# 08：絞込：ファイル名");
        logger.writeLine(MdlConst.LVL_NONE, "# 09：除外：フォルダ名");
        logger.writeLine(MdlConst.LVL_NONE, "# 10：除外：ファイル名");
        logger.writeLine(MdlConst.LVL_NONE, "# 11：ファイル削除前コマンド");
        logger.writeLine(MdlConst.LVL_NONE, "# 12：ファイル削除前作成ディレクトリ");
        logger.writeLine(MdlConst.LVL_NONE, "# 13：日付設定先パス");
        logger.writeLine(MdlConst.LVL_NONE, "# ※ファイル削除前コマンド／ファイル削除前作成ディレクトリの文字列置換マクロ");
        logger.writeLine(MdlConst.LVL_NONE, "#   {}、_PATH_    ：ファイルフルパス");
        logger.writeLine(MdlConst.LVL_NONE, "#   _RELPATH_     ：削除対象ベースパスからの相対パス");
        logger.writeLine(MdlConst.LVL_NONE, "#   _RELFLAT_     ：相対パスのパス区切り文字列「\\」「/」を「_」に変換したもの");
        logger.writeLine(MdlConst.LVL_NONE, "#   _BASEDIR_     ：削除対象ベースパス");
        logger.writeLine(MdlConst.LVL_NONE, "#   _DIR_         ：ファイルフルパスの親ディレクトリパス");
        logger.writeLine(MdlConst.LVL_NONE, "#   _RELDIR_      ：ファイル相対パスの親ディレクトリパス");
        logger.writeLine(MdlConst.LVL_NONE, "#   _RELDIRFLAT_  ：ファイル相対パスの親ディレクトリパスのパス区切り文字列「\\」「/」を「_」に変換したもの");
        logger.writeLine(MdlConst.LVL_NONE, "#   _FILENAME_    ：ファイル名（拡張子付き）");
        logger.writeLine(MdlConst.LVL_NONE, "#   _BASENAME_    ：ファイル名（拡張子無し）");
        logger.writeLine(MdlConst.LVL_NONE, "#   _COMPUTERNAME_：コンピュータ名");
        logger.writeLine(MdlConst.LVL_NONE, "#   %y%Y%m%d%w    ：日付");
        logger.writeLine(MdlConst.LVL_NONE, "#   %H%M%S%pid    ：時刻／PID");
        logger.writeLine(MdlConst.LVL_NONE, "################################################################################");
        logger.writeLine(MdlConst.LVL_NONE, "#--------------+-----+---------------------+------+------------------+-----+-----+------------------+---------------+------------------+-------------------+----------------------------------------------------------------------+---------------------------+------------------------+");
        logger.writeLine(MdlConst.LVL_NONE, "# サーバ共通：ログ                                                                                                                                                                                                                                                                     |");
        logger.writeLine(MdlConst.LVL_NONE, "#--------------+-----+---------------------+------+------------------+-----+-----+------------------+---------------+------------------+-------------------+----------------------------------------------------------------------+---------------------------+------------------------+");
        logger.writeLine(MdlConst.LVL_NONE, "# ホストリスト | FLG | OPTIONS             | 期間 | パス             | MIN | MAX | 絞込ディレクトリ | 絞込ファイル  | 除外ディレクトリ | 除外ファイル      | 事前実行コマンド                                                     | 事前作成ディレクトリパス  | タイムスタンプ設定先   |");
        logger.writeLine(MdlConst.LVL_NONE, "ALL            |   1 | F,DAYS,TSF,P4,PRERM |    2 | C:\\Log           |     |     |                  |               |                  | \\.zip$,^ys\\.log   | C:\\Progra~1\\7-Zip\\7z.exe a -y -snl \"_DIR_\\_BASENAME_.zip\" \"{}\"       |                           | \"_DIR_\\_BASENAME_.zip\" |");
        logger.writeLine(MdlConst.LVL_NONE, "ALL            |   1 | F,DAYS              |    7 | C:\\Log           |     |     |                  | \\.zip$        |                  |                   | C:\\Tool\\Infra\\bin.cur\\FsFileUtil.exe -f \"{}\" -t \"\\\\FILESEVER\\Backup\\Log\\%y\\%m\\_COMPUTERNAME_\\_RELPATH_\\_FILENAME_\" | |    |");
        logger.writeLine(MdlConst.LVL_NONE, "AP             |   1 | F,NORM              |    0 | C:\\Log\\webapps   |     |     |                  | ^App\\.log$    |                  |                   | C:\\Tool\\Infra\\bin.cur\\FsFileUtil.exe -f \"{}\" -a rotate -k 10         |                           |                        |");
        logger.writeLine(MdlConst.LVL_NONE, "ALL            |   1 | F,DAYS,TSF,P4,PRERM |    2 | C:\\Log\\webapps   |     |     |                  | ^App\\.log     |                  | \\.zip$,^App\\.log$ | C:\\Progra~1\\7-Zip\\7z.exe a -y -snl \"_DIR_\\App.%Y%m%d.zip\" \"{}\"       |                           | \"_DIR_\\App.%Y%m%d.zip\" |");
        logger.writeLine(MdlConst.LVL_NONE, "#--------------+-----+---------------------+------+------------------+-----+-----+------------------+---------------+------------------+-------------------+----------------------------------------------------------------------+---------------------------+------------------------+");
        logger.writeLine(MdlConst.LVL_NONE, "# BATCHサーバ：リリースバックアップ                                                                                                                                                                                                                                                    |");
        logger.writeLine(MdlConst.LVL_NONE, "#--------------+-----+---------------------+------+------------------+-----+-----+------------------+---------------+------------------+-------------------+----------------------------------------------------------------------+---------------------------+------------------------+");
        logger.writeLine(MdlConst.LVL_NONE, "BATCH          |   1 | D,GEN,TSF           |   10 | C:\\job\\_backup   |     |   0 | _\\d{8}_\\d{6}$    |               |                  |                   | C:\\Progra~1\\7-Zip\\7z.exe a -y -snl \"{}.zip\" \"{}\"                     |                           | \"{}.zip\"               |");
        logger.writeLine(MdlConst.LVL_NONE, "BATCH          |   1 | F,GEN               |   30 | C:\\job\\_backup   |     |   0 |                  | \\.zip$        |                  |                   |                                                                      |                           |                        |");
        logger.writeLine(MdlConst.LVL_NONE, "#--------------+-----+---------------------+------+------------------+-----+-----+------------------+---------------+------------------+-------------------+----------------------------------------------------------------------+---------------------------+------------------------+");
        logger.writeLine(MdlConst.LVL_NONE, "################################################################################");
        logger.writeLine(MdlConst.LVL_NONE, "#◆サンプル設定ファイルの解説");
        logger.writeLine(MdlConst.LVL_NONE, "################################################################################");
        logger.writeLine(MdlConst.LVL_NONE, "# 1)C:\\Logのファイルのうち、今から２日前より古いファイルをzip圧縮し（元のファイルのタイムスタンプをzipファイルに適用）、圧縮が正常終了した場合は元のファイルを削除");
        logger.writeLine(MdlConst.LVL_NONE, "#   ホストリスト：ALL⇒全サーバを対象");
        logger.writeLine(MdlConst.LVL_NONE, "#   OPTIONS：F⇒ファイルを対象、DAYS⇒２日間経過したファイルを対象、TSF⇒元のファイルのタイムスタンプをzipファイルに適用、P4⇒圧縮時のプロセス優先度を４番とデフォルトの３番から１つ下げる、PRERM⇒ZIPファイルが既に存在する場合は削除");
        logger.writeLine(MdlConst.LVL_NONE, "#   除外ファイル：ファイル名が「\\.zip$,^App\\.log」に該当するファイルは対象外とし、該当しないファイルを対象とする");
        logger.writeLine(MdlConst.LVL_NONE, "#   削除ファイル⇒C:\\Log\\tomcat\\catalina.log.20230225");
        logger.writeLine(MdlConst.LVL_NONE, "#   圧縮ファイル⇒C:\\Log\\tomcat\\catalina.log.20230225.zip");
        logger.writeLine(MdlConst.LVL_NONE, "# 2)C:\\Logのファイルのうち、７日間経過したZIPファイルをファイルサーバへコピーし、コピーが成功したらコピー元のファイルを削除");
        logger.writeLine(MdlConst.LVL_NONE, "#   ホストリスト：ALL⇒全サーバを対象");
        logger.writeLine(MdlConst.LVL_NONE, "#   OPTIONS：F⇒ファイルを対象、DAYS⇒今から７日前より古いファイルを対象");
        logger.writeLine(MdlConst.LVL_NONE, "#   絞込ファイル：ファイル名が「\\.zip$」に該当するファイルを対象とし、該当しないファイルは対象外とする");
        logger.writeLine(MdlConst.LVL_NONE, "#   移動元ファイル名⇒C:\\Log\\tomcat\\catalina.log.20230225.zip");
        logger.writeLine(MdlConst.LVL_NONE, "#   移動先ファイル名⇒\\\\FILESEVER\\Backup\\Log\\%y\\%m\\_COMPUTERNAME_\\_RELPATH_\\_FILENAME_⇒\\\\FILESEVER\\Backup\\Log\\2023\\02\\SERVER001\\tomcat\\catalina.log.20230225.zip");
        logger.writeLine(MdlConst.LVL_NONE, "# 3)C:\\Log\\webapps\\App.logをC:\\Log\\*\\App.log.1に名前を変更し、C:\\Log\\webapps\\App.log.10まで10世代保持");
        logger.writeLine(MdlConst.LVL_NONE, "#   ホストリスト：AP⇒ホスト名が正規表現「AP」に該当するサーバ");
        logger.writeLine(MdlConst.LVL_NONE, "#   OPTIONS：F⇒ファイルを対象、NORM⇒該当ファイルを削除しない（事前実行コマンドでリネームするので）");
        logger.writeLine(MdlConst.LVL_NONE, "#   絞込ファイル：ファイル名が「^ys\\.log$」に該当するファイルを対象とし、該当しないファイルは対象外とする");
        logger.writeLine(MdlConst.LVL_NONE, "# 4)C:\\Log\\webapps\\App.log.1ファイルのうち、２日間経過したファイルをzip圧縮し（元のファイルのタイムスタンプをzipファイルに適用）、圧縮が正常終了した場合は元のファイルを削除");
        logger.writeLine(MdlConst.LVL_NONE, "#   ホストリスト：ALL⇒全サーバを対象");
        logger.writeLine(MdlConst.LVL_NONE, "#   OPTIONS：F⇒ファイルを対象、DAYS⇒２日間経過したファイルを対象、TSF⇒元のファイルのタイムスタンプをzipファイルに適用、P4⇒圧縮時のプロセス優先度を４番とデフォルトの３番から１つ下げる、PRERM⇒ZIPファイルが既に存在する場合は削除");
        logger.writeLine(MdlConst.LVL_NONE, "#   絞込ファイル：ファイル名が「^App\\.log」に該当するファイルを対象とし、該当しないファイルは対象外とする");
        logger.writeLine(MdlConst.LVL_NONE, "#   除外ファイル：ファイル名が「\\.zip$,^App\\.log$」に該当するファイルは対象外とし、該当しないファイルを対象とする");
        logger.writeLine(MdlConst.LVL_NONE, "#   ※^App\\.log.1～^App\\.log.10を^App\\.log.1～^App\\.log.10");
        logger.writeLine(MdlConst.LVL_NONE, "#   ※圧縮ファイル名は、App.%Y%m%d.zipとファイル名に日付をつける");
        logger.writeLine(MdlConst.LVL_NONE, "#   ※ZIPファイルのファイルサーバへの退避は、2)で対象となる");
        logger.writeLine(MdlConst.LVL_NONE, "# 5)C:\\job\\_backupにサブディレクトリ名がbat_20230225_100000と日付_日時を含む場合、最新の10世代保持し、それより古い世代のディレクトリをzip圧縮し（元のディレクトリ名の日付_日時zipファイルのタイムスタンプに適用）、圧縮が正常終了した場合は元のファイルを削除");
        logger.writeLine(MdlConst.LVL_NONE, "#   ホストリスト：BATCH⇒ホスト名が正規表現「BATCH」に該当するサーバ");
        logger.writeLine(MdlConst.LVL_NONE, "#   OPTIONS：D⇒ディレクトリを対象、GEN⇒世代保存削除、TSF⇒元のファイルのタイムスタンプをzipファイルに適用");
        logger.writeLine(MdlConst.LVL_NONE, "#   絞込ファイル：ディレクトリ名が「_\\d{8}_\\d{6}$」に該当するディレクトリを対象とし、該当しないディレクトリは対象外とする");
        logger.writeLine(MdlConst.LVL_NONE, "# 6)C:\\job\\_backup\\xxx_yyyymmdd_HHMMSS.zipファイルのうち、10世代より古いZIPファイルがあれば削除");
        logger.writeLine(MdlConst.LVL_NONE, "################################################################################");
    }

    // --- ゲッター / セッター ---

    /**
     * 実行可能ファイルのベース名を取得します。
     *
     * @return 実行可能ファイルのベース名
     */
    public String getExeBaseName() {
        return exeBaseName;
    }

    /**
     * 実行可能ファイルのベース名を設定します。
     *
     * @param exeBaseName 実行可能ファイルのベース名
     */
    public void setExeBaseName(String exeBaseName) {
        this.exeBaseName = exeBaseName != null ? exeBaseName : "";
    }

    /**
     * 実行可能ファイルが配置されているディレクトリパスを取得します。
     *
     * @return 実行ディレクトリパス
     */
    public String getExeDir() {
        return exeDir;
    }

    /**
     * 実行可能ファイルが配置されているディレクトリパスを設定します。
     *
     * @param exeDir 実行ディレクトリパス
     */
    public void setExeDir(String exeDir) {
        this.exeDir = exeDir != null ? exeDir : "";
    }

    /**
     * 使用法・ヘルプ表示フラグを取得します。
     *
     * @return 使用法表示フラグ（{@link #USAGE_NONE}, {@link #USAGE_USAGE}, {@link #USAGE_SHOW_SAMPLE_CONFIG}）
     */
    public int getUsageFlag() {
        return usageFlag;
    }

    /**
     * アプリケーションの終了コードを取得します。
     *
     * @return 終了コード
     */
    public int getReturnCode() {
        return returnCode;
    }

    /**
     * アプリケーションの終了コードを設定します。
     *
     * @param returnCode 終了コード
     */
    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    /**
     * 詳細ログ出力レベル（Verbose）を取得します。
     *
     * @return ログ出力レベル
     */
    public int getVerbose() {
        return verbose;
    }

    /**
     * 詳細ログ出力レベル（Verbose）を設定します。
     *
     * @param verbose ログ出力レベル
     */
    public void setVerbose(int verbose) {
        this.verbose = verbose;
    }

    /**
     * スタックトレース出力フラグを取得します。
     *
     * @return スタックトレース出力フラグ
     */
    public boolean isStackTrace() {
        return isStackTrace;
    }

    /**
     * スタックトレース出力フラグを設定します。
     *
     * @param stackTrace スタックトレース出力フラグ
     */
    public void setStackTrace(boolean stackTrace) {
        isStackTrace = stackTrace;
    }

    /**
     * コマンド実行タイムアウト秒数を取得します。
     *
     * @return タイムアウト秒数
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * コマンド実行タイムアウト秒数を設定します。
     *
     * @param timeout タイムアウト秒数
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * 実行中のマシン名（ホスト名）を取得します。
     *
     * @return マシン名
     */
    public String getMachineName() {
        return configFile.getMachineName();
    }

    /**
     * リスト表示のみ（非削除モード）であるかを取得します。
     *
     * @return リスト表示モードの場合は {@code true}
     */
    public boolean isList() {
        return isList;
    }

    /**
     * 更新のみ表示フラグを取得します。
     *
     * @return 更新のみ表示フラグ
     */
    public boolean isDiff() {
        return isDiff;
    }

    /**
     * 更新のみ表示フラグを設定します。
     *
     * @param diff 更新のみ表示フラグ
     */
    public void setDiff(boolean diff) {
        isDiff = diff;
    }

    /**
     * 削除対象の定義リストを取得します。
     *
     * @return 削除対象定義リスト
     */
    public List<ClsBaseDir> getTargetList() {
        return targetList;
    }

    /**
     * 削除対象の定義リストを設定します。
     *
     * @param targetList 削除対象定義リスト
     */
    public void setTargetList(List<ClsBaseDir> targetList) {
        this.targetList = targetList != null ? targetList : new ArrayList<>();
    }

    /**
     * サブフォルダのソート種別コードを取得します。
     *
     * @return ソート種別コード
     */
    public int getSortType() {
        return sortType;
    }

    /**
     * サブフォルダのソート種別コードを設定します。
     *
     * @param sortType ソート種別コード
     */
    public void setSortType(int sortType) {
        this.sortType = sortType;
    }

    /**
     * ソート昇順フラグを取得します。
     *
     * @return 昇順の場合は {@code true}、降順の場合は {@code false}
     */
    public boolean isAscending() {
        return isAscending;
    }

    /**
     * ソート昇順フラグを設定します。
     *
     * @param ascending 昇順の場合は {@code true}、降順の場合は {@code false}
     */
    public void setAscending(boolean ascending) {
        isAscending = ascending;
    }

    /**
     * ディレクトリ一覧表示フラグを取得します。
     *
     * @return ディレクトリ一覧表示フラグ
     */
    public boolean isShowDirList() {
        return isShowDirList;
    }

    /**
     * ディレクトリ一覧表示フラグを設定します。
     *
     * @param showDirList ディレクトリ一覧表示フラグ
     */
    public void setShowDirList(boolean showDirList) {
        isShowDirList = showDirList;
    }

    /**
     * ファイル一覧表示フラグを取得します。
     *
     * @return ファイル一覧表示フラグ
     */
    public boolean isShowFileList() {
        return isShowFileList;
    }

    /**
     * ファイル一覧表示フラグを設定します。
     *
     * @param showFileList ファイル一覧表示フラグ
     */
    public void setShowFileList(boolean showFileList) {
        isShowFileList = showFileList;
    }
}
