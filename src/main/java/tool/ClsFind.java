package tool;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import tool.cmnclslib.cls.ClsCmdExec;
import tool.cmnclslib.cls.ClsLogger;
import tool.cmnclslib.mdl.MdlApp;
import tool.cmnclslib.mdl.MdlConst;
import tool.cmnclslib.mdl.MdlDate;
import tool.cmnclslib.mdl.MdlFile;
import tool.cmnclslib.mdl.MdlUtil;

/**
 * ファイルおよびディレクトリの探索、条件判定、コマンド実行、削除処理を統括するクラスです。
 */
public class ClsFind {

    private final ClsLogger logger;
    private final ClsAppArg appArg;
    private ClsBaseDir baseDir;
    private final ClsCmdExec cmdExec;
    private long totalDirCount = 0;
    private long totalFileCount = 0;
    private long deleteErrorFiles = 0;
    private long deletedFiles = 0;
    private long deleteErrorDirs = 0;
    private long deletedDirs = 0;
    private long exceptionCount = 0;
    private String titleEn = "DEL";
    private String titleJp = "削除";

    /**
     * {@link ClsFind} クラスの新しいインスタンスを初期化します。
     *
     * @param logger ログ出力用の {@link ClsLogger} オブジェクト
     * @param appArg パラメータ管理用の {@link ClsAppArg} オブジェクト
     */
    public ClsFind(ClsLogger logger, ClsAppArg appArg) {
        this.logger = logger;
        this.appArg = appArg;
        this.cmdExec = new ClsCmdExec(logger);
        this.baseDir = new ClsBaseDir();
    }

    /**
     * 現在のOS環境に応じたデフォルトのコマンド実行シェルを取得します。
     *
     * @return デフォルトシェルのパスまたはコマンド名
     */
    private String getDefaultShell() {
        if (MdlApp.isWindows()) {
            String comSpec = System.getenv("ComSpec");
            return (comSpec != null && !comSpec.isBlank()) ? comSpec : "cmd.exe";
        } else {
            String shell = System.getenv("SHELL");
            return (shell != null && !shell.isBlank()) ? shell : "/bin/sh";
        }
    }

    /**
     * 実行モードおよびOS環境に応じたコマンド実行設定を行います。
     *
     * @param cmdArg コマンド引数文字列
     */
    private void setupCmdExec(String cmdArg) {
        switch (baseDir.getExecModeCode()) {
            case ClsBaseDir.EXEC_MODE_CMD:
                if (MdlApp.isWindows()) {
                    cmdExec.setCmdPath(getDefaultShell());
                    cmdExec.setCmdArgs("/c " + cmdArg);
                } else {
                    cmdExec.setCmdPath(getDefaultShell());
                    cmdExec.setCmdArgs("-c \"" + cmdArg.replace("\"", "\\\"") + "\"");
                }
                break;
            case ClsBaseDir.EXEC_MODE_PS:
                if (MdlApp.isWindows()) {
                    cmdExec.setCmdPath("powershell");
                    cmdExec.setCmdArgs("-NoProfile -command \"" + cmdArg + "; exit $LASTEXITCODE\"");
                } else {
                    cmdExec.setCmdPath("pwsh");
                    cmdExec.setCmdArgs("-NoProfile -Command \"" + cmdArg + "; exit $LASTEXITCODE\"");
                }
                break;
            default:
                cmdExec.setCmdPath(MdlUtil.getRegexTarget(cmdArg, "^(?<TARGET>\\S+)\\s+.*"));
                cmdExec.setCmdArgs(MdlUtil.getRegexTarget(cmdArg, "^\\S+\\s+(?<TARGET>.*)"));
                break;
        }
    }

    /**
     * 指定されたディレクトリまたはファイルを処理します。
     *
     * @param baseDir 処理対象の {@link ClsBaseDir} オブジェクト
     * @return 処理が成功した場合は {@code true}、失敗した場合は {@code false}
     */
    public boolean execute(ClsBaseDir baseDir) {
        this.baseDir = baseDir;

        if (!MdlFile.pathExists(this.baseDir.getPath())) {
            this.baseDir.setReturnCode(this.baseDir.getNaRetCode());
            if (logger != null) {
                logger.writeLine(MdlConst.LVL_NONE, "[SKIP] NO SUCH A DIRECTORY OR FILE");
            }
            return true;
        }

        boolean isOk = true;
        totalDirCount = 0;
        totalFileCount = 0;
        exceptionCount = 0;
        deleteErrorFiles = 0;
        deletedFiles = 0;
        deleteErrorDirs = 0;
        deletedDirs = 0;

        if (this.baseDir.isExec()) {
            titleEn = "DEL";
            titleJp = "削除";
        } else {
            titleEn = "-D-";
            titleJp = "抽出";
        }
        if (this.baseDir.isRmSymlink()) {
            this.baseDir.setSymLink(true);
        }

        // 削除前コマンド
        if (this.baseDir.getPreRmCmd() != null && !this.baseDir.getPreRmCmd().isBlank()) {
            if (this.baseDir.getVerbose() > 3) {
                baseDir.setShowCmd(true);
                baseDir.setShowOutput(true);
                baseDir.setShowExitCode(true);
            }
            cmdExec.setCmdPath(getDefaultShell());
            cmdExec.setShowCmd(this.baseDir.isShowCmd());
            cmdExec.setShowExitCode(this.baseDir.isShowExitCode());
            cmdExec.setShowOutput(this.baseDir.isShowOutput());
            cmdExec.setVerbose(this.baseDir.getVerbose());
            cmdExec.setWarnThreshold(this.baseDir.getWarnThreshold());
            cmdExec.setErrorThreshold(this.baseDir.getErrorThreshold());
            cmdExec.setErrAtNegative(this.baseDir.isErrorAtNegativeValue());
            cmdExec.setAlwaysNormal(this.baseDir.isAlwaysNormal());
            cmdExec.setTimeout(this.baseDir.getTimeout());
            if (this.baseDir.getWorkDir() != null && !this.baseDir.getWorkDir().isBlank()) {
                cmdExec.setWorkDir(this.baseDir.getWorkDir());
            }
            cmdExec.initialize();
        }

        if (appArg.getVerbose() > 0) {
            appArg.printDefinition(this.baseDir);
        }
        if (this.baseDir.isShowCmdParam() && logger != null) {
            logger.writeLine(MdlConst.LVL_NONE, "ALWAYS NORMAL    = " + this.baseDir.isAlwaysNormal() + " ERROR AT NEGATIVE = " + this.baseDir.isErrorAtNegativeValue());
            logger.writeLine(MdlConst.LVL_NONE, "THRESHOLD : WARN = " + this.baseDir.getWarnThreshold() + " / ERROR = " + this.baseDir.getErrorThreshold());
            logger.writeLine(MdlConst.LVL_NONE, "CWD              = " + this.baseDir.getWorkDir());
        }

        // カレントディレクトリ階層チェック
        if (this.baseDir.isBaseDir()) {
            recursive(this.baseDir.getPath(), "", 0, 0);
        } else {
            boolean isSymlink = false;
            if (this.baseDir.isSymLink()) {
                isSymlink = MdlFile.isSymlink(this.baseDir.getPath());
            }
            deleteTarget(this.baseDir.getPath(), MdlFile.PATH_IS_FILE, "", 0, true, isSymlink);
        }

        if (exceptionCount + deleteErrorFiles + deleteErrorDirs > 0) {
            isOk = false;
            this.baseDir.setReturnCode(this.baseDir.getErrRetCode());
        }

        if (appArg.getVerbose() > -2 && logger != null) {
            if (this.baseDir.isExec()) {
                logger.writeLine(MdlConst.LVL_NONE, "総数（DIR=" + totalDirCount + " FILE=" + totalFileCount + "）" + titleJp + "数（DIR=" + deletedDirs + " FILE=" + deletedFiles + "）失敗数（DIR=" + deleteErrorDirs + " FILE=" + deleteErrorFiles + " OTHER=" + exceptionCount + "）");
            } else {
                logger.writeLine(MdlConst.LVL_NONE, "総数（DIR=" + totalDirCount + " FILE=" + totalFileCount + "）" + titleJp + "数（DIR=" + deletedDirs + " FILE=" + deletedFiles + "）");
            }
        }
        return isOk;
    }

    /**
     * 指定されたディレクトリを再帰的に処理します。
     *
     * @param currentPath 現在のパス
     * @param relativePath 相対パス
     * @param currentDepth 現在の深さ
     * @param previousEffective 前回の有効フラグ
     * @return 処理が成功した場合は {@code true}、失敗した場合は {@code false}
     */
    public boolean recursive(String currentPath, String relativePath, long currentDepth, int previousEffective) {
        boolean isOk = true;
        boolean isSymlinkDir = false;
        int currentEffective = previousEffective;
        int checkFilter = 0;

        if (currentDepth >= baseDir.getMinDepth()) {
            if (currentDepth > baseDir.getMaxDepth()) {
                if (baseDir.getVerbose() > 6 && logger != null) {
                    logger.writeLine(MdlConst.LVL_NONE, "RETURN : currentDepth(" + currentDepth + " > baseDir.MaxDepth(" + baseDir.getMaxDepth() + ")");
                }
                return true;
            }

            try {
                if (baseDir.isSymLink()) {
                    isSymlinkDir = MdlFile.isSymlink(currentPath);
                }

                if (baseDir.getVerbose() > 6 && logger != null) {
                    logger.writeLine(MdlConst.LVL_NONE, "■■■[recursive()][ParentDir][" + currentDepth + "] PATH=" + relativePath + " ■■■");
                    logger.writeLine(MdlConst.LVL_NONE, "isSymlinkDir      = " + isSymlinkDir);
                    logger.writeLine(MdlConst.LVL_NONE, "previousEffective     = " + previousEffective);
                    logger.writeLine(MdlConst.LVL_NONE, "IsIncHitRecursive = " + baseDir.isIncHitRecursive());
                    logger.writeLine(MdlConst.LVL_NONE, "IsExcHitRecursive = " + baseDir.isExcHitRecursive());
                    logger.writeLine(MdlConst.LVL_NONE, "IsDirFilterOr     = " + baseDir.isDirFilterOr());
                }

                if (currentDepth == 0) {
                    checkFilter = MdlFile.evalPathFilterCode(relativePath, baseDir.isRegIncBasename(), baseDir.isRegExcBasename(), baseDir.getIncDirsList(), baseDir.getExcDirsList(), baseDir.isDirFilterOr(), baseDir.getVerbose());
                    currentEffective = MdlFile.combineFilterFlags(currentEffective, checkFilter, baseDir.isDirFilterOr(), baseDir.isIncHitRecursive(), baseDir.isExcHitRecursive());
                }

                if (baseDir.getVerbose() > 6 && logger != null) {
                    logger.writeLine(MdlConst.LVL_NONE, "checkFilter      = " + checkFilter);
                    logger.writeLine(MdlConst.LVL_NONE, "currentEffective  = " + currentEffective);
                }

                if (currentDepth > 0 && (currentEffective > 1 && baseDir.isExcHitRecursive())) {
                    return true;
                }

                totalDirCount++;

                // 現在のディレクトリに存在するファイルを処理
                switch (baseDir.getActionCode()) {
                    case ClsBaseDir.ACTION_GEN_DELETE:
                        if (baseDir.isRmDir() && !isSymlinkDir) {
                            execCurDirSubDirs(currentPath, relativePath, currentDepth);
                        }
                        if (baseDir.isRmFile() && !isSymlinkDir) {
                            execCurDirFiles(currentPath, relativePath, currentDepth);
                        }
                        break;
                    default:
                        if (currentEffective == 1) {
                            if (baseDir.isRmDir() && currentDepth > 0 && (!isSymlinkDir || baseDir.isRmSymlink())) {
                                deleteTarget(currentPath, MdlFile.PATH_IS_DIRECTORY, relativePath, currentDepth, false, isSymlinkDir);
                            }
                            if (baseDir.isRmFile() && !isSymlinkDir) {
                                execCurDirFiles(currentPath, relativePath, currentDepth);
                            }
                        }
                        break;
                }
                if (isSymlinkDir) {
                    return isOk;
                }
            } catch (Exception ex) {
                exceptionCount++;
                if (logger != null) {
                    logger.writeLine(MdlConst.LVL_NONE, "[ERR] ClsFind.recursive() 1 : " + ex.getMessage() + " : " + relativePath);
                    if (appArg.isStackTrace()) {
                        logger.writeLine(MdlConst.LVL_NONE, "");
                        for (StackTraceElement elem : ex.getStackTrace()) {
                            logger.writeLine(MdlConst.LVL_NONE, elem.toString());
                        }
                        logger.writeLine(MdlConst.LVL_NONE, "");
                    }
                }
            }
        }

        // 現在のディレクトリに存在するサブディレクトリを処理
        try {
            if (MdlFile.pathExists(currentPath)) {
                String[] subDirectories = MdlFile.getSortedDirectories(currentPath, "*", false, appArg.getSortType(), appArg.isAscending(), appArg.isShowDirList());
                for (String subDirectoryPath : subDirectories) {
                    if (!MdlFile.pathExists(subDirectoryPath)) {
                        continue;
                    }
                    String subDirectoryName = new File(subDirectoryPath).getName();
                    String nextRelativePath = "";
                    if (currentDepth == 0) {
                        nextRelativePath = subDirectoryName;
                    } else {
                        nextRelativePath = (relativePath.isEmpty()) ? subDirectoryName : relativePath + File.separator + subDirectoryName;
                    }

                    boolean isSymlinkSubDir = false;
                    if (baseDir.isSymLink()) {
                        isSymlinkSubDir = MdlFile.isSymlink(subDirectoryPath);
                    }
                    if (baseDir.getVerbose() > 6 && logger != null) {
                        logger.writeLine(MdlConst.LVL_NONE, "===[recursive()][SubDir][" + currentDepth + "] PATH=" + nextRelativePath + " ===");
                    }

                    int checkSubDirFilter = MdlFile.evalPathFilterCode(nextRelativePath, baseDir.isRegIncBasename(), baseDir.isRegExcBasename(), baseDir.getIncDirsList(), baseDir.getExcDirsList(), baseDir.isDirFilterOr(), baseDir.getVerbose());
                    int subDirEffective = MdlFile.combineFilterFlags(currentEffective, checkSubDirFilter, baseDir.isDirFilterOr(), baseDir.isIncHitRecursive(), baseDir.isExcHitRecursive());

                    if (baseDir.getVerbose() > 6 && logger != null) {
                        logger.writeLine(MdlConst.LVL_NONE, "intCheckSubDirFilter  = " + checkSubDirFilter);
                        logger.writeLine(MdlConst.LVL_NONE, "intSubDirEffective = " + subDirEffective);
                    }

                    if ((subDirEffective < 2 || !baseDir.isExcHitRecursive()) && currentDepth < baseDir.getMaxDepth()) {
                        // 再帰処理
                        recursive(subDirectoryPath, nextRelativePath, currentDepth + 1, subDirEffective);
                        // 空ディレクトリ削除
                        if (subDirEffective == 1 && baseDir.isRmEmptyDir() && !isSymlinkSubDir) {
                            rmEmptyDir(subDirectoryPath, relativePath, currentDepth + 1, false);
                        }
                    }
                }

                if (currentEffective == 1 && baseDir.isRmEmptyDir() && !isSymlinkDir) {
                    // 空ディレクトリ削除
                    if (baseDir.isExec()) {
                        rmEmptyDir(currentPath, relativePath, currentDepth, false);
                    }
                }
            }
        } catch (Exception ex) {
            exceptionCount++;
            if (logger != null) {
                logger.writeLine(MdlConst.LVL_NONE, "[ERR] ClsFind.recursive() 2 : " + ex.getMessage() + " : " + relativePath);
                if (appArg.isStackTrace()) {
                    logger.writeLine(MdlConst.LVL_NONE, "");
                    for (StackTraceElement elem : ex.getStackTrace()) {
                        logger.writeLine(MdlConst.LVL_NONE, elem.toString());
                    }
                    logger.writeLine(MdlConst.LVL_NONE, "");
                }
            }
        }
        return isOk;
    }

    /**
     * 現在のディレクトリ内のサブディレクトリを処理します。
     *
     * @param currentPath 現在のディレクトリのパス
     * @param relativePath 相対パス
     * @param currentDepth 現在の深さ
     */
    public void execCurDirSubDirs(String currentPath, String relativePath, long currentDepth) {
        final String methodName = "[ClsFind.execCurDirSubDirs()]";
        if (!MdlFile.pathExists(currentPath)) {
            return;
        }
        baseDir.getTargetList().clear();
        try {
            String[] subDirectories = MdlFile.getSortedDirectories(currentPath, "*", false, appArg.getSortType(), appArg.isAscending(), appArg.isShowDirList());
            for (String subDirectoryPath : subDirectories) {
                if (!MdlFile.isPathFilterMatched(subDirectoryPath, true, true, baseDir.getIncDirsList(), baseDir.getExcDirsList(), baseDir.getVerbose())) {
                    if (baseDir.getVerbose() > 2 && logger != null) {
                        logger.writeLine(MdlConst.LVL_NONE, "[---][D][対象外] " + subDirectoryPath);
                    }
                    continue;
                }
                String subDirectoryName = new File(subDirectoryPath).getName();
                baseDir.getTargetList().add(subDirectoryName);
            }

            // 降順ソート
            Collections.sort(baseDir.getTargetList(), Collections.reverseOrder());

            int index = 0;
            for (String subDirectoryName : baseDir.getTargetList()) {
                String nextDirectoryPath = currentPath + File.separator + subDirectoryName;
                String nextRelativePath = "";

                if (currentDepth == 0) {
                    nextRelativePath = subDirectoryName;
                } else {
                    nextRelativePath = (relativePath.isEmpty()) ? subDirectoryName : relativePath + File.separator + subDirectoryName;
                }

                boolean isSymlinkSubDir = false;
                if (baseDir.isSymLink()) {
                    isSymlinkSubDir = MdlFile.isSymlink(nextRelativePath);
                }

                if (index >= baseDir.getGeneration()) {
                    deleteTarget(nextDirectoryPath, MdlFile.PATH_IS_DIRECTORY, nextRelativePath, currentDepth, false, isSymlinkSubDir);
                } else {
                    if (baseDir.getVerbose() > 0 && logger != null) {
                        logger.writeLine(MdlConst.LVL_NONE, "[---][D][保  持] " + nextDirectoryPath);
                    }
                }
                index++;
            }
        } catch (Exception exception) {
            exceptionCount++;
            if (logger != null) {
                logger.writeLine(MdlConst.LVL_E, methodName + "[EXCEPTION] " + currentPath + " : " + exception.getMessage());
                if (appArg.isStackTrace()) {
                    logger.writeLine(MdlConst.LVL_NONE, "");
                    for (StackTraceElement elem : exception.getStackTrace()) {
                        logger.writeLine(MdlConst.LVL_NONE, elem.toString());
                    }
                    logger.writeLine(MdlConst.LVL_NONE, "");
                }
            }
        }
    }

    /**
     * 指定されたディレクトリ内のファイルを処理します。
     *
     * @param currentPath 現在のディレクトリのパス
     * @param relativePath 相対パス
     * @param currentDepth 現在のディレクトリの深さ
     */
    public void execCurDirFiles(String currentPath, String relativePath, long currentDepth) {
        final String methodName = "[ClsFind.execCurDirFiles()]";
        if (!MdlFile.pathExists(currentPath)) {
            return;
        }

        try {
            String[] filePathList = MdlFile.getSortedFiles(currentPath, "*", false, appArg.getSortType(), appArg.isAscending(), appArg.isShowFileList());

            switch (baseDir.getActionCode()) {
                case ClsBaseDir.ACTION_GEN_DELETE:
                    baseDir.getTargetList().clear();
                    for (String targetFilePath : filePathList) {
                        totalFileCount++;
                        if (!MdlFile.isPathFilterMatched(targetFilePath, true, true, baseDir.getIncFilesList(), baseDir.getExcFilesList(), baseDir.getVerbose())) {
                            if (baseDir.getVerbose() > 2 && logger != null) {
                                logger.writeLine(MdlConst.LVL_NONE, "[---][F][対象外] " + targetFilePath);
                            }
                            continue;
                        }
                        String fileName = new File(targetFilePath).getName();
                        baseDir.getTargetList().add(fileName);
                    }

                    // 降順ソート
                    Collections.sort(baseDir.getTargetList(), Collections.reverseOrder());

                    int index = 0;
                    for (String fileName : baseDir.getTargetList()) {
                        String targetFilePath = currentPath + File.separator + fileName;
                        if (index >= baseDir.getGeneration()) {
                            if (baseDir.getVerbose() > 6 && logger != null) {
                                logger.writeLine(MdlConst.LVL_NONE, "[DEL][F][非保持] " + targetFilePath);
                            }
                            deleteTarget(targetFilePath, MdlFile.PATH_IS_FILE, relativePath, currentDepth, true, false);
                        } else {
                            if (baseDir.getVerbose() > 0 && logger != null) {
                                logger.writeLine(MdlConst.LVL_NONE, "[---][F][保  持] " + targetFilePath);
                            }
                        }
                        index++;
                    }
                    break;
                default:
                    for (String targetFilePath : filePathList) {
                        totalFileCount++;
                        deleteTarget(targetFilePath, MdlFile.PATH_IS_FILE, relativePath, currentDepth, true, false);
                    }
                    break;
            }
        } catch (Exception ex) {
            exceptionCount++;
            if (logger != null) {
                logger.writeLine(MdlConst.LVL_E, methodName + " " + currentPath + " : " + ex.getMessage());
            }
        }
    }

    /**
     * 指定されたディレクトリが空である場合に削除します。
     *
     * @param currentPath 現在のディレクトリのパス
     * @param relativePath 相対パス
     * @param currentDepth 現在の深さ
     * @param isCheckEffective 有効性をチェックするかどうか
     */
    public void rmEmptyDir(String currentPath, String relativePath, long currentDepth, boolean isCheckEffective) {
        try {
            if (MdlFile.PATH_IS_DIRECTORY == MdlFile.getPathType(currentPath) && MdlFile.isEmptyDirectory(currentPath)) {
                deleteTarget(currentPath, MdlFile.PATH_IS_DIRECTORY, relativePath, currentDepth, isCheckEffective, false);
            }
        } catch (Exception ex) {
            deleteErrorDirs++;
            if (baseDir.getVerbose() >= 0 && logger != null) {
                logger.writeLine(MdlConst.LVL_NONE, " => EXCEPTION : " + ex.getMessage());
            }
        }
    }

    /**
     * 指定されたターゲットを削除します。
     *
     * @param targetPath ターゲットのパス
     * @param targetType ターゲットの種類
     * @param relativePath 相対パス
     * @param currentDepth 現在の深さ
     * @param isCheckEffective 有効性をチェックするかどうか
     * @param isSymlink シンボリックリンクかどうか
     */
    public void deleteTarget(String targetPath, int targetType, String relativePath, long currentDepth, boolean isCheckEffective, boolean isSymlink) {
        boolean isEffective = false;
        String displayPath = (baseDir.isDq() ? " \"" + targetPath + "\"" : targetPath);
        String message = "";
        List<String> includesList;
        List<String> excludesList;
        String targetTypeLabel;

        // 存在チェック
        if (!MdlFile.pathExists(targetPath)) {
            if (logger != null) {
                logger.writeLine(MdlConst.LVL_NONE, "[ClsFind.deleteTarget()] NOT FOUND : " + targetPath);
            }
            return;
        }

        // TYPE判定
        switch (targetType) {
            case MdlFile.PATH_IS_FILE:
                includesList = baseDir.getIncFilesList();
                excludesList = baseDir.getExcFilesList();
                targetTypeLabel = "F";
                break;
            default:
                includesList = baseDir.getIncDirsList();
                excludesList = baseDir.getExcDirsList();
                targetTypeLabel = (isSymlink ? "S" : "D");
                break;
        }

        // 削除処理
        try {
            // ファイル名での絞込・除外確認
            if (isCheckEffective) {
                if (!MdlFile.isPathFilterMatched(targetPath, true, true, includesList, excludesList, baseDir.getVerbose())) {
                    return;
                }
            }

            File targetFile = new File(targetPath);
            long lastModMillis = targetFile.lastModified();
            LocalDateTime targetUpdateDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(lastModMillis), ZoneId.systemDefault());

            // 作成日付
            if (baseDir.isCreationTime()) {
                try {
                    Path p = Paths.get(targetPath);
                    BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                    targetUpdateDateTime = LocalDateTime.ofInstant(attrs.creationTime().toInstant(), ZoneId.systemDefault());
                } catch (Exception e) {
                    if (baseDir.getVerbose() > 6 && logger != null) {
                        logger.writeLine(MdlConst.LVL_NONE, "Failed to read creationTime: " + e.getMessage());
                    }
                }
            }

            // ファイル名から日付抽出
            if (baseDir.isDateByName()) {
                String fileName = targetFile.getName();
                String modifiedDate = MdlDate.extractDateFromPath(fileName, true, 19700101);
                LocalDateTime parsedDate = MdlDate.parseDateTime(modifiedDate);
                if (parsedDate != null) {
                    targetUpdateDateTime = parsedDate;
                }
            }

            // 日付判定
            if (baseDir.isTerm()) {
                if (baseDir.getThresholdDate() != null) {
                    if (baseDir.isNew()) {
                        if (targetUpdateDateTime.isAfter(baseDir.getThresholdDate())) {
                            isEffective = true;
                        }
                    } else {
                        if (targetUpdateDateTime.isBefore(baseDir.getThresholdDate())) {
                            isEffective = true;
                        }
                    }
                } else {
                    isEffective = true;
                }
            } else {
                isEffective = true;
            }

            // 削除対象の場合
            if (isEffective) {
                if (baseDir.getVerbose() >= 0) {
                    message = "[" + titleEn + "][" + targetTypeLabel + "][" + MdlDate.getFormattedDate(targetUpdateDateTime, "yyyy/MM/dd HH:mm:ss") + "] " + displayPath;
                } else {
                    message = displayPath;
                }

                LocalDateTime timestamp = LocalDateTime.now();

                switch (baseDir.getDateTimeMode()) {
                    case ClsBaseDir.DATETIME_TODAY:
                        timestamp = LocalDate.now().atStartOfDay();
                        break;
                    case ClsBaseDir.DATETIME_YESTERDAY:
                        timestamp = LocalDate.now().minusDays(1).atStartOfDay();
                        break;
                    case ClsBaseDir.DATETIME_FILEINFO:
                        timestamp = targetUpdateDateTime;
                        break;
                    default:
                        break;
                }

                // 実行フラグが立っている場合
                if (baseDir.isExec()) {
                    // ディレクトリ作成が指定された場合
                    if (baseDir.isMkDir()) {
                        String cmdArg = MdlFile.replacePathForCmd(baseDir.getMkdirPath(), targetPath, baseDir.getPath(), relativePath, baseDir.isDq(), baseDir.getVerbose(), timestamp);
                        if (!MdlFile.pathExists(cmdArg)) {
                            if (baseDir.getVerbose() > 1 && logger != null) {
                                logger.writeLine(MdlConst.LVL_NONE, "[NEW][" + targetTypeLabel + "][" + MdlDate.getFormattedDate(targetUpdateDateTime, "yyyy/MM/dd HH:mm:ss") + "] mkdir " + cmdArg);
                            }
                            MdlFile.createDirectory(cmdArg);
                        }
                    }

                    // タイムスタンプ設定先存在時事前削除が指定された場合
                    if (baseDir.isPreRmFile() && !baseDir.getSetDateTimeTo().isBlank() && MdlFile.pathExists(baseDir.getSetDateTimeTo())) {
                        if (baseDir.getVerbose() > 2 && logger != null) {
                            logger.writeLine(MdlConst.LVL_NONE, " -> RM -F " + baseDir.getSetDateTimeTo());
                        }
                        MdlFile.deleteRecursively(baseDir.getSetDateTimeTo());
                    }

                    // コマンド実行が指定された場合
                    if (baseDir.isPreRmCmd()) {
                        String cmdArg = MdlFile.replacePathForCmd(baseDir.getPreRmCmd(), targetPath, baseDir.getPath(), relativePath, baseDir.isDq(), baseDir.getVerbose(), timestamp);
                        setupCmdExec(cmdArg);
                        if (baseDir.getVerbose() >= 0) {
                            message = "[" + titleEn + "][" + targetTypeLabel + "][" + MdlDate.getFormattedDate(targetUpdateDateTime, "yyyy/MM/dd HH:mm:ss") + "] " + cmdArg;
                        }
                        if (!message.isEmpty() && logger != null) {
                            logger.writeLine(MdlConst.LVL_NONE, message);
                        }

                        // 削除前コマンド実行
                        if (cmdExec.executeThread(baseDir.getPriority()) != 0) {
                            // 削除前コマンド実行に失敗した場合
                            switch (targetType) {
                                case MdlFile.PATH_IS_FILE:
                                    deleteErrorFiles++;
                                    break;
                                default:
                                    deleteErrorDirs++;
                                    break;
                            }
                            if (baseDir.getVerbose() >= 0 && logger != null) {
                                logger.writeLine(MdlConst.LVL_NONE, " => ERROR : Cmd Return Code != 0 : " + cmdExec.getCmdPath() + " " + cmdExec.getCmdArgs());
                                logger.writeLine(MdlConst.LVL_NONE, " => SKIP  : DELETE : " + targetPath);
                            }
                            return;
                        } else {
                            // 削除前コマンド実行に成功した場合
                            // タイムスタンプの設定
                            if (baseDir.isSetDateTime()) {
                                try {
                                    String setToPath = MdlFile.replacePathForCmd(baseDir.getSetDateTimeTo(), targetPath, baseDir.getPath(), relativePath, baseDir.isDq(), baseDir.getVerbose(), timestamp);
                                    switch (MdlFile.getPathType(setToPath)) {
                                        case MdlFile.PATH_IS_DIRECTORY:
                                            if (baseDir.getVerbose() > 2 && logger != null) {
                                                logger.writeLine(MdlConst.LVL_NONE, " -> SET TIMESTAMP(" + targetTypeLabel + ")：" + MdlDate.getFormattedDate(timestamp, "yyyy/MM/dd HH:mm:ss"));
                                            }
                                            MdlFile.setDateToDirMain(setToPath, timestamp, 7, false, true);
                                            break;
                                        case MdlFile.PATH_IS_FILE:
                                            if (baseDir.getVerbose() > 2 && logger != null) {
                                                logger.writeLine(MdlConst.LVL_NONE, " -> SET TIMESTAMP(" + targetTypeLabel + ")：" + MdlDate.getFormattedDate(timestamp, "yyyy/MM/dd HH:mm:ss"));
                                            }
                                            MdlFile.setDateToFileMain(setToPath, timestamp, 7, false, true);
                                            break;
                                        default:
                                            break;
                                    }
                                } catch (Exception e) {
                                    if (baseDir.getVerbose() > 6 && logger != null) {
                                        logger.writeLine(MdlConst.LVL_NONE, "Failed to set timestamp: " + e.getMessage());
                                    }
                                }
                            }
                            // 削除実行
                            if (MdlFile.pathExists(targetPath)) {
                                if (baseDir.isRm()) {
                                    if (baseDir.getVerbose() > 2 && logger != null) {
                                        logger.writeLine(MdlConst.LVL_NONE, " -> TRY DELETE");
                                    }
                                    if (!MdlFile.deleteRecursively(targetPath, baseDir.getVerbose())) {
                                        switch (targetType) {
                                            case MdlFile.PATH_IS_FILE:
                                                deleteErrorFiles++;
                                                break;
                                            default:
                                                deleteErrorDirs++;
                                                break;
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // コマンド実行が指定されなかった場合
                        if (!message.isEmpty() && logger != null) {
                            logger.writeLine(MdlConst.LVL_NONE, message);
                        }
                        if (baseDir.isRm()) {
                            if (!MdlFile.deleteRecursively(targetPath, baseDir.getVerbose())) {
                                switch (targetType) {
                                    case MdlFile.PATH_IS_FILE:
                                        deleteErrorFiles++;
                                        break;
                                    default:
                                        deleteErrorDirs++;
                                        break;
                                }
                            }
                        }
                    }
                } else {
                    // DRYRUN
                    if (baseDir.isMkDir()) {
                        if (baseDir.getVerbose() > 1) {
                            String cmdArg = MdlFile.replacePathForCmd(baseDir.getMkdirPath(), targetPath, baseDir.getPath(), relativePath, baseDir.isDq(), baseDir.getVerbose());
                            if (!MdlFile.pathExists(cmdArg) && logger != null) {
                                logger.writeLine(MdlConst.LVL_NONE, "[-N-][" + targetTypeLabel + "][" + MdlDate.getFormattedDate(targetUpdateDateTime, "yyyy/MM/dd HH:mm:ss") + "] mkdir " + cmdArg);
                            }
                        }
                    }
                    if (baseDir.isPreRmFile() && !baseDir.getSetDateTimeTo().isBlank() && MdlFile.pathExists(baseDir.getSetDateTimeTo())) {
                        if (logger != null) {
                            logger.writeLine(MdlConst.LVL_NONE, "[-D-][" + targetTypeLabel + "][" + MdlDate.getFormattedDate(targetUpdateDateTime, "yyyy/MM/dd HH:mm:ss") + "] RM -F " + baseDir.getSetDateTimeTo());
                        }
                    }
                    if (baseDir.isPreRmCmd()) {
                        if (baseDir.getVerbose() >= 0) {
                            String cmdArg = MdlFile.replacePathForCmd(baseDir.getPreRmCmd(), targetPath, baseDir.getPath(), relativePath, baseDir.isDq(), baseDir.getVerbose());
                            setupCmdExec(cmdArg);
                            message = "[" + titleEn + "][" + targetTypeLabel + "][" + MdlDate.getFormattedDate(targetUpdateDateTime, "yyyy/MM/dd HH:mm:ss") + "] " + cmdExec.getCmdPath() + " " + cmdExec.getCmdArgs();
                        }
                    }
                    if (!message.isEmpty() && logger != null) {
                        logger.writeLine(MdlConst.LVL_NONE, message);
                    }
                }
                switch (targetType) {
                    case MdlFile.PATH_IS_FILE:
                        deletedFiles++;
                        break;
                    default:
                        deletedDirs++;
                        break;
                }
            } else {
                // 削除対象外の場合
                if (!baseDir.isDiff()) {
                    if (baseDir.getVerbose() >= 0) {
                        message = "[---][" + targetTypeLabel + "][" + MdlDate.getFormattedDate(targetUpdateDateTime, "yyyy/MM/dd HH:mm:ss") + "] " + displayPath;
                    }
                }
                if (!message.isEmpty() && logger != null) {
                    logger.writeLine(MdlConst.LVL_NONE, message);
                }
            }
        } catch (Exception ex) {
            switch (targetType) {
                case MdlFile.PATH_IS_FILE:
                    deleteErrorFiles++;
                    break;
                default:
                    deleteErrorDirs++;
                    break;
            }
            if (baseDir.getVerbose() >= 0 && logger != null) {
                logger.writeLine(MdlConst.LVL_NONE, " => EXCEPTION : " + ex.getMessage());
                if (appArg.isStackTrace()) {
                    logger.writeLine(MdlConst.LVL_NONE, "");
                    for (StackTraceElement elem : ex.getStackTrace()) {
                        logger.writeLine(MdlConst.LVL_NONE, elem.toString());
                    }
                    logger.writeLine(MdlConst.LVL_NONE, "");
                }
            }
        }
    }
}
