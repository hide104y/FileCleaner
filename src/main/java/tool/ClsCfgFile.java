package tool;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import tool.cmnclslib.cls.ClsLogger;
import tool.cmnclslib.mdl.MdlApp;
import tool.cmnclslib.mdl.MdlConst;
import tool.cmnclslib.mdl.MdlDate;
import tool.cmnclslib.mdl.MdlFile;
import tool.cmnclslib.mdl.MdlUtil;

/**
 * 設定ファイルの読み込みおよび解析を行うクラスです。
 */
public class ClsCfgFile {

    private final ClsLogger logger;

    private List<ClsBaseDir> targetList = new ArrayList<>();
    private List<String> listStr = new ArrayList<>();
    private int verbose = 0;
    private int timeout = 3600;
    private String machineName = "";
    private String delimiter = ";|";

    /**
     * {@link ClsCfgFile} クラスの新しいインスタンスを初期化します。
     *
     * @param logger ログ出力を行う {@link ClsLogger} インスタンス
     */
    public ClsCfgFile(ClsLogger logger) {
        this.logger = logger;
        String host = System.getenv("COMPUTERNAME");
        if (host == null || host.isEmpty()) {
            try {
                host = java.net.InetAddress.getLocalHost().getHostName();
            } catch (Exception e) {
                host = "";
            }
        }
        this.machineName = (host != null && !host.isEmpty()) ? host.toUpperCase(Locale.ROOT) : "";
    }

    /**
     * 指定された設定ファイルを読み込み、設定内容を解析してターゲットリストに追加します。
     *
     * @param configFilePath 読み込む設定ファイルのパス
     * @param callAddTarget ターゲット追加処理を行う場合は {@code true}。ファイル構造チェックのみを行う場合は {@code false}
     * @return 読み込みおよび解析が成功した場合は {@code true}、例外が発生した場合は {@code false}
     */
    public boolean readConfig(String configFilePath, boolean callAddTarget) {
        final String methodName = "[ClsCfgFile.readConfig()]";
        int currentLineNumber = 0;
        boolean isSuccess = true;
        String regexPattern = "^\\s*(?<KEY>[^#" + Pattern.quote(delimiter) + "]+)\\s*[" + Pattern.quote(delimiter) + "]\\s*(?<VAL>.+)\\s*$";
        Pattern regex = Pattern.compile(regexPattern);

        try {
            Charset charset = MdlFile.detectFileEncoding(configFilePath);
            if (charset == null) {
                charset = StandardCharsets.UTF_8;
            }

            try (BufferedReader reader = Files.newBufferedReader(Paths.get(configFilePath), charset)) {
                String currentLine;
                while ((currentLine = reader.readLine()) != null) {
                    currentLineNumber++;
                    String trimmedLine = currentLine.strip();
                    if (verbose > 5 && logger != null) {
                        logger.writeLine(MdlConst.LVL_DEBUG, String.format(Locale.ROOT, "%s[%04d] LINE = %s", methodName, currentLineNumber, trimmedLine));
                    }
                    Matcher match = regex.matcher(trimmedLine);
                    if (match.find()) {
                        String keyGroup = match.group("KEY");
                        String[] keys = keyGroup.split(",");
                        boolean isMatch = false;
                        for (String key : keys) {
                            String target = key.strip();
                            if ("ALL".equalsIgnoreCase(target) || "LOCALHOST".equalsIgnoreCase(target)) {
                                isMatch = true;
                                break;
                            }
                            try {
                                if (Pattern.compile(target, Pattern.CASE_INSENSITIVE).matcher(machineName).find()) {
                                    isMatch = true;
                                    break;
                                }
                            } catch (Exception e) {
                                if (verbose > 5 && logger != null) {
                                    logger.writeLine(MdlConst.LVL_DEBUG, methodName + " Regex error: " + e.getMessage());
                                }
                            }
                        }

                        if (isMatch && callAddTarget) {
                            addTarget(currentLineNumber + ";" + match.group("VAL").strip());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            isSuccess = false;
            if (logger != null) {
                logger.writeLine(MdlConst.LVL_E, String.format(Locale.ROOT, "%s[LINE:%d] Exception : %s", methodName, currentLineNumber, ex.getMessage()));
            }
        }
        return isSuccess;
    }

    /**
     * 設定ファイルから読み込んだ1行の文字列を解析し、ターゲット情報を設定リストに追加します。
     *
     * @param line 設定ファイルの行文字列
     * @return 解析およびリスト追加が正常に行われた場合は {@code true}、構文エラーや行番号が無効な場合は {@code false}
     */
    public boolean addTarget(String line) {
        final String methodName = "[ClsCfgFile.addTarget()]";
        ClsBaseDir baseDir = new ClsBaseDir();
        baseDir.setTimeout(timeout);
        baseDir.setVerbose(verbose);

        int columnIndex = 0;
        Pattern delimiterRegex = Pattern.compile("\\s*[" + Pattern.quote(delimiter) + "]\\s*");
        boolean isDate = false;
        boolean isOk = true;

        String[] lineParts = delimiterRegex.split(line, -1);

        // 01:IDX-00：削除対象タイプフラグ
        String stringValue = lineParts.length > 0 ? lineParts[columnIndex].trim() : "";

        if (!stringValue.isEmpty() && MdlUtil.isNumeric(stringValue)) {
            baseDir.setLineNo(Integer.parseInt(stringValue));
            baseDir.setVerbose(verbose > 2 ? verbose : MdlUtil.parseInt(stringValue, 1) - 1);
        }
        if (baseDir.getLineNo() == 0) {
            return false;
        }

        String formattedLineNumber = String.format(Locale.ROOT, "%04d", baseDir.getLineNo());

        if (verbose > 4 && logger != null) {
            logger.writeLine(MdlConst.LVL_DEBUG, methodName + "[" + formattedLineNumber + "] HIT = " + String.join("|", lineParts));
        }

        // 必須行
        if (lineParts.length > 2) {
            // 02:IDX-01：削除実行フラグ
            columnIndex++;
            stringValue = lineParts.length > columnIndex ? lineParts[columnIndex].trim() : "";
            if (MdlUtil.isNumeric(stringValue)) {
                int execFlagValue = MdlUtil.parseInt(stringValue, 0);
                baseDir.setExec(execFlagValue > 0);
            }

            // 03:IDX-02：削除対象タイプ
            columnIndex++;
            stringValue = lineParts.length > columnIndex ? lineParts[columnIndex].trim().toUpperCase(Locale.ROOT) : "";
            if (!stringValue.isEmpty()) {
                List<String> typeList = MdlUtil.parseCsvToList(null, stringValue, "[,\\/]", verbose, true);
                for (String element : typeList) {
                    String typeString = element.trim().toUpperCase(Locale.ROOT);
                    switch (typeString) {
                        case "ALL":
                            baseDir.setRmFile(true);
                            baseDir.setRmDir(true);
                            baseDir.setRmEmptyDir(true);
                            baseDir.setRmSymlink(true);
                            baseDir.setSymLink(true);
                            break;
                        case "F":
                            baseDir.setRmFile(true);
                            break;
                        case "D":
                            baseDir.setRmDir(true);
                            break;
                        case "E":
                            baseDir.setRmEmptyDir(true);
                            break;
                        case "S":
                            baseDir.setSymLink(true);
                            break;
                        case "SRM":
                            baseDir.setRmSymlink(true);
                            break;
                        case "C":
                            baseDir.setCreationTime(true);
                            break;
                        case "NAME":
                            baseDir.setDateByName(true);
                            break;
                        case "NIR":
                            baseDir.setIncHitRecursive(false);
                            break;
                        case "NXR":
                            baseDir.setExcHitRecursive(false);
                            break;
                        case "NORM":
                            baseDir.setRm(false);
                            break;
                        case "DAYS":
                            baseDir.setDays(true);
                            break;
                        case "DATE":
                            isDate = true;
                            break;
                        case "GEN":
                            baseDir.setActionCode(ClsBaseDir.ACTION_GEN_DELETE);
                            baseDir.setMaxDepth(0);
                            break;
                        case "TSN":
                            baseDir.setDateTimeMode(ClsBaseDir.DATETIME_NOW);
                            break;
                        case "TST":
                            baseDir.setDateTimeMode(ClsBaseDir.DATETIME_TODAY);
                            break;
                        case "TSY":
                            baseDir.setDateTimeMode(ClsBaseDir.DATETIME_YESTERDAY);
                            break;
                        case "TSF":
                            baseDir.setDateTimeMode(ClsBaseDir.DATETIME_FILEINFO);
                            break;
                        case "NA-MKDIR":
                            baseDir.setNaRetCode(MdlConst.LVL_I);
                            baseDir.setMkRmBaseDir(true);
                            break;
                        case "NA-I":
                            baseDir.setNaRetCode(MdlConst.LVL_I);
                            break;
                        case "NA-W":
                            baseDir.setNaRetCode(MdlConst.LVL_W);
                            break;
                        case "NA-E":
                            baseDir.setNaRetCode(MdlConst.LVL_E);
                            break;
                        case "ERR-I":
                            baseDir.setErrRetCode(MdlConst.LVL_I);
                            break;
                        case "ERR-W":
                            baseDir.setErrRetCode(MdlConst.LVL_W);
                            break;
                        case "ERR-E":
                            baseDir.setErrRetCode(MdlConst.LVL_E);
                            break;
                        case "NORMAL":
                            baseDir.setAlwaysNormal(true);
                            break;
                        case "NEGATIVE":
                            baseDir.setErrorAtNegativeValue(true);
                            break;
                        case "CWD":
                            baseDir.setWorkDir("objBaseDir.Path");
                            break;
                        case "CWD-MKDIR":
                            baseDir.setWorkDir("objBaseDir.StrMkdirPath");
                            break;
                        case "SHOW-PARAM":
                            baseDir.setShowCmdParam(true);
                            break;
                        case "PRERM":
                            baseDir.setPreRmFile(true);
                            break;
                        case "M-EXE":
                            baseDir.setExecModeCode(ClsBaseDir.EXEC_MODE_EXE);
                            break;
                        case "M-CMD":
                            baseDir.setExecModeCode(ClsBaseDir.EXEC_MODE_CMD);
                            break;
                        case "M-PS":
                            baseDir.setExecModeCode(ClsBaseDir.EXEC_MODE_PS);
                            break;
                        default:
                            String warnVal = getRegexGroupValue(typeString, "^W(?<VAL>[0-9]+)$", "VAL");
                            if (!warnVal.isEmpty()) {
                                baseDir.setWarnThreshold(MdlUtil.parseInt(warnVal, 0));
                            }
                            String errVal = getRegexGroupValue(typeString, "^E(?<VAL>[0-9]+)$", "VAL");
                            if (!errVal.isEmpty()) {
                                baseDir.setErrorThreshold(MdlUtil.parseInt(errVal, 0));
                            }
                            String priorityVal = getRegexGroupValue(typeString, "^P(?<VAL>[0-9])$", "VAL");
                            if (!priorityVal.isEmpty()) {
                                baseDir.setPriority(Math.min(MdlUtil.parseInt(priorityVal, 0), 5));
                            }
                            String timeoutVal = getRegexGroupValue(typeString, "^TIMEOUT(?<VAL>[0-9]+)$", "VAL");
                            if (!timeoutVal.isEmpty()) {
                                baseDir.setTimeout(MdlUtil.parseInt(timeoutVal, timeout));
                            }
                            break;
                    }
                }
            }

            // 04:IDX-03：経過日数／保存世代
            columnIndex++;
            stringValue = lineParts.length > columnIndex ? lineParts[columnIndex].trim() : "";
            if (!stringValue.isEmpty()) {
                if (baseDir.getActionCode() == ClsBaseDir.ACTION_GEN_DELETE) {
                    baseDir.setGeneration(MdlUtil.parseInt(stringValue, ClsBaseDir.GENERATION));
                } else {
                    if (isDate) {
                        LocalDateTime parsedDate = MdlDate.parseDateTime(stringValue);
                        if (parsedDate != null) {
                            baseDir.setTerm(true);
                            baseDir.setThresholdDate(parsedDate);
                            long days = ChronoUnit.DAYS.between(parsedDate.toLocalDate(), LocalDate.now());
                            baseDir.setTerm(days);
                        }
                    } else {
                        baseDir.setTerm(true);
                        baseDir.setTerm(MdlUtil.parseDouble(stringValue, 0.0));
                        if (baseDir.getTerm() < 0) {
                            baseDir.setNew(true);
                            baseDir.setTerm(Math.abs(baseDir.getTerm()));
                        }
                        long termNanos = (long) (baseDir.getTerm() * 86400_000_000_000L);
                        if (baseDir.isDays()) {
                            baseDir.setThresholdDate(LocalDate.now().atStartOfDay().minusNanos(termNanos));
                        } else {
                            baseDir.setThresholdDate(LocalDateTime.now().minusNanos(termNanos));
                        }
                    }
                }
            }

            // 05:IDX-04：パス
            columnIndex++;
            stringValue = lineParts.length > columnIndex ? lineParts[columnIndex].trim() : "";
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
                    boolean exists = false;
                    if (baseDir.isMkRmBaseDir()) {
                        if (MdlFile.createDirectory(stringValue) <= MdlFile.OK_MKDIR_HANTEI) {
                            baseDir.setBaseDir(true);
                            exists = true;
                        } else if (logger != null) {
                            logger.writeLine(baseDir.getNaRetCode(), methodName + "[LINE:" + formattedLineNumber + "][COL:05:PATH] FAILED TO MKDIR : " + stringValue);
                        }
                    }
                    if (!exists) {
                        baseDir.setExec(false);
                        baseDir.setReturnCode(baseDir.getNaRetCode());
                        if (logger != null) {
                            logger.writeLine(baseDir.getNaRetCode(), methodName + "[LINE:" + formattedLineNumber + "][COL:05:PATH] NO SUCH A FILE OR DIRECTORY : " + stringValue);
                        }
                    }
                    break;
            }

            // 06:IDX-05：MIN
            columnIndex++;
            if (lineParts.length > columnIndex) {
                stringValue = lineParts[columnIndex].trim();
                if (!stringValue.isEmpty()) {
                    if (MdlUtil.isNumeric(stringValue)) {
                        baseDir.setMinDepth(Math.abs(Long.parseLong(stringValue)));
                    } else {
                        baseDir.setOk(false);
                        if (logger != null) {
                            logger.writeLine(MdlConst.LVL_E, methodName + "[LINE:" + formattedLineNumber + "][COL:06:MIN] SYNTAX ERROR (NOT NUMERIC) : " + stringValue);
                        }
                    }
                }
            }

            // 07:IDX-06：MAX
            columnIndex++;
            if (lineParts.length > columnIndex) {
                stringValue = lineParts[columnIndex].trim();
                if (!stringValue.isEmpty()) {
                    if (MdlUtil.isNumeric(stringValue)) {
                        baseDir.setMaxDepth(Math.abs(Long.parseLong(stringValue)));
                    } else {
                        baseDir.setOk(false);
                        if (logger != null) {
                            logger.writeLine(MdlConst.LVL_E, methodName + "[LINE:" + formattedLineNumber + "][COL:07:MAX] SYNTAX ERROR (NOT NUMERIC) : " + stringValue);
                        }
                    }
                }
            }

            // 08:IDX-07：INC:DIRS
            columnIndex++;
            baseDir.setDirFilterOr(true);
            if (lineParts.length > columnIndex) {
                stringValue = lineParts[columnIndex].trim();
                if (!stringValue.isEmpty()) {
                    listStr = baseDir.getIncDirsList();
                    if (!parseCsvToList(stringValue)) {
                        if (logger != null) {
                            logger.writeLine(MdlConst.LVL_E, methodName + "[LINE:" + formattedLineNumber + "][COL:08:INC DIR] SYNTAX ERROR : " + stringValue);
                        }
                        baseDir.setOk(false);
                    }
                }
            }

            // 09:IDX-08：INC:FILES
            columnIndex++;
            if (lineParts.length > columnIndex) {
                stringValue = lineParts[columnIndex].trim();
                if (!stringValue.isEmpty()) {
                    listStr = baseDir.getIncFilesList();
                    if (!parseCsvToList(stringValue)) {
                        if (logger != null) {
                            logger.writeLine(MdlConst.LVL_E, methodName + "[LINE:" + formattedLineNumber + "][COL:09:INC FILE] SYNTAX ERROR : " + stringValue);
                        }
                        baseDir.setOk(false);
                    }
                }
            }

            // 10:IDX-09：EXC:DIRS
            columnIndex++;
            if (lineParts.length > columnIndex) {
                stringValue = lineParts[columnIndex].trim();
                if (!stringValue.isEmpty()) {
                    listStr = baseDir.getExcDirsList();
                    if (!parseCsvToList(stringValue)) {
                        if (logger != null) {
                            logger.writeLine(MdlConst.LVL_E, methodName + "[LINE:" + formattedLineNumber + "][COL:10:EXC DIR] SYNTAX ERROR : " + stringValue);
                        }
                        baseDir.setOk(false);
                    }
                }
            }

            // 11:IDX-10：EXC:FILES
            columnIndex++;
            if (lineParts.length > columnIndex) {
                stringValue = lineParts[columnIndex].trim();
                if (!stringValue.isEmpty()) {
                    listStr = baseDir.getExcFilesList();
                    if (!parseCsvToList(stringValue)) {
                        if (logger != null) {
                            logger.writeLine(MdlConst.LVL_E, methodName + "[LINE:" + formattedLineNumber + "][COL:11:EXC FILE] SYNTAX ERROR : " + stringValue);
                        }
                        baseDir.setOk(false);
                    }
                }
            }

            // 12:IDX-11：ファイル削除前コマンド
            columnIndex++;
            if (lineParts.length > columnIndex) {
                stringValue = lineParts[columnIndex].trim();
                if (!stringValue.isEmpty()) {
                    baseDir.setPreRmCmd(stringValue);
                    baseDir.setPreRmCmd(true);
                }
            }

            // 13:IDX-12：ファイル削除前作成ディレクトリ
            columnIndex++;
            if (lineParts.length > columnIndex) {
                stringValue = MdlUtil.trimQuotes(lineParts[columnIndex]);
                if (!stringValue.isEmpty()) {
                    baseDir.setMkdirPath(stringValue);
                    baseDir.setMkDir(true);
                }
            }

            // 14:IDX-13：日付設定先
            columnIndex++;
            if (lineParts.length > columnIndex) {
                stringValue = MdlUtil.trimQuotes(lineParts[columnIndex]);
                if (!stringValue.isEmpty()) {
                    baseDir.setSetDateTimeTo(stringValue);
                    baseDir.setSetDateTime(true);
                }
            }

            // 調整
            if ("objBaseDir.Path".equals(baseDir.getWorkDir())) {
                baseDir.setWorkDir(baseDir.getPath());
            }
            if ("objBaseDir.StrMkdirPath".equals(baseDir.getWorkDir())) {
                baseDir.setWorkDir(baseDir.getMkdirPath());
            }
        } else {
            // 不具合行
            if (logger != null) {
                logger.writeLine(MdlConst.LVL_E, methodName + "[LINE:" + formattedLineNumber + "] SYNTAX ERROR : " + line);
            }
            isOk = false;
        }

        targetList.add(baseDir);

        if (!baseDir.isOk()) {
            isOk = false;
        }

        return isOk;
    }

    /**
     * CSV形式の文字列をリストに変換して格納します。
     *
     * @param csvString CSV形式の文字列
     * @return 処理が正常に完了した場合は {@code true}、失敗した場合は {@code false}
     */
    public boolean parseCsvToList(String csvString) {
        if (csvString == null || csvString.isEmpty()) {
            return true;
        }

        final String methodName = "[ClsCfgFile.parseCsvToList()]";
        String[] csvArray = csvString.split(",");

        for (String element : csvArray) {
            String tempString = element.startsWith("*") ? "." + element : element;
            try {
                listStr.add(tempString);
            } catch (Exception e) {
                if (logger != null) {
                    logger.writeLine(MdlConst.LVL_W, methodName + " SYNTAX ERROR : " + csvString);
                }
                return false;
            }
        }

        return true;
    }

    /**
     * 指定された正規表現パターンに基づいて、入力文字列からキーに対応する値を抽出します。
     *
     * @param input 入力文字列
     * @param pattern 正規表現パターン
     * @param key 抽出する値のキー名
     * @return キーに対応する抽出された値。一致しない場合は空文字列
     */
    public static String getRegexGroupValue(String input, String pattern, String key) {
        if (input == null || pattern == null || key == null) {
            return "";
        }
        try {
            Matcher match = Pattern.compile(pattern).matcher(input);
            if (match.find()) {
                return match.group(key);
            }
        } catch (Exception e) {
            return "";
        }
        return "";
    }

    // --- ゲッター / セッター ---

    /**
     * 解析されたターゲットディレクトリ定義のリストを取得します。
     *
     * @return ターゲットディレクトリ定義リスト
     */
    public List<ClsBaseDir> getTargetList() {
        return targetList;
    }

    /**
     * 解析されたターゲットディレクトリ定義のリストを設定します。
     *
     * @param targetList ターゲットディレクトリ定義リスト
     */
    public void setTargetList(List<ClsBaseDir> targetList) {
        this.targetList = targetList != null ? targetList : new ArrayList<>();
    }

    /**
     * 一時格納用文字列リストを取得します。
     *
     * @return 一時格納用文字列リスト
     */
    public List<String> getListStr() {
        return listStr;
    }

    /**
     * 一時格納用文字列リストを設定します。
     *
     * @param listStr 一時格納用文字列リスト
     */
    public void setListStr(List<String> listStr) {
        this.listStr = listStr != null ? listStr : new ArrayList<>();
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
     * 判定基準となるマシン名（ホスト名）を取得します。
     *
     * @return マシン名
     */
    public String getMachineName() {
        return machineName;
    }

    /**
     * 判定基準となるマシン名（ホスト名）を設定します。
     *
     * @param machineName マシン名
     */
    public void setMachineName(String machineName) {
        this.machineName = machineName != null ? machineName : "";
    }

    /**
     * 設定ファイルの区切り文字を取得します。
     *
     * @return 区切り文字
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * 設定ファイルの区切り文字を設定します。
     *
     * @param delimiter 区切り文字
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter != null ? delimiter : ";|";
    }
}
