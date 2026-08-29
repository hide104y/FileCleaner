package tool;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import tool.cmnclslib.mdl.MdlConst;

/**
 * 削除対象ディレクトリ・ファイルの設定および動作パラメータを保持するクラスです。
 */
public class ClsBaseDir {

    public static final int ACTION_DELETE = 0;
    public static final int ACTION_GEN_DELETE = 1;
    public static final int GENERATION = 10;
    public static final int DATETIME_NOW = 0;
    public static final int DATETIME_TODAY = 1;
    public static final int DATETIME_YESTERDAY = 2;
    public static final int DATETIME_FILEINFO = 3;
    public static final int EXEC_MODE_NORMAL = 0;
    public static final int EXEC_MODE_CMD = 1;
    public static final int EXEC_MODE_PS = 2;
    public static final int EXEC_MODE_PSC = 3;
    public static final int EXEC_MODE_EXE = 4;

    private int actionCode = ACTION_DELETE;
    private int lineNo = 0;
    private int verbose = 0;
    private int naRetCode = MdlConst.LVL_I;
    private int errRetCode = MdlConst.LVL_W;
    private int returnCode = MdlConst.LVL_I;
    private boolean isOk = true;
    private boolean isExec = true;
    private boolean isRm = true;
    private String targetType = "f";
    private String path = "";
    private boolean isBaseDir = false;
    private boolean isTerm = false;
    private boolean isDays = false;
    private boolean isNew = false;
    private double term = 0.0;
    private LocalDateTime thresholdDate;
    private boolean isRmFile = false;
    private boolean isRmEmptyDir = false;
    private boolean isRmDir = false;
    private boolean isRmSymlink = false;
    private boolean isSymLink = false;
    private boolean isDq = false;
    private boolean isDiff = true;
    private String mkdirPath = "";
    private boolean isMkDir = false;
    private boolean isMkRmBaseDir = false;
    private String workDir = "";
    private String preRmCmd = "";
    private boolean isPreRmCmd = false;
    private boolean isPreRmFile = false;
    private int execModeCode = EXEC_MODE_EXE;
    private int priority = 3;
    private int warnThreshold = MdlConst.INT_NULL;
    private int errorThreshold = MdlConst.INT_NULL;
    private int timeout = 3600;
    private boolean isErrorAtNegativeValue = false;
    private boolean isAlwaysNormal = false;
    private boolean isShowCmd = false;
    private boolean isShowOutput = false;
    private boolean isShowExitCode = false;
    private boolean isShowCmdParam = false;
    private boolean isSetDateTime = false;
    private String setDateTimeTo = "";
    private int dateTimeMode = DATETIME_NOW;
    private boolean isCreationTime = false;
    private boolean isDateByName = false;
    private long minDepth = 0;
    private long maxDepth = MdlConst.LNG_MAX;
    private boolean isRegIncBasename = true;
    private boolean isRegExcBasename = true;
    private boolean isIncHitRecursive = true;
    private boolean isExcHitRecursive = true;
    private boolean isDirFilterOr = false;
    private List<String> incDirsList = new ArrayList<>();
    private List<String> incFilesList = new ArrayList<>();
    private List<String> excDirsList = new ArrayList<>();
    private List<String> excFilesList = new ArrayList<>();
    private int generation = GENERATION;
    private List<String> targetList = new ArrayList<>();

    /**
     * {@link ClsBaseDir} クラスの新しいインスタンスを初期化します。
     */
    public ClsBaseDir() {
    }

    /**
     * 日時モードを表す数値コードから、対応する文字列コード（"n", "t", "y", "f"）を取得します。
     *
     * @param dateTimeMode 日時モード数値コード（例: {@link #DATETIME_TODAY}）
     * @return 日時モードを表す文字列コード
     */
    public String getDateTimeModeString(int dateTimeMode) {
        switch (dateTimeMode) {
            case DATETIME_TODAY:
                return "t";
            case DATETIME_YESTERDAY:
                return "y";
            case DATETIME_FILEINFO:
                return "f";
            default:
                return "n";
        }
    }

    /**
     * 日時モードを表す文字列コード（"t", "today", "y", "yesterday", "f", "file"）を数値コードに変換します。
     *
     * @param modeString 日時モード文字列コード
     * @return 日時モードを表す数値コード（該当しない場合は {@link #DATETIME_NOW}）
     */
    public int parseDateTimeMode(String modeString) {
        if (modeString == null || modeString.isBlank()) {
            return DATETIME_NOW;
        }
        String lower = modeString.strip().toLowerCase(java.util.Locale.ROOT);
        switch (lower) {
            case "t":
            case "today":
                return DATETIME_TODAY;
            case "y":
            case "yesterday":
                return DATETIME_YESTERDAY;
            case "f":
            case "file":
                return DATETIME_FILEINFO;
            default:
                return DATETIME_NOW;
        }
    }

    /**
     * アクションコード数値から対応するアクション文字列（"delete", "gendel"）を取得します。
     *
     * @param actionCode アクション数値コード（例: {@link #ACTION_GEN_DELETE}）
     * @return アクションを表す文字列コード
     */
    public String getActionString(int actionCode) {
        if (actionCode == ACTION_GEN_DELETE) {
            return "gendel";
        }
        return "delete";
    }

    /**
     * アクション文字列コード（"gendel" 等）からアクション数値コードに変換します。
     *
     * @param actionString アクション文字列コード
     * @return アクション数値コード（該当しない場合は 0）
     */
    public int parseAction(String actionString) {
        if (actionString != null && "gendel".equalsIgnoreCase(actionString.strip())) {
            return ACTION_GEN_DELETE;
        }
        return 0;
    }

    // --- ゲッター / セッター ---

    /**
     * アクション数値コードを取得します。
     *
     * @return アクション数値コード（{@link #ACTION_DELETE} または {@link #ACTION_GEN_DELETE}）
     */
    public int getActionCode() {
        return actionCode;
    }

    /**
     * アクション数値コードを設定します。
     *
     * @param actionCode アクション数値コード
     */
    public void setActionCode(int actionCode) {
        this.actionCode = actionCode;
    }

    /**
     * 設定ファイル内の行番号を取得します。
     *
     * @return 行番号
     */
    public int getLineNo() {
        return lineNo;
    }

    /**
     * 設定ファイル内の行番号を設定します。
     *
     * @param lineNo 行番号
     */
    public void setLineNo(int lineNo) {
        this.lineNo = lineNo;
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
     * パスが存在しない場合の戻り値コードを取得します。
     *
     * @return 戻り値コード
     */
    public int getNaRetCode() {
        return naRetCode;
    }

    /**
     * パスが存在しない場合の戻り値コードを設定します。
     *
     * @param naRetCode 戻り値コード
     */
    public void setNaRetCode(int naRetCode) {
        this.naRetCode = naRetCode;
    }

    /**
     * 削除失敗時の戻り値コードを取得します。
     *
     * @return 戻り値コード
     */
    public int getErrRetCode() {
        return errRetCode;
    }

    /**
     * 削除失敗時の戻り値コードを設定します。
     *
     * @param errRetCode 戻り値コード
     */
    public void setErrRetCode(int errRetCode) {
        this.errRetCode = errRetCode;
    }

    /**
     * 実行結果の戻り値コードを取得します。
     *
     * @return 戻り値コード
     */
    public int getReturnCode() {
        return returnCode;
    }

    /**
     * 実行結果の戻り値コードを設定します。
     *
     * @param returnCode 戻り値コード
     */
    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    /**
     * 定義設定が正常であるかを取得します。
     *
     * @return 正常な場合は {@code true}
     */
    public boolean isOk() {
        return isOk;
    }

    /**
     * 定義設定が正常であるかを設定します。
     *
     * @param ok 正常フラグ
     */
    public void setOk(boolean ok) {
        isOk = ok;
    }

    /**
     * 処理実行フラグを取得します。
     *
     * @return 実行する場合は {@code true}
     */
    public boolean isExec() {
        return isExec;
    }

    /**
     * 処理実行フラグを設定します。
     *
     * @param exec 実行フラグ
     */
    public void setExec(boolean exec) {
        isExec = exec;
    }

    /**
     * 削除実行フラグを取得します。
     *
     * @return 削除を実行する場合は {@code true}
     */
    public boolean isRm() {
        return isRm;
    }

    /**
     * 削除実行フラグを設定します。
     *
     * @param rm 削除実行フラグ
     */
    public void setRm(boolean rm) {
        isRm = rm;
    }

    /**
     * 削除対象タイプ文字列を取得します。
     *
     * @return ターゲットタイプ文字列
     */
    public String getTargetType() {
        return targetType;
    }

    /**
     * 削除対象タイプ文字列を設定します。
     *
     * @param targetType ターゲットタイプ文字列
     */
    public void setTargetType(String targetType) {
        this.targetType = targetType != null ? targetType : "f";
    }

    /**
     * 対象ディレクトリまたはファイルのパスを取得します。
     *
     * @return 対象パス
     */
    public String getPath() {
        return path;
    }

    /**
     * 対象ディレクトリまたはファイルのパスを設定します。
     *
     * @param path 対象パス
     */
    public void setPath(String path) {
        this.path = path != null ? path : "";
    }

    /**
     * 対象パスがディレクトリであるかを取得します。
     *
     * @return ディレクトリの場合は {@code true}、ファイルの場合は {@code false}
     */
    public boolean isBaseDir() {
        return isBaseDir;
    }

    /**
     * 対象パスがディレクトリであるかを設定します。
     *
     * @param baseDir ディレクトリフラグ
     */
    public void setBaseDir(boolean baseDir) {
        isBaseDir = baseDir;
    }

    /**
     * 期間・経過日数による評価を行うかを取得します。
     *
     * @return 期間評価を行う場合は {@code true}
     */
    public boolean isTerm() {
        return isTerm;
    }

    /**
     * 期間・経過日数による評価を行うかを設定します。
     *
     * @param term 期間評価フラグ
     */
    public void setTerm(boolean term) {
        isTerm = term;
    }

    /**
     * 日数単位（日付境界）で評価するかを取得します。
     *
     * @return 日数単位で評価する場合は {@code true}
     */
    public boolean isDays() {
        return isDays;
    }

    /**
     * 日数単位（日付境界）で評価するかを設定します。
     *
     * @param days 日数単位フラグ
     */
    public void setDays(boolean days) {
        isDays = days;
    }

    /**
     * 閾値日時より新しいファイルを削除対象とするかを取得します。
     *
     * @return 新しいファイルを対象とする場合は {@code true}
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * 閾値日時より新しいファイルを削除対象とするかを設定します。
     *
     * @param isNew 新規ファイル対象フラグ
     */
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    /**
     * 削除判定経過日数（または期間）を取得します。
     *
     * @return 経過日数
     */
    public double getTerm() {
        return term;
    }

    /**
     * 削除判定経過日数（または期間）を設定します。
     *
     * @param term 経過日数
     */
    public void setTerm(double term) {
        this.term = term;
    }

    /**
     * 削除判定の閾値日時を取得します。
     *
     * @return 閾値日時
     */
    public LocalDateTime getThresholdDate() {
        return thresholdDate;
    }

    /**
     * 削除判定の閾値日時を設定します。
     *
     * @param thresholdDate 閾値日時
     */
    public void setThresholdDate(LocalDateTime thresholdDate) {
        this.thresholdDate = thresholdDate;
    }

    /**
     * ファイル削除フラグを取得します。
     *
     * @return ファイルを削除する場合は {@code true}
     */
    public boolean isRmFile() {
        return isRmFile;
    }

    /**
     * ファイル削除フラグを設定します。
     *
     * @param rmFile ファイル削除フラグ
     */
    public void setRmFile(boolean rmFile) {
        isRmFile = rmFile;
    }

    /**
     * 空ディレクトリ削除フラグを取得します。
     *
     * @return 空ディレクトリを削除する場合は {@code true}
     */
    public boolean isRmEmptyDir() {
        return isRmEmptyDir;
    }

    /**
     * 空ディレクトリ削除フラグを設定します。
     *
     * @param rmEmptyDir 空ディレクトリ削除フラグ
     */
    public void setRmEmptyDir(boolean rmEmptyDir) {
        isRmEmptyDir = rmEmptyDir;
    }

    /**
     * ディレクトリ削除フラグを取得します。
     *
     * @return ディレクトリを削除する場合は {@code true}
     */
    public boolean isRmDir() {
        return isRmDir;
    }

    /**
     * ディレクトリ削除フラグを設定します。
     *
     * @param rmDir ディレクトリ削除フラグ
     */
    public void setRmDir(boolean rmDir) {
        isRmDir = rmDir;
    }

    /**
     * シンボリックリンク削除フラグを取得します。
     *
     * @return シンボリックリンクを削除する場合は {@code true}
     */
    public boolean isRmSymlink() {
        return isRmSymlink;
    }

    /**
     * シンボリックリンク削除フラグを設定します。
     *
     * @param rmSymlink シンボリックリンク削除フラグ
     */
    public void setRmSymlink(boolean rmSymlink) {
        isRmSymlink = rmSymlink;
    }

    /**
     * シンボリックリンク判定フラグを取得します。
     *
     * @return シンボリックリンク判定を行う場合は {@code true}
     */
    public boolean isSymLink() {
        return isSymLink;
    }

    /**
     * シンボリックリンク判定フラグを設定します。
     *
     * @param symLink シンボリックリンク判定フラグ
     */
    public void setSymLink(boolean symLink) {
        isSymLink = symLink;
    }

    /**
     * 一覧表示時ダブルクォーテーション囲みフラグを取得します。
     *
     * @return ダブルクォーテーションで囲む場合は {@code true}
     */
    public boolean isDq() {
        return isDq;
    }

    /**
     * 一覧表示時ダブルクォーテーション囲みフラグを設定します。
     *
     * @param dq ダブルクォーテーション囲みフラグ
     */
    public void setDq(boolean dq) {
        isDq = dq;
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
     * 事前作成ディレクトリパスを取得します。
     *
     * @return 事前作成ディレクトリパス
     */
    public String getMkdirPath() {
        return mkdirPath;
    }

    /**
     * 事前作成ディレクトリパスを設定します。
     *
     * @param mkdirPath 事前作成ディレクトリパス
     */
    public void setMkdirPath(String mkdirPath) {
        this.mkdirPath = mkdirPath != null ? mkdirPath : "";
    }

    /**
     * 事前ディレクトリ作成フラグを取得します。
     *
     * @return 事前ディレクトリ作成を行う場合は {@code true}
     */
    public boolean isMkDir() {
        return isMkDir;
    }

    /**
     * 事前ディレクトリ作成フラグを設定します。
     *
     * @param mkDir 事前ディレクトリ作成フラグ
     */
    public void setMkDir(boolean mkDir) {
        isMkDir = mkDir;
    }

    /**
     * 対象パスが存在しない場合の自動ディレクトリ作成フラグを取得します。
     *
     * @return 自動作成する場合は {@code true}
     */
    public boolean isMkRmBaseDir() {
        return isMkRmBaseDir;
    }

    /**
     * 対象パスが存在しない場合の自動ディレクトリ作成フラグを設定します。
     *
     * @param mkRmBaseDir 自動作成フラグ
     */
    public void setMkRmBaseDir(boolean mkRmBaseDir) {
        isMkRmBaseDir = mkRmBaseDir;
    }

    /**
     * 作業ディレクトリ（CWD）パスを取得します。
     *
     * @return 作業ディレクトリパス
     */
    public String getWorkDir() {
        return workDir;
    }

    /**
     * 作業ディレクトリ（CWD）パスを設定します。
     *
     * @param workDir 作業ディレクトリパス
     */
    public void setWorkDir(String workDir) {
        this.workDir = workDir != null ? workDir : "";
    }

    /**
     * 削除前実行コマンド文字列を取得します。
     *
     * @return 削除前実行コマンド
     */
    public String getPreRmCmd() {
        return preRmCmd;
    }

    /**
     * 削除前実行コマンド文字列を設定します。
     *
     * @param preRmCmd 削除前実行コマンド
     */
    public void setPreRmCmd(String preRmCmd) {
        this.preRmCmd = preRmCmd != null ? preRmCmd : "";
    }

    /**
     * 削除前コマンド実行フラグを取得します。
     *
     * @return 削除前コマンドを実行する場合は {@code true}
     */
    public boolean isPreRmCmd() {
        return isPreRmCmd;
    }

    /**
     * 削除前コマンド実行フラグを設定します。
     *
     * @param preRmCmd 削除前コマンド実行フラグ
     */
    public void setPreRmCmd(boolean preRmCmd) {
        isPreRmCmd = preRmCmd;
    }

    /**
     * タイムスタンプ設定先存在時事前削除フラグを取得します。
     *
     * @return 事前削除フラグ
     */
    public boolean isPreRmFile() {
        return isPreRmFile;
    }

    /**
     * タイムスタンプ設定先存在時事前削除フラグを設定します。
     *
     * @param preRmFile 事前削除フラグ
     */
    public void setPreRmFile(boolean preRmFile) {
        isPreRmFile = preRmFile;
    }

    /**
     * コマンド実行モード数値コードを取得します。
     *
     * @return 実行モードコード（{@link #EXEC_MODE_NORMAL}, {@link #EXEC_MODE_CMD}, {@link #EXEC_MODE_PS}, {@link #EXEC_MODE_PSC}, {@link #EXEC_MODE_EXE}）
     */
    public int getExecModeCode() {
        return execModeCode;
    }

    /**
     * コマンド実行モード数値コードを設定します。
     *
     * @param execModeCode 実行モードコード
     */
    public void setExecModeCode(int execModeCode) {
        this.execModeCode = execModeCode;
    }

    /**
     * プロセス実行優先度を取得します。
     *
     * @return プロセス優先度
     */
    public int getPriority() {
        return priority;
    }

    /**
     * プロセス実行優先度を設定します。
     *
     * @param priority プロセス優先度
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * コマンド警告終了コード閾値を取得します。
     *
     * @return 警告終了コード閾値
     */
    public int getWarnThreshold() {
        return warnThreshold;
    }

    /**
     * コマンド警告終了コード閾値を設定します。
     *
     * @param warnThreshold 警告終了コード閾値
     */
    public void setWarnThreshold(int warnThreshold) {
        this.warnThreshold = warnThreshold;
    }

    /**
     * コマンド異常終了コード閾値を取得します。
     *
     * @return 異常終了コード閾値
     */
    public int getErrorThreshold() {
        return errorThreshold;
    }

    /**
     * コマンド異常終了コード閾値を設定します。
     *
     * @param errorThreshold 異常終了コード閾値
     */
    public void setErrorThreshold(int errorThreshold) {
        this.errorThreshold = errorThreshold;
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
     * 負の終了コードをエラーと判定するフラグを取得します。
     *
     * @return 負の値をエラーとする場合は {@code true}
     */
    public boolean isErrorAtNegativeValue() {
        return isErrorAtNegativeValue;
    }

    /**
     * 負の終了コードをエラーと判定するフラグを設定します。
     *
     * @param errorAtNegativeValue 負値エラーフラグ
     */
    public void setErrorAtNegativeValue(boolean errorAtNegativeValue) {
        isErrorAtNegativeValue = errorAtNegativeValue;
    }

    /**
     * 常に正常終了とみなすフラグを取得します。
     *
     * @return 常に正常とする場合は {@code true}
     */
    public boolean isAlwaysNormal() {
        return isAlwaysNormal;
    }

    /**
     * 常に正常終了とみなすフラグを設定します。
     *
     * @param alwaysNormal 常に正常フラグ
     */
    public void setAlwaysNormal(boolean alwaysNormal) {
        isAlwaysNormal = alwaysNormal;
    }

    /**
     * 実行コマンド表示フラグを取得します。
     *
     * @return コマンドを表示する場合は {@code true}
     */
    public boolean isShowCmd() {
        return isShowCmd;
    }

    /**
     * 実行コマンド表示フラグを設定します。
     *
     * @param showCmd コマンド表示フラグ
     */
    public void setShowCmd(boolean showCmd) {
        isShowCmd = showCmd;
    }

    /**
     * コマンド標準出力表示フラグを取得します。
     *
     * @return 標準出力を表示する場合は {@code true}
     */
    public boolean isShowOutput() {
        return isShowOutput;
    }

    /**
     * コマンド標準出力表示フラグを設定します。
     *
     * @param showOutput 標準出力表示フラグ
     */
    public void setShowOutput(boolean showOutput) {
        isShowOutput = showOutput;
    }

    /**
     * コマンド終了コード表示フラグを取得します。
     *
     * @return 終了コードを表示する場合は {@code true}
     */
    public boolean isShowExitCode() {
        return isShowExitCode;
    }

    /**
     * コマンド終了コード表示フラグを設定します。
     *
     * @param showExitCode 終了コード表示フラグ
     */
    public void setShowExitCode(boolean showExitCode) {
        isShowExitCode = showExitCode;
    }

    /**
     * コマンドパラメータ表示フラグを取得します。
     *
     * @return パラメータを表示する場合は {@code true}
     */
    public boolean isShowCmdParam() {
        return isShowCmdParam;
    }

    /**
     * コマンドパラメータ表示フラグを設定します。
     *
     * @param showCmdParam パラメータ表示フラグ
     */
    public void setShowCmdParam(boolean showCmdParam) {
        isShowCmdParam = showCmdParam;
    }

    /**
     * 日時設定フラグを取得します。
     *
     * @return 日時を設定する場合は {@code true}
     */
    public boolean isSetDateTime() {
        return isSetDateTime;
    }

    /**
     * 日時設定フラグを設定します。
     *
     * @param setDateTime 日時設定フラグ
     */
    public void setSetDateTime(boolean setDateTime) {
        isSetDateTime = setDateTime;
    }

    /**
     * 日時設定先パスを取得します。
     *
     * @return 日時設定先パス
     */
    public String getSetDateTimeTo() {
        return setDateTimeTo;
    }

    /**
     * 日時設定先パスを設定します。
     *
     * @param setDateTimeTo 日時設定先パス
     */
    public void setSetDateTimeTo(String setDateTimeTo) {
        this.setDateTimeTo = setDateTimeTo != null ? setDateTimeTo : "";
    }

    /**
     * 日時取得モード数値コードを取得します。
     *
     * @return 日時取得モード（{@link #DATETIME_NOW}, {@link #DATETIME_TODAY}, {@link #DATETIME_YESTERDAY}, {@link #DATETIME_FILEINFO}）
     */
    public int getDateTimeMode() {
        return dateTimeMode;
    }

    /**
     * 日時取得モード数値コードを設定します。
     *
     * @param dateTimeMode 日時取得モード
     */
    public void setDateTimeMode(int dateTimeMode) {
        this.dateTimeMode = dateTimeMode;
    }

    /**
     * 作成日時評価フラグを取得します。
     *
     * @return 作成日時で評価する場合は {@code true}、更新日時で評価する場合は {@code false}
     */
    public boolean isCreationTime() {
        return isCreationTime;
    }

    /**
     * 作成日時評価フラグを設定します。
     *
     * @param creationTime 作成日時評価フラグ
     */
    public void setCreationTime(boolean creationTime) {
        isCreationTime = creationTime;
    }

    /**
     * ファイル名から日時を抽出して評価するフラグを取得します。
     *
     * @return ファイル名から日時を抽出する場合は {@code true}
     */
    public boolean isDateByName() {
        return isDateByName;
    }

    /**
     * ファイル名から日時を抽出して評価するフラグを設定します。
     *
     * @param dateByName ファイル名日時評価フラグ
     */
    public void setDateByName(boolean dateByName) {
        isDateByName = dateByName;
    }

    /**
     * 探索最小階層数を取得します。
     *
     * @return 最小階層数
     */
    public long getMinDepth() {
        return minDepth;
    }

    /**
     * 探索最小階層数を設定します。
     *
     * @param minDepth 最小階層数
     */
    public void setMinDepth(long minDepth) {
        this.minDepth = minDepth;
    }

    /**
     * 探索最大階層数を取得します。
     *
     * @return 最大階層数
     */
    public long getMaxDepth() {
        return maxDepth;
    }

    /**
     * 探索最大階層数を設定します。
     *
     * @param maxDepth 最大階層数
     */
    public void setMaxDepth(long maxDepth) {
        this.maxDepth = maxDepth;
    }

    /**
     * 絞込ファイル名をベース名で評価するフラグを取得します。
     *
     * @return ベース名評価フラグ
     */
    public boolean isRegIncBasename() {
        return isRegIncBasename;
    }

    /**
     * 絞込ファイル名をベース名で評価するフラグを設定します。
     *
     * @param regIncBasename ベース名評価フラグ
     */
    public void setRegIncBasename(boolean regIncBasename) {
        isRegIncBasename = regIncBasename;
    }

    /**
     * 除外ファイル名をベース名で評価するフラグを取得します。
     *
     * @return ベース名評価フラグ
     */
    public boolean isRegExcBasename() {
        return isRegExcBasename;
    }

    /**
     * 除外ファイル名をベース名で評価するフラグを設定します。
     *
     * @param regExcBasename ベース名評価フラグ
     */
    public void setRegExcBasename(boolean regExcBasename) {
        isRegExcBasename = regExcBasename;
    }

    /**
     * 絞込一致結果を階層下に適用するフラグを取得します。
     *
     * @return 階層下に適用する場合は {@code true}
     */
    public boolean isIncHitRecursive() {
        return isIncHitRecursive;
    }

    /**
     * 絞込一致結果を階層下に適用するフラグを設定します。
     *
     * @param incHitRecursive 階層下適用フラグ
     */
    public void setIncHitRecursive(boolean incHitRecursive) {
        isIncHitRecursive = incHitRecursive;
    }

    /**
     * 除外一致結果を階層下に適用するフラグを取得します。
     *
     * @return 階層下に適用する場合は {@code true}
     */
    public boolean isExcHitRecursive() {
        return isExcHitRecursive;
    }

    /**
     * 除外一致結果を階層下に適用するフラグを設定します。
     *
     * @param excHitRecursive 階層下適用フラグ
     */
    public void setExcHitRecursive(boolean excHitRecursive) {
        isExcHitRecursive = excHitRecursive;
    }

    /**
     * ディレクトリフィルタをOR条件で評価するフラグを取得します。
     *
     * @return OR条件で評価する場合は {@code true}
     */
    public boolean isDirFilterOr() {
        return isDirFilterOr;
    }

    /**
     * ディレクトリフィルタをOR条件で評価するフラグを設定します。
     *
     * @param dirFilterOr OR条件フラグ
     */
    public void setDirFilterOr(boolean dirFilterOr) {
        isDirFilterOr = dirFilterOr;
    }

    /**
     * 絞込対象ディレクトリ名正規表現リストを取得します。
     *
     * @return 絞込ディレクトリ正規表現リスト
     */
    public List<String> getIncDirsList() {
        return incDirsList;
    }

    /**
     * 絞込対象ディレクトリ名正規表現リストを設定します。
     *
     * @param incDirsList 絞込ディレクトリ正規表現リスト
     */
    public void setIncDirsList(List<String> incDirsList) {
        this.incDirsList = incDirsList != null ? incDirsList : new ArrayList<>();
    }

    /**
     * 絞込対象ファイル名正規表現リストを取得します。
     *
     * @return 絞込ファイル正規表現リスト
     */
    public List<String> getIncFilesList() {
        return incFilesList;
    }

    /**
     * 絞込対象ファイル名正規表現リストを設定します。
     *
     * @param incFilesList 絞込ファイル正規表現リスト
     */
    public void setIncFilesList(List<String> incFilesList) {
        this.incFilesList = incFilesList != null ? incFilesList : new ArrayList<>();
    }

    /**
     * 除外対象ディレクトリ名正規表現リストを取得します。
     *
     * @return 除外ディレクトリ正規表現リスト
     */
    public List<String> getExcDirsList() {
        return excDirsList;
    }

    /**
     * 除外対象ディレクトリ名正規表現リストを設定します。
     *
     * @param excDirsList 除外ディレクトリ正規表現リスト
     */
    public void setExcDirsList(List<String> excDirsList) {
        this.excDirsList = excDirsList != null ? excDirsList : new ArrayList<>();
    }

    /**
     * 除外対象ファイル名正規表現リストを取得します。
     *
     * @return 除外ファイル正規表現リスト
     */
    public List<String> getExcFilesList() {
        return excFilesList;
    }

    /**
     * 除外対象ファイル名正規表現リストを設定します。
     *
     * @param excFilesList 除外ファイル正規表現リスト
     */
    public void setExcFilesList(List<String> excFilesList) {
        this.excFilesList = excFilesList != null ? excFilesList : new ArrayList<>();
    }

    /**
     * 保存世代数を取得します。
     *
     * @return 保存世代数
     */
    public int getGeneration() {
        return generation;
    }

    /**
     * 保存世代数を設定します。
     *
     * @param generation 保存世代数
     */
    public void setGeneration(int generation) {
        this.generation = generation;
    }

    /**
     * 一時対象ファイル・ディレクトリ名リストを取得します。
     *
     * @return 一時対象リスト
     */
    public List<String> getTargetList() {
        return targetList;
    }

    /**
     * 一時対象ファイル・ディレクトリ名リストを設定します。
     *
     * @param targetList 一時対象リスト
     */
    public void setTargetList(List<String> targetList) {
        this.targetList = targetList != null ? targetList : new ArrayList<>();
    }
}
