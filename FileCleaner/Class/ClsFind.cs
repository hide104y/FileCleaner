using System;
using System.Text.RegularExpressions;
using System.Collections.Generic;
using CmnClsLib.Class;
using CmnClsLib.Module;
using static System.Runtime.InteropServices.JavaScript.JSType;

// 2026/08/08 Gemini 3.6 Flash (High) Review & Modified

namespace FileCleaner.Class
{
    public class ClsFind
    {
        private ClsLogger _logger;
        private ClsAppArg _appArg;
        private ClsBaseDir _baseDir;
        private ClsCmdExec _cmdExec;
        private ulong _totalDirCount = 0;
        private ulong _totalFileCount = 0;
        private ulong _deleteErrorFiles = 0;
        private ulong _deletedFiles = 0;
        private ulong _deleteErrorDirs = 0;
        private ulong _deletedDirs = 0;
        private ulong _exceptionCount = 0;
        private string _titleEn = "DEL";
        private string _titleJp = "削除";
        
        /// <summary>
        /// ClsFind クラスのコンストラクタ
        /// </summary>
        /// <param name="logger">ログ出力用のオブジェクト</param>
        /// <param name="objParam">パラメータ管理用のオブジェクト</param>
        public ClsFind(ClsLogger logger, ClsAppArg objParam)
        {
            _logger = logger;
            _appArg = objParam;
            _cmdExec = new(logger);
            _baseDir = new();
        }

        /// <summary>
        /// 指定されたディレクトリまたはファイルを処理します。
        /// </summary>
        /// <param name="baseDir">処理対象のディレクトリまたはファイル</param>
        /// <returns>処理が成功した場合はtrue、失敗した場合はfalse</returns>
        public bool Execute(ClsBaseDir baseDir)
        {
            _baseDir = baseDir;

            if (!MdlFile.PathExists(_baseDir.Path))
            {
                _baseDir.ReturnCode = _baseDir.NaRetCode;
                _logger.WriteLine(MdlConst.LVL_NONE, "[SKIP] NO SUCH A DIRECTORY OR FILE");
                return true;
            }

            bool isOk = true;
            _totalDirCount = 0;
            _totalFileCount = 0;
            _exceptionCount = 0;
            _deleteErrorFiles = 0;
            _deletedFiles = 0;
            _deleteErrorDirs = 0;
            _deletedDirs = 0;

            if (_baseDir.IsExec)
            {
                _titleEn = "DEL";
                _titleJp = "削除";
            }
            else
            {
                _titleEn = "-D-";
                _titleJp = "抽出";
            }
            if (_baseDir.IsRmSymlink) _baseDir.IsSymLink = true;

            // 削除前コマンド
            if (!string.IsNullOrEmpty(_baseDir.PreRmCmd))
            {
                if (_baseDir.Verbose > 3)
                {
                    baseDir.IsShowCmd = true;
                    baseDir.IsShowOutput = true;
                    baseDir.IsShowExitCode = true;
                }
                _cmdExec.CmdPath = System.Environment.GetEnvironmentVariable("ComSpec") ?? "cmd";
                _cmdExec.IsShowCmd = _baseDir.IsShowCmd;
                _cmdExec.IsShowExitCode = _baseDir.IsShowExitCode;
                _cmdExec.IsShowOutput = _baseDir.IsShowOutput;
                _cmdExec.Verbose = _baseDir.Verbose;
                _cmdExec.WarnThreshold = _baseDir.WarnThreshold;
                _cmdExec.ErrorThreshold = _baseDir.ErrorThreshold;
                _cmdExec.IsErrorAtNegativeValue = _baseDir.IsErrorAtNegativeValue;
                _cmdExec.IsAlwaysNormal = _baseDir.IsAlwaysNormal;
                _cmdExec.Timeout = _baseDir.Timeout;
                if (!string.IsNullOrEmpty(_baseDir.WorkDir)) _cmdExec.WorkDir = _baseDir.WorkDir;
                _cmdExec.Initialize();
            }

            if (_appArg.Verbose > 0) { _appArg.PrintDefinition(_baseDir); }
            if (_baseDir.IsShowCmdParam)
            {
                _logger.WriteLine(MdlConst.LVL_NONE, "ALWAYS NORMAL    = " + _baseDir.IsAlwaysNormal + " ERROR AT NEGATIVE = " + _baseDir.IsErrorAtNegativeValue);
                _logger.WriteLine(MdlConst.LVL_NONE, "THRESHOLD : WARN = " + _baseDir.WarnThreshold + " / ERROR = " + _baseDir.ErrorThreshold);
                _logger.WriteLine(MdlConst.LVL_NONE, "CWD              = " + _baseDir.WorkDir);
            }

            // カレントディレクトリ階層チェック
            if (_baseDir.IsBaseDir)
            {
                Recursive(_baseDir.Path, "", 0, 0);
            }
            else
            {
                bool isSymlink = false;
                if (_baseDir.IsSymLink) isSymlink = MdlFile.IsSymlink(_baseDir.Path);
                DeleteTarget(_baseDir.Path, MdlFile.PATH_IS_FILE, "", 0, true, isSymlink);
            }

            if (_exceptionCount + _deleteErrorFiles + _deleteErrorDirs > 0)
            {
                isOk = false;
                _baseDir.ReturnCode = _baseDir.ErrRetCode;
            }

            if (_appArg.Verbose > -2)
            {
                if (_baseDir.IsExec)
                {
                    _logger.WriteLine(MdlConst.LVL_NONE, "総数（DIR=" + _totalDirCount + " FILE=" + _totalFileCount + "）" + _titleJp + "数（DIR=" + _deletedDirs.ToString() + " FILE=" + _deletedFiles.ToString() + "）失敗数（DIR=" + _deleteErrorDirs + " FILE=" + _deleteErrorFiles + " OTHER=" + _exceptionCount + "）");
                }
                else
                {
                    _logger.WriteLine(MdlConst.LVL_NONE, "総数（DIR=" + _totalDirCount + " FILE=" + _totalFileCount + "）" + _titleJp + "数（DIR=" + _deletedDirs.ToString() + " FILE=" + _deletedFiles.ToString() + "）");
                }
            }
            return isOk;
        }

        /// <summary>
        /// 指定されたディレクトリを再帰的に処理します。
        /// </summary>
        /// <param name="currentPath">現在のパス</param>
        /// <param name="relativePath">相対パス</param>
        /// <param name="currentDepth">現在の深さ</param>
        /// <param name="previousEffective">前回の有効フラグ</param>
        /// <returns>処理が成功した場合はtrue、失敗した場合はfalse</returns>
        public bool Recursive(string currentPath, string relativePath, ulong currentDepth, int previousEffective)
        {
            bool isOk = true;
            bool isSymlinkDir = false;
            int currentEffective = previousEffective;
            int checkFilter = 0;

            if (currentDepth >= _baseDir.MinDepth)
            {
                if (currentDepth > _baseDir.MaxDepth)
                {
                    if (_baseDir.Verbose > 6)
                    {
                        _logger.WriteLine(MdlConst.LVL_NONE, "RETURN : currentDepth(" + currentDepth + " > _baseDir.UlngMax(" + _baseDir.MaxDepth + ")");
                    }
                    return true;
                }

                try
                {
                    if (_baseDir.IsSymLink) isSymlinkDir = MdlFile.IsSymlink(currentPath);

                    if (_baseDir.Verbose > 6)
                    {
                        _logger.WriteLine(MdlConst.LVL_NONE, "■■■[recursive()][ParentDir][" + currentDepth + "] PATH=" + relativePath + " ■■■");
                        _logger.WriteLine(MdlConst.LVL_NONE, "isSymlinkDir      = " + isSymlinkDir);
                        _logger.WriteLine(MdlConst.LVL_NONE, "previousEffective     = " + previousEffective);
                        _logger.WriteLine(MdlConst.LVL_NONE, "IsIncHitRecursive = " + _baseDir.IsIncHitRecursive);
                        _logger.WriteLine(MdlConst.LVL_NONE, "IsExcHitRecursive = " + _baseDir.IsExcHitRecursive);
                        _logger.WriteLine(MdlConst.LVL_NONE, "IsDirFilterOr     = " + _baseDir.IsDirFilterOr);
                    }

                    if (0 == currentDepth)
                    {
                        checkFilter = MdlFile.EvaluatePathFilterCode(relativePath, _baseDir.IsRegIncBasename, _baseDir.IsRegExcBasename, _baseDir.IncDirsList, _baseDir.ExcDirsList, _baseDir.IsDirFilterOr, _baseDir.Verbose);
                        currentEffective = MdlFile.CombineFilterFlags(currentEffective, checkFilter, _baseDir.IsDirFilterOr, _baseDir.IsIncHitRecursive, _baseDir.IsExcHitRecursive);
                    }

                    if (_baseDir.Verbose > 6)
                    {
                        _logger.WriteLine(MdlConst.LVL_NONE, "checkFilter      = " + checkFilter);
                        _logger.WriteLine(MdlConst.LVL_NONE, "currentEffective  = " + currentEffective);
                    }

                    if (currentDepth > 0 && (currentEffective > 1 && _baseDir.IsExcHitRecursive))
                    {
                        return true;
                    }

                    _totalDirCount++;

                    // 現在のディレクトリに存在するファイルを処理
                    switch (_baseDir.ActionCode)
                    {
                        case ClsBaseDir.ACTION_GEN_DELETE:
                            if (_baseDir.IsRmDir && !isSymlinkDir)
                            {
                                ExecCurDirSubDirs(currentPath, relativePath, currentDepth);
                            }
                            if (_baseDir.IsRmFile && !isSymlinkDir)
                            {
                                ExecCurDirFiles(currentPath, relativePath, currentDepth);
                            }
                            break;
                        default:
                            if (1 == currentEffective)
                            {
                                if (_baseDir.IsRmDir && currentDepth > 0 && (!isSymlinkDir || _baseDir.IsRmSymlink))
                                {
                                    DeleteTarget(currentPath, MdlFile.PATH_IS_DIRECTORY, relativePath, currentDepth, false, isSymlinkDir);
                                }
                                if (_baseDir.IsRmFile && !isSymlinkDir)
                                {
                                    ExecCurDirFiles(currentPath, relativePath, currentDepth);
                                }
                            }
                            break;
                    }
                    if (isSymlinkDir) return isOk;
                }
                catch (Exception objExcptn)
                {
                    _exceptionCount++;
                    _logger.WriteLine(MdlConst.LVL_NONE, "[ERR] ClsFind.recursive() 1 : " + objExcptn.Message + " : " + relativePath);
                    if (_appArg.IsStackTrace)
                    {
                        _logger.WriteLine(MdlConst.LVL_NONE, "");
                        _logger.WriteLine(MdlConst.LVL_NONE, objExcptn.StackTrace ?? "");
                        _logger.WriteLine(MdlConst.LVL_NONE, "");
                    }
                }
            }

            // 現在のディレクトリに存在するサブディレクトリを処理
            try
            {
                if (MdlFile.PathExists(currentPath))
                {
                    foreach (string subDirectoryPath in MdlFile.GetSortedDirectories(currentPath, "*", System.IO.SearchOption.TopDirectoryOnly, _appArg.SortType, _appArg.IsAscending, _appArg.IsShowDirList))
                    {
                        if (!MdlFile.PathExists(subDirectoryPath)) continue;
                        string subDirectoryName = System.IO.Path.GetFileName(subDirectoryPath);
                        string nextRelativePath = "";
                        if (0 == currentDepth)
                        {
                            nextRelativePath = subDirectoryName;
                        }
                        else
                        {
                            nextRelativePath = System.IO.Path.Combine(relativePath, subDirectoryName);
                        }

                        bool isSymlinkSubDir = false;

                        if (_baseDir.IsSymLink) isSymlinkSubDir = MdlFile.IsSymlink(subDirectoryPath);
                        if (_baseDir.Verbose > 6)
                        {
                            _logger.WriteLine(MdlConst.LVL_NONE, "===[recursive()][SubDir][" + currentDepth + "] PATH=" + nextRelativePath + " ===");
                        }

                        int intCheckSubDirFilter = MdlFile.EvaluatePathFilterCode(nextRelativePath, _baseDir.IsRegIncBasename, _baseDir.IsRegExcBasename, _baseDir.IncDirsList, _baseDir.ExcDirsList, _baseDir.IsDirFilterOr, _baseDir.Verbose);
                        int intSubDirEffective = MdlFile.CombineFilterFlags(currentEffective, intCheckSubDirFilter, _baseDir.IsDirFilterOr, _baseDir.IsIncHitRecursive, _baseDir.IsExcHitRecursive);

                        if (_baseDir.Verbose > 6)
                        {
                            _logger.WriteLine(MdlConst.LVL_NONE, "intCheckSubDirFilter  = " + intCheckSubDirFilter);
                            _logger.WriteLine(MdlConst.LVL_NONE, "intSubDirEffective = " + intSubDirEffective);
                        }

                        if ((2 > intSubDirEffective || !_baseDir.IsExcHitRecursive) && currentDepth < _baseDir.MaxDepth)
                        {
                            // 再帰処理
                            Recursive(subDirectoryPath, nextRelativePath, currentDepth + 1, intSubDirEffective);
                            // 空ディレクトリ削除
                            if (1 == intSubDirEffective && _baseDir.IsRmEmptyDir && !isSymlinkSubDir) RmEmptyDir(subDirectoryPath, relativePath, currentDepth + 1, false);
                        }
                    }

                    if (1 == currentEffective && _baseDir.IsRmEmptyDir && !isSymlinkDir)
                    {
                        // 空ディレクトリ削除
                        if (_baseDir.IsExec) RmEmptyDir(currentPath, relativePath, currentDepth, false);
                    }
                }
            }
            catch (Exception objExcptn)
            {
                _exceptionCount++;
                _logger.WriteLine(MdlConst.LVL_NONE, "[ERR] ClsFind.recursive() 2 : " + objExcptn.Message + " : " + relativePath);
                if (_appArg.IsStackTrace)
                {
                    _logger.WriteLine(MdlConst.LVL_NONE, "");
                    _logger.WriteLine(MdlConst.LVL_NONE, objExcptn.StackTrace ?? "");
                    _logger.WriteLine(MdlConst.LVL_NONE, "");
                }
            }
            return isOk;
        }

        /// <summary>
        /// 現在のディレクトリ内のサブディレクトリを処理します。
        /// </summary>
        /// <param name="currentPath">現在のディレクトリのパス</param>
        /// <param name="relativePath">相対パス</param>
        /// <param name="currentDepth">現在の深さ</param>
        public void ExecCurDirSubDirs(string currentPath, string relativePath, ulong currentDepth)
        {
            const string METHOD_NAME = "[ClsFind.ExecCurDirSubDirs()]";
            if (!MdlFile.PathExists(currentPath)) return;
            _baseDir.TargetList.Clear();
            try
            {
                // カレントディレクトリのサブディレクトリを１件毎に処理
                foreach (string subDirectoryPath in MdlFile.GetSortedDirectories(currentPath, "*", System.IO.SearchOption.TopDirectoryOnly, _appArg.SortType, _appArg.IsAscending, _appArg.IsShowDirList))
                {
                    if (!MdlFile.IsPathFilterMatched(subDirectoryPath, true, true, _baseDir.IncDirsList, _baseDir.ExcDirsList, _baseDir.Verbose))
                    {
                        if (_baseDir.Verbose > 2)
                        {
                            _logger.WriteLine(MdlConst.LVL_NONE, "[---][D][対象外] " + subDirectoryPath);
                        }
                        continue;
                    }
                    string subDirectoryName = System.IO.Path.GetFileName(subDirectoryPath);
                    _baseDir.TargetList.Add(subDirectoryName);
                }

                // 降順ソート
                _baseDir.TargetList.Sort((a, b) => b.CompareTo(a));

                int index = 0;
                foreach (string subDirectoryName in _baseDir.TargetList)
                {
                    string nextDirectoryPath = System.IO.Path.Combine(currentPath, subDirectoryName);
                    string nextRelativePath = "";

                    if (0 == currentDepth)
                    {
                        nextRelativePath = subDirectoryName;
                    }
                    else
                    {
                        nextRelativePath = System.IO.Path.Combine(relativePath, subDirectoryName);
                    }

                    bool isSymlinkSubDir = false;
                    if (_baseDir.IsSymLink) isSymlinkSubDir = MdlFile.IsSymlink(nextRelativePath);

                    if (index >= _baseDir.Generation)
                    {
                        DeleteTarget(nextDirectoryPath, MdlFile.PATH_IS_DIRECTORY, nextRelativePath, currentDepth, false, isSymlinkSubDir);
                    }
                    else
                    {
                        if (_baseDir.Verbose > 0)
                        {
                            _logger.WriteLine(MdlConst.LVL_NONE, "[---][D][保  持] " + nextDirectoryPath);
                        }
                    }

                    index++;
                }
            }
            catch (Exception exception)
            {
                _exceptionCount++;
                _logger.WriteLine(MdlConst.LVL_E, METHOD_NAME + "[EXCEPTION] " + currentPath + " : " + exception.Message);
                if (_appArg.IsStackTrace)
                {
                    _logger.WriteLine(MdlConst.LVL_NONE, "");
                    _logger.WriteLine(MdlConst.LVL_NONE, exception.StackTrace ?? "");
                    _logger.WriteLine(MdlConst.LVL_NONE, "");
                }
            }
            return;
        }

        /// <summary>
        /// 指定されたディレクトリ内のファイルを処理します。
        /// </summary>
        /// <param name="currentPath">現在のディレクトリのパス</param>
        /// <param name="relativePath">相対パス</param>
        /// <param name="currentDepth">現在のディレクトリの深さ</param>
        public void ExecCurDirFiles(string currentPath, string relativePath, ulong currentDepth)
        {
            const string METHOD_NAME = "[ClsFind.ExecCurDirFiles()]";
            string[] filePathList = Array.Empty<string>();
            // 存在チェック
            if (!MdlFile.PathExists(currentPath)) return;
            // カレントディレクトリのファイル名の一覧を取得
            try
            {
                // カレントディレクトリのファイルパス一覧を取得
                filePathList = MdlFile.GetSortedFiles(currentPath, "*", System.IO.SearchOption.TopDirectoryOnly, _appArg.SortType, _appArg.IsAscending, _appArg.IsShowFileList);
                // 処理
                switch (_baseDir.ActionCode)
                {
                    // 世代保存削除
                    case ClsBaseDir.ACTION_GEN_DELETE:
                        // リスト初期化
                        _baseDir.TargetList.Clear();
                        // ファイル一覧のファイルを１件毎に処理
                        foreach (string targetFilePath in filePathList)
                        {
                            _totalFileCount++;
                            // ファイル名での絞込・除外確認
                            if (!MdlFile.IsPathFilterMatched(targetFilePath, true, true, _baseDir.IncFilesList, _baseDir.ExcFilesList, _baseDir.Verbose))
                            {
                                if (_baseDir.Verbose > 2)
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, "[---][F][対象外] " + targetFilePath);
                                }
                                continue;
                            }
                            // ファイル名の取得
                            string fileName = System.IO.Path.GetFileName(targetFilePath);
                            // リスト追加
                            _baseDir.TargetList.Add(fileName);
                        }
                        // 降順ソート
                        _baseDir.TargetList.Sort((a, b) => b.CompareTo(a));
                        // ループ処理
                        int index = 0;
                        foreach (string fileName in _baseDir.TargetList)
                        {
                            string targetFilePath = System.IO.Path.Combine(currentPath, fileName);
                            if (index >= _baseDir.Generation)
                            {
                                if (_baseDir.Verbose > 6)
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, "[DEL][F][非保持] " + targetFilePath);
                                }
                                DeleteTarget(targetFilePath, MdlFile.PATH_IS_FILE, relativePath, currentDepth, true, false);
                            }
                            else
                            {
                                if (_baseDir.Verbose > 0)
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, "[---][F][保  持] " + targetFilePath);
                                }
                            }
                            index++;
                        }
                        break;
                    // 通常削除
                    default:
                        foreach (string targetFilePath in filePathList)
                        {
                            _totalFileCount++;
                            DeleteTarget(targetFilePath, MdlFile.PATH_IS_FILE, relativePath, currentDepth, true, false);
                        }
                        break;
                }
            }
            catch (Exception ex)
            {
                _exceptionCount++;
                _logger.WriteLine(MdlConst.LVL_E, METHOD_NAME + " " + currentPath + " : " + ex.Message);
            }
            return;
        }

        /// <summary>
        /// 指定されたディレクトリが空である場合に削除します。
        /// </summary>
        /// <param name="currentPath">現在のディレクトリのパス</param>
        /// <param name="relativePath">相対パス</param>
        /// <param name="currentDepth">現在の深さ</param>
        /// <param name="isCheckEffective">有効性をチェックするかどうか</param>
        public void RmEmptyDir(string currentPath, string relativePath, ulong currentDepth, bool isCheckEffective)
        {
            string displayPath = (_baseDir.IsDq ? " \"" + currentPath + "\"" : currentPath);
            try
            {
                if (MdlFile.PATH_IS_DIRECTORY == MdlFile.GetPathType(currentPath) && MdlFile.IsEmptyDirectory(currentPath))
                {
                    DeleteTarget(currentPath, MdlFile.PATH_IS_DIRECTORY, relativePath, currentDepth, isCheckEffective, false);
                }
            }
            catch (Exception ex)
            {
                _deleteErrorDirs++;
                if (_baseDir.Verbose >= 0)
                {
                    _logger.WriteLine(MdlConst.LVL_NONE, " => EXCEPTION : " + ex.Message);
                }
            }
            return;
        }

        /// <summary>
        /// 指定されたターゲットを削除します。
        /// </summary>
        /// <param name="targetPath">ターゲットのパス</param>
        /// <param name="targetType">ターゲットの種類</param>
        /// <param name="relativePath">相対パス</param>
        /// <param name="currentDepth">現在の深さ</param>
        /// <param name="isCheckEffective">有効性をチェックするかどうか</param>
        /// <param name="isSymlink">シンボリックリンクかどうか</param>
        public void DeleteTarget(string targetPath, int targetType, string relativePath, ulong currentDepth, bool isCheckEffective, bool isSymlink)
        {
            bool isEffective = false;
            string displayPath = (_baseDir.IsDq ? " \"" + targetPath + "\"" : targetPath);
            string message = "";
            List<string> includesList = [];
            List<string> excludesList = [];
            string strType = "";

            // 存在チェック
            if (!MdlFile.PathExists(targetPath))
            {
                _logger.WriteLine(MdlConst.LVL_NONE, "[ClsFind.DeleteTarget()] NOT FOUND : " + targetPath);
                return;
            }

            // TYPE判定
            switch (targetType)
            {
                case MdlFile.PATH_IS_FILE:
                    includesList = _baseDir.IncFilesList;
                    excludesList = _baseDir.ExcFilesList;
                    strType = "F";
                    break;
                default:
                    includesList = _baseDir.IncDirsList;
                    excludesList = _baseDir.ExcDirsList;
                    strType = (isSymlink ? "S" : "D");
                    break;
            }

            // 削除処理
            try
            {
                // ファイル名での絞込・除外確認
                if (isCheckEffective)
                {
                    if (!MdlFile.IsPathFilterMatched(targetPath, true, true, includesList, excludesList, _baseDir.Verbose))
                    {
                        return;
                    }
                }

                // ファイルの更新日取得
                // 更新日付
                DateTime dtTargetUpdate = (MdlFile.PATH_IS_FILE == targetType ? System.IO.File.GetLastWriteTime(targetPath) : System.IO.Directory.GetLastWriteTime(targetPath));
                // 作成日付
                if (_baseDir.IsCreationTime)
                {
                    dtTargetUpdate = (MdlFile.PATH_IS_FILE == targetType ? System.IO.File.GetCreationTime(targetPath) : System.IO.Directory.GetCreationTime(targetPath));
                }
                // カレントファイル名の先頭から順番に日付を検索・取得
                if (_baseDir.IsDateByName)
                {
                    string strFileName = System.IO.Path.GetFileName(targetPath);
                    string strModifiedDate = MdlDate.ExtractDateFromPath(strFileName, true, 19700101);
                    if (MdlDate.TryParseDateTime(strModifiedDate, out DateTime dtTmp))
                    {
                        dtTargetUpdate = dtTmp;
                    }
                }

                // 日付判定
                if (_baseDir.IsTerm)
                {
                    if (_baseDir.IsNew)
                    {
                        if (dtTargetUpdate > _baseDir.ThresholdDate) isEffective = true;
                    }
                    else
                    {
                        if (dtTargetUpdate < _baseDir.ThresholdDate) isEffective = true;
                    }
                }
                else
                {
                    isEffective = true;
                }

                // 削除対象の場合
                if (isEffective)
                {
                    if (_baseDir.Verbose >= 0)
                    {
                        message = "[" + _titleEn + "][" + strType + "][" + MdlDate.GetFormattedDate(dtTargetUpdate, "yyyy/MM/dd HH:mm:ss") + "] " + displayPath;
                    }
                    else
                    {
                        message = displayPath;
                    }

                    DateTime dtTimestamp = DateTime.Now;

                    switch (_baseDir.DateTimeMode)
                    {
                        case ClsBaseDir.DATETIME_TODAY:
                            dtTimestamp = DateTime.Today;
                            break;
                        case ClsBaseDir.DATETIME_YESTERDAY:
                            dtTimestamp = DateTime.Today.AddDays(-1);
                            break;
                        case ClsBaseDir.DATETIME_FILEINFO:
                            dtTimestamp = dtTargetUpdate;
                            break;
                    }

                    // 実行フラグが立っている場合
                    if (_baseDir.IsExec)
                    {
                        // ディレクトリ作成が指定された場合
                        if (_baseDir.IsMkDir)
                        {
                            string strArg = MdlFile.ReplacePathForCmdExec(_baseDir.MkdirPath, targetPath, _baseDir.Path, relativePath, _baseDir.IsDq, _baseDir.Verbose, dtTimestamp);
                            if (!MdlFile.PathExists(strArg))
                            {
                                if (_baseDir.Verbose > 1)
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, "[NEW][" + strType + "][" + MdlDate.GetFormattedDate(dtTargetUpdate, "yyyy/MM/dd HH:mm:ss") + "] mkdir " + strArg);
                                }
                                MdlFile.CreateDirectory(strArg);
                            }
                        }

                        // タイムスタンプ設定先存在時事前削除が指定された場合
                        if (_baseDir.IsPreRmFile && !string.IsNullOrEmpty(_baseDir.SetDateTimeTo) && MdlFile.PathExists(_baseDir.SetDateTimeTo))
                        {
                            if (_baseDir.Verbose > 2) _logger.WriteLine(MdlConst.LVL_NONE, " -> RM -F " + _baseDir.SetDateTimeTo);
                            MdlFile.DeleteRecursively(_baseDir.SetDateTimeTo);
                        }

                        // コマンド実行が指定された場合
                        if (_baseDir.IsPreRmCmd)
                        {
                            string strArg = MdlFile.ReplacePathForCmdExec(_baseDir.PreRmCmd, targetPath, _baseDir.Path, relativePath, _baseDir.IsDq, _baseDir.Verbose, dtTimestamp);
                            switch (_baseDir.ExecModeCode)
                            {
                                case ClsBaseDir.EXEC_MODE_CMD:
                                    _cmdExec.CmdPath = System.Environment.GetEnvironmentVariable("ComSpec") ?? "cmd";
                                    _cmdExec.CmdArgs = "/c " + strArg;
                                    break;
                                case ClsBaseDir.EXEC_MODE_PS:
                                    _cmdExec.CmdPath = "powershell";
                                    _cmdExec.CmdArgs = "-NoProfile -command \"" + strArg + "; exit $LASTEXITCODE\"";
                                    break;
                                default:
                                    _cmdExec.CmdPath = MdlUtil.GetRegexTarget(strArg, @"^(?<TARGET>\S+)\s+.*");
                                    _cmdExec.CmdArgs = MdlUtil.GetRegexTarget(strArg, @"^\S+\s+(?<TARGET>.*)");
                                    break;
                            }
                            if (_baseDir.Verbose >= 0)
                            {
                                message = "[" + _titleEn + "][" + strType + "][" + MdlDate.GetFormattedDate(dtTargetUpdate, "yyyy/MM/dd HH:mm:ss") + "] " + strArg;
                            }
                            if (!string.IsNullOrEmpty(message)) _logger.WriteLine(MdlConst.LVL_NONE, message);

                            // 削除前コマンド実行
                            if (0 != _cmdExec.ExecuteThread(_baseDir.Priority))
                            {
                                // 削除前コマンド実行に失敗した場合
                                switch (targetType)
                                {
                                    case MdlFile.PATH_IS_FILE:
                                        _deleteErrorFiles++;
                                        break;
                                    default:
                                        _deleteErrorDirs++;
                                        break;
                                }
                                if (_baseDir.Verbose >= 0)
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, " => ERROR : Cmd Return Code != 0 : " + _cmdExec.CmdPath + " " + _cmdExec.CmdArgs);
                                    _logger.WriteLine(MdlConst.LVL_NONE, " => SKIP  : DELETE : " + targetPath);
                                }
                                return;
                            }
                            // 削除前コマンド実行に成功した場合
                            else
                            {
                                // タイムスタンプの設定
                                if (_baseDir.IsSetDateTime)
                                {
                                    try
                                    {
                                        string strSetTo = MdlFile.ReplacePathForCmdExec(_baseDir.SetDateTimeTo, targetPath, _baseDir.Path, relativePath, _baseDir.IsDq, _baseDir.Verbose, dtTimestamp);
                                        switch (MdlFile.GetPathType(strSetTo))
                                        {
                                            case MdlFile.PATH_IS_DIRECTORY:
                                                if (_baseDir.Verbose > 2) _logger.WriteLine(MdlConst.LVL_NONE, " -> SET TIMESTAMP(" + strType + ")：" + MdlDate.GetFormattedDate(dtTimestamp, "yyyy/MM/dd HH:mm:ss"));
                                                MdlFile.SetDateToDirMain(strSetTo, dtTimestamp, 7, false);
                                                break;
                                            case MdlFile.PATH_IS_FILE:
                                                if (_baseDir.Verbose > 2) _logger.WriteLine(MdlConst.LVL_NONE, " -> SET TIMESTAMP(" + strType + ")：" + MdlDate.GetFormattedDate(dtTimestamp, "yyyy/MM/dd HH:mm:ss"));
                                                MdlFile.SetDateToFileMain(strSetTo, dtTimestamp, 7, false);
                                                break;
                                        }
                                    }
                                    catch { }
                                }
                                // 削除実行
                                if (MdlFile.PathExists(targetPath))
                                {
                                    if (_baseDir.IsRm)
                                    {
                                        if (_baseDir.Verbose > 2) _logger.WriteLine(MdlConst.LVL_NONE, " -> TRY DELETE");
                                        if (!MdlFile.DeleteRecursively(targetPath, _baseDir.Verbose))
                                        {
                                            switch (targetType)
                                            {
                                                case MdlFile.PATH_IS_FILE:
                                                    _deleteErrorFiles++;
                                                    break;
                                                default:
                                                    _deleteErrorDirs++;
                                                    break;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // コマンド実行が指定されなかった場合
                        else
                        {
                            if (!string.IsNullOrEmpty(message)) _logger.WriteLine(MdlConst.LVL_NONE, message);
                            if (_baseDir.IsRm)
                            {
                                if (!MdlFile.DeleteRecursively(targetPath, _baseDir.Verbose))
                                {
                                    switch (targetType)
                                    {
                                        case MdlFile.PATH_IS_FILE:
                                            _deleteErrorFiles++;
                                            break;
                                        default:
                                            _deleteErrorDirs++;
                                            break;
                                    }
                                }
                            }
                        }
                    }
                    // DRYRUN
                    else
                    {
                        if (_baseDir.IsMkDir)
                        {
                            if (_baseDir.Verbose > 1)
                            {
                                string strArg = MdlFile.ReplacePathForCmdExec(_baseDir.MkdirPath, targetPath, _baseDir.Path, relativePath, _baseDir.IsDq, _baseDir.Verbose);
                                if (!MdlFile.PathExists(strArg))
                                {
                                    _logger.WriteLine(MdlConst.LVL_NONE, "[-N-][" + strType + "][" + MdlDate.GetFormattedDate(dtTargetUpdate, "yyyy/MM/dd HH:mm:ss") + "] mkdir " + strArg);
                                }
                            }
                        }
                        if (_baseDir.IsPreRmFile && !string.IsNullOrEmpty(_baseDir.SetDateTimeTo) && MdlFile.PathExists(_baseDir.SetDateTimeTo))
                        {
                            _logger.WriteLine(MdlConst.LVL_NONE, "[-D-][" + strType + "][" + MdlDate.GetFormattedDate(dtTargetUpdate, "yyyy/MM/dd HH:mm:ss") + "] RM -F " + _baseDir.SetDateTimeTo);
                        }
                        if (_baseDir.IsPreRmCmd)
                        {
                            if (_baseDir.Verbose >= 0)
                            {
                                string strArg = MdlFile.ReplacePathForCmdExec(_baseDir.PreRmCmd, targetPath, _baseDir.Path, relativePath, _baseDir.IsDq, _baseDir.Verbose);
                                switch (_baseDir.ExecModeCode)
                                {
                                    case ClsBaseDir.EXEC_MODE_CMD:
                                        _cmdExec.CmdPath = System.Environment.GetEnvironmentVariable("ComSpec") ?? "cmd";
                                        _cmdExec.CmdArgs = "/c " + strArg;
                                        break;
                                    case ClsBaseDir.EXEC_MODE_PS:
                                        _cmdExec.CmdPath = "powershell";
                                        _cmdExec.CmdArgs = "-NoProfile -command \"" + strArg + "; exit $LASTEXITCODE\"";
                                        break;
                                    default:
                                        _cmdExec.CmdPath = MdlUtil.GetRegexTarget(strArg, @"^(?<TARGET>\S+)\s+.*");
                                        _cmdExec.CmdArgs = MdlUtil.GetRegexTarget(strArg, @"^\S+\s+(?<TARGET>.*)");
                                        break;
                                }
                                message = "[" + _titleEn + "][" + strType + "][" + MdlDate.GetFormattedDate(dtTargetUpdate, "yyyy/MM/dd HH:mm:ss") + "] " + _cmdExec.CmdPath + " " + _cmdExec.CmdArgs;
                            }
                        }
                        if (!string.IsNullOrEmpty(message)) _logger.WriteLine(MdlConst.LVL_NONE, message);
                    }
                    switch (targetType)
                    {
                        case MdlFile.PATH_IS_FILE:
                            _deletedFiles++;
                            break;
                        default:
                            _deletedDirs++;
                            break;
                    }
                }
                // 削除対象外の場合
                else
                {
                    if (!_baseDir.IsDiff)
                    {
                        if (_baseDir.Verbose >= 0)
                        {
                            message = "[---][" + strType + "][" + MdlDate.GetFormattedDate(dtTargetUpdate, "yyyy/MM/dd HH:mm:ss") + "] " + displayPath;
                        }
                    }
                    if (!string.IsNullOrEmpty(message)) _logger.WriteLine(MdlConst.LVL_NONE, message);
                }
            }
            catch (Exception objExcptn)
            {
                switch (targetType)
                {
                    case MdlFile.PATH_IS_FILE:
                        _deleteErrorFiles++;
                        break;
                    default:
                        _deleteErrorDirs++;
                        break;
                }
                if (_baseDir.Verbose >= 0)
                {
                    _logger.WriteLine(MdlConst.LVL_NONE, " => EXCEPTION : " + objExcptn.Message);
                    if (_appArg.IsStackTrace)
                    {
                        _logger.WriteLine(MdlConst.LVL_NONE, "");
                        _logger.WriteLine(MdlConst.LVL_NONE, objExcptn.StackTrace ?? "");
                        _logger.WriteLine(MdlConst.LVL_NONE, "");
                    }
                }
            }
        }

    }
}
