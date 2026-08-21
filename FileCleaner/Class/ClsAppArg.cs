using System;
using System.Collections.Generic;
using CmnClsLib.Class;
using CmnClsLib.Module;

// 2026/08/08 Gemini 3.6 Flash (High) Review & Modified

namespace FileCleaner.Class
{
    public class ClsAppArg
    {
        public const int USAGE_NONE = 0;
        public const int USAGE_USAGE = 1;
        public const int USAGE_SHOW_SAMPLE_CONFIG = 2;

        private ClsLogger _logger;
        private ClsCmmnArgs _cmmnArgs;
        private ClsCfgFile _configFile;
        private ClsBaseDir _baseDir = new();
        private string _exeDir = "";
        private string _exeBaseName = "";
        private int _pid = 0;
        private int _verbose = 0;
        private int _returnCode = MdlConst.LVL_I;
        private int _usageFlag = 0;
        private bool _isStackTrace = false;
        private int _timeout = 3600;
        private bool _isList = true;
        private bool _isDiff = true;
        private List<ClsBaseDir> _targetList = [];
        private string _delimiter = @";|";
        private string _configPath = "";
        private bool _isExample = false;
        private int _sortType = MdlFile.SORT_BY_NONE;               // ソートタイプ
        private bool _isAscending = true;                           // 昇順
        private bool _isShowDirList = false;                        // ディレクトリリストの表示フラグ
        private bool _isShowFileList = false;                       // ファイルリストの表示フラグ

        /// <summary>
        /// <see cref="ClsAppArg"/> クラスの新しいインスタンスを初期化します。
        /// </summary>
        /// <param name="logger">ログ出力に使用する <see cref="ClsLogger"/> オブジェクト。</param>
        /// <returns>なし</returns>
        /// <example>
        /// <code>
        /// var logger = new ClsLogger();
        /// var appArg = new ClsAppArg(logger);
        /// </code>
        /// </example>
        public ClsAppArg(ClsLogger logger)
        {
            ArgumentNullException.ThrowIfNull(logger);
            _logger = logger;

            _cmmnArgs = new(_logger);
            _cmmnArgs.GetModuleInfo(System.Diagnostics.Process.GetCurrentProcess().MainModule?.FileName ?? "");
            _exeDir = _cmmnArgs.ExeDir;
            _exeBaseName = _cmmnArgs.ExeBaseName;
            _pid = _cmmnArgs.Pid;

            _configFile = new(_logger)
            {
                TargetList = _targetList
            };
        }

        public string ExeBaseName { get => _exeBaseName; set => _exeBaseName = value; }
        public string ExeDir { get => _exeDir; set => _exeDir = value; }
        public int UsageFlag => _usageFlag;
        public int ReturnCode { get => _returnCode; set => _returnCode = value; }
        public int Verbose { get => _verbose; set => _verbose = value; }
        public bool IsStackTrace { get => _isStackTrace; set => _isStackTrace = value; }
        public int Timeout { get => _timeout; set => _timeout = value; }
        public string MachineName => _configFile.MachineName;
        public bool IsList => _isList;
        public bool IsDiff { get => _isDiff; set => _isDiff = value; }
        public List<ClsBaseDir> TargetList { get => _targetList; set => _targetList = value; }
        public int SortType { get => _sortType; set => _sortType = value; }
        public bool IsAscending { get => _isAscending; set => _isAscending = value; }
        public bool IsShowDirList { get => _isShowDirList; set => _isShowDirList = value; }
        public bool IsShowFileList { get => _isShowFileList; set => _isShowFileList = value; }

        /// <summary>
        /// コマンドライン引数の配列を解析し、アプリケーションの設定パラメータを取得・保持します。
        /// </summary>
        /// <param name="args">コマンドライン引数の配列。</param>
        /// <returns>引数の解析およびパラメータ取得が正常に完了した場合は <c>true</c>。失敗した場合は <c>false</c>。</returns>
        /// <example>
        /// <code>
        /// string[] args = new string[] { "-path", @"C:\Logs", "-term", "30" };
        /// bool isSuccess = appArg.GetArgs(args);
        /// </code>
        /// </example>
        public bool Parse(string[] args)
        {
            ArgumentNullException.ThrowIfNull(args);

            const string METHOD_NAME = "[ClsParam.GetArgs()]";
            Dictionary<string, string> namedArgs = [];
            bool isOk = true;
            string stringValue = "";
            bool hasValue = false;

            // -----------------------------------------------------------------
            // ClsCmmnParams処理
            // -----------------------------------------------------------------
            namedArgs = MdlArg.GetNamedArgs(args);
            _cmmnArgs.NamedArgs = namedArgs;
            isOk = _cmmnArgs.GetCommonArgs();

            // -----------------------------------------------------------------
            // ClsCmmnParams引数取得：ETC
            // -----------------------------------------------------------------
            // -h|--help ：使用方法
            _usageFlag = (_cmmnArgs.IsUsage ? USAGE_USAGE : USAGE_NONE);
            // -v|-vv|-brief      ：冗長表示|簡素表示
            _verbose = _cmmnArgs.Verbose;
            // -stacktrace        ：例外時スタックトレース表示
            _isStackTrace = _cmmnArgs.IsStackTrace;
            // -timeout           ：タイムアウト
            _timeout = _cmmnArgs.Timeout;

            _configFile.Verbose = _verbose;

            // -----------------------------------------------------------------
            // Option：
            // -----------------------------------------------------------------
            foreach (string key in new string[] { "help-conf", "show-sample-config" })
            {
                if (MdlArg.ContainsKey(namedArgs, key))
                {
                    _usageFlag = USAGE_SHOW_SAMPLE_CONFIG;
                    return true;
                }
            }

            // _isUsage_sample
            foreach (string key in new string[] { "h-sample" })
            {
                if (MdlArg.ContainsKey(namedArgs, key))
                {
                    _usageFlag = USAGE_USAGE;
                    _isExample = true;
                }
            }

            // 削除フラグ
            foreach (string key in new string[] { "clean", "exec", "list" })
            {
                if (MdlArg.ContainsKey(namedArgs, key))
                {
                    switch (key)
                    {
                        case "clean":
                        case "exec":
                            _isList = false;
                            break;
                        case "list":
                            _isList = true;
                            break;
                    }
                }
            }

            // 設定ファイル
            hasValue = false;
            bool isHitArg = false;
            foreach (string key in new string[] { "c", "conf", "cnf" })
            {
                if (MdlArg.ContainsKey(namedArgs, key))
                {
                    isHitArg = true;
                    stringValue = MdlArg.GetValue(namedArgs, key);
                    if (!string.IsNullOrEmpty(stringValue))
                    {
                        _configPath = _cmmnArgs.GetPathParam(key, MdlFile.PATH_IS_FILE, false);
                        if (!string.IsNullOrEmpty(_configPath)) hasValue = true;
                        break;
                    }
                }
            }
            if (isHitArg && !hasValue)
            {
                isOk = false;
                ConsoleWriteLine(MdlConst.LVL_NONE, "INVALID ARGUMENT : -c|-conf path_to_conf");
            }

            foreach (string key in new string[] { "delimiter" })
            {
                if (MdlArg.ContainsKey(namedArgs, key))
                {
                    stringValue = MdlArg.GetValue(namedArgs, key);
                    if (!string.IsNullOrEmpty(stringValue))
                    {
                        _delimiter = stringValue.Trim();
                        break;
                    }
                }
            }

            // -----------------------------------------------------------------
            // 設定ファイルが指定されなかった場合
            // -----------------------------------------------------------------
            if (string.IsNullOrEmpty(_configPath))
            {
                ClsBaseDir baseDir = new()
                {
                    Verbose = _verbose,
                    Timeout = _timeout
                };

                // パスが存在しない場合の終了コード
                if (MdlArg.ContainsKey(namedArgs, "na-mkdir")) baseDir.IsMkRmBaseDir = true;
                if (MdlArg.ContainsKey(namedArgs, "na-i")) baseDir.NaRetCode = MdlConst.LVL_I;
                if (MdlArg.ContainsKey(namedArgs, "na-w")) baseDir.NaRetCode = MdlConst.LVL_W;
                if (MdlArg.ContainsKey(namedArgs, "na-e")) baseDir.NaRetCode = MdlConst.LVL_E;

                // 削除失敗時の終了コード
                if (MdlArg.ContainsKey(namedArgs, "err-i")) baseDir.ErrRetCode = MdlConst.LVL_I;
                if (MdlArg.ContainsKey(namedArgs, "err-w")) baseDir.ErrRetCode = MdlConst.LVL_W;
                if (MdlArg.ContainsKey(namedArgs, "err-e")) baseDir.ErrRetCode = MdlConst.LVL_E;

                // ACTION
                foreach (string key in new string[] { "a", "action" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        stringValue = MdlArg.GetValue(namedArgs, key);
                        if (!string.IsNullOrEmpty(stringValue))
                        {
                            switch (stringValue.ToLower())
                            {
                                case "gendel":
                                case "gen":
                                case "g":
                                    if (_verbose > 4) _logger.WriteLine(MdlConst.LVL_NONE, $"ARG -{key} {stringValue}");
                                    baseDir.ActionCode = ClsBaseDir.ACTION_GEN_DELETE;
                                    baseDir.MaxDepth = 0;
                                    break;
                            }
                        }
                    }
                }

                // パスの取得
                foreach (string key in new string[] { "path", "f" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        stringValue = MdlArg.GetFullPath(namedArgs, key);
                        if (!string.IsNullOrEmpty(stringValue))
                        {
                            stringValue = MdlFile.RemoveTrailingPathSeparator(MdlFile.GetAbsolutePath(stringValue));
                            baseDir.Path = stringValue;

                            if (string.IsNullOrEmpty(MdlFile.GetDirectoryPath(baseDir.Path))) baseDir.Path += @"\.";
                            switch (MdlFile.GetPathType(stringValue))
                            {
                                case MdlFile.PATH_IS_DIRECTORY:
                                    baseDir.IsBaseDir = true;
                                    break;
                                case MdlFile.PATH_IS_FILE:
                                    baseDir.IsBaseDir = false;
                                    break;
                                default:
                                    bool isExist = false;
                                    if (baseDir.IsMkRmBaseDir)
                                    {
                                        if (MdlFile.CreateDirectory(stringValue) <= MdlFile.OK_MKDIR_HANTEI)
                                        {
                                            baseDir.IsBaseDir = true;
                                            isExist = true;
                                        }
                                        else
                                        {
                                            _logger.WriteLine(baseDir.NaRetCode, $"{METHOD_NAME}FAILED TO MKDIR : {stringValue}");
                                        }
                                    }
                                    if (!isExist)
                                    {
                                        isOk = false;
                                        baseDir.IsExec = false;
                                        baseDir.ReturnCode = baseDir.NaRetCode;
                                        _logger.WriteLine(baseDir.NaRetCode, $"{METHOD_NAME}NO SUCH A FILE OR DIRECTORY : {stringValue}");
                                    }
                                    break;
                            }
                            break;
                        }
                    }
                }
                if (isOk && string.IsNullOrEmpty(baseDir.Path))
                {
                    baseDir.ReturnCode = baseDir.NaRetCode;
                    ConsoleWriteLine(MdlConst.LVL_NONE, "INVALID ARGUMENT : -path path_to_target");
                    isOk = false;
                }

                // 削除対象判別経過日数の取得
                foreach (string key in new string[] { "term", "days" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        // FLAG ON
                        switch(key)
                        {
                            case "term":
                                baseDir.IsTerm = true;
                                break;
                            case "days":
                                baseDir.IsTerm = true;
                                baseDir.IsDays = true;
                                break;
                        }   
                        // VALUE
                        stringValue = MdlArg.GetValue(namedArgs, key);
                        if (!string.IsNullOrEmpty(stringValue))
                        {
                            double parsedDouble = MdlUtil.ParseDouble(stringValue, MdlConst.DBL_NULL);
                            if (parsedDouble != MdlConst.DBL_NULL) baseDir.Term = parsedDouble;
                        }
                    }
                }

                // 削除対象は判別経過日数より新しい更新日付とするか否か
                foreach (string key in new string[] { "new" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        baseDir.IsTerm = true;
                        baseDir.IsNew = true;
                    }
                }

                // 日付閾値の取得
                if (baseDir.IsDays)
                {
                    baseDir.ThresholdDate = DateTime.Today.AddDays(-1.0 * baseDir.Term);
                }
                else
                {
                    baseDir.ThresholdDate = DateTime.Now.AddDays(-1.0 * baseDir.Term);
                }

                // 日付閾値の取得
                foreach (string key in new string[] { "date" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        stringValue = MdlArg.GetValue(namedArgs, key);
                        if (!string.IsNullOrEmpty(stringValue))
                        {
                            if (MdlDate.TryParseDateTime(stringValue, out DateTime parsedDateTime))
                            {
                                baseDir.IsTerm = true;
                                baseDir.ThresholdDate = parsedDateTime;
                            }
                        }
                    }
                }

                // 削除対象ディレクトリ階層(MIN)
                foreach (string key in new string[] { "min" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        stringValue = MdlArg.GetValue(namedArgs, key);
                        if (!string.IsNullOrEmpty(stringValue))
                        {
                            baseDir.MinDepth = MdlUtil.ParseULong(stringValue, 0);
                            break;
                        }
                    }
                }

                // 削除対象ディレクトリ階層(MAX)
                foreach (string key in new string[] { "max" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        stringValue = MdlArg.GetValue(namedArgs, key);
                        if (!string.IsNullOrEmpty(stringValue))
                        {
                            baseDir.MaxDepth = MdlUtil.ParseULong(stringValue, MdlConst.LNG_MAX);
                            break;
                        }
                    }
                }

                // ディレクトリ階層の整合性チェック
                if (baseDir.MinDepth > baseDir.MaxDepth)
                {
                    isOk = false;
                    ConsoleWriteLine(MdlConst.LVL_E, $"{METHOD_NAME} INVALID ARGUMENT : -min {baseDir.MinDepth} > -max : {baseDir.MaxDepth}");
                }

                // 更新日付ではなく作成日で評価
                foreach (string key in new string[] { "ctime" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        baseDir.IsCreationTime = true;
                    }
                }

                // LIST表示時のダブルクォーテーションフラグ
                foreach (string key in new string[] { "dq" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        baseDir.IsDq = true;
                    }
                }

                // ファイル削除前コマンド
                foreach (string key in new string[] { "precmd" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        stringValue = MdlArg.GetValue(namedArgs, key);
                        if (!string.IsNullOrEmpty(stringValue))
                        {
                            baseDir.PreRmCmd = stringValue;
                            baseDir.IsPreRmCmd = true;
                            break;
                        }
                    }
                }

                // ファイル削除前コマンド
                foreach (string key in new string[] { "mkdir" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        stringValue = MdlArg.GetValue(namedArgs, key);
                        if (!string.IsNullOrEmpty(stringValue))
                        {
                            baseDir.MkdirPath = stringValue;
                            baseDir.IsMkDir = true;
                            break;
                        }
                    }
                }

                // 更新のみ表示フラグ
                foreach (string key in new string[] { "no-diff" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        _isDiff = false;
                        break;
                    }
                }

                // TYPE
                stringValue = MdlArg.GetValue(namedArgs, "type") ?? "";
                if (string.IsNullOrEmpty(stringValue))
                {
                    baseDir.TargetType = "f";
                    baseDir.IsRmFile = true;
                }
                else
                {
                    baseDir.TargetType = stringValue;
                    string lowerType = stringValue.ToLower();
                    if (lowerType.Contains('f')) baseDir.IsRmFile = true;
                    if (lowerType.Contains('d')) baseDir.IsRmDir = true;
                    if (lowerType.Contains('e')) baseDir.IsRmEmptyDir = true;
                    if (lowerType.Contains('s')) baseDir.IsRmSymlink = true;
                    if (lowerType.Contains('b'))
                    {
                        baseDir.IsRmFile = true;
                        baseDir.IsRmEmptyDir = true;
                    }
                    if (lowerType.Contains('a'))
                    {
                        baseDir.IsRmFile = true;
                        baseDir.IsRmDir = true;
                        baseDir.IsRmEmptyDir = true;
                        baseDir.IsRmSymlink = true;
                    }
                }

                // シンボリックリンク判定フラグ
                foreach (string key in new string[] { "sym" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        baseDir.IsSymLink = true;
                        break;
                    }
                }

                // フィルタ設定
                _cmmnArgs.GetFilterLists();
                baseDir.IncFilesList = _cmmnArgs.IncFilesList;
                baseDir.IncDirsList = _cmmnArgs.IncDirsList;
                baseDir.ExcFilesList = _cmmnArgs.ExcFilesList;
                baseDir.ExcDirsList = _cmmnArgs.ExcDirsList;
                baseDir.IsRegIncBasename = _cmmnArgs.IsRegIncBasename;
                baseDir.IsRegExcBasename = _cmmnArgs.IsRegExcBasename;
                baseDir.IsDirFilterOr = _cmmnArgs.IsDirFilterOr;
                baseDir.IsIncHitRecursive = _cmmnArgs.IsIncHitRecursive;
                baseDir.IsExcHitRecursive = _cmmnArgs.IsExcHitRecursive;

                // 保存世代削除：保存世代  
                foreach (string key in new string[] { "gen" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        stringValue = MdlArg.GetValue(namedArgs, key);
                        if (!string.IsNullOrEmpty(stringValue))
                        {
                            int parsedInt = MdlUtil.ParseInt(stringValue, MdlConst.INT_NULL);
                            if (parsedInt != MdlConst.INT_NULL) baseDir.Generation = parsedInt;
                            break;
                        }
                    }
                }

                // 日付取得フラグ
                foreach (string key in new string[] { "name" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        baseDir.IsDateByName = true;
                    }
                }

                // 非削除フラグ
                foreach (string key in new string[] { "no-rm" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        baseDir.IsRm = false;
                    }
                }

                // 日付設定先の取得
                foreach (string key in new string[] { "ts-to" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        stringValue = MdlArg.GetValue(namedArgs, key);
                        if (!string.IsNullOrEmpty(stringValue))
                        {
                            baseDir.IsSetDateTime = true;
                            baseDir.SetDateTimeTo = stringValue;
                            break;
                        }
                    }
                }

                // 日付設定先の取得
                foreach (string key in new string[] { "ts" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        stringValue = MdlArg.GetValue(namedArgs, key);
                        if (!string.IsNullOrEmpty(stringValue))
                        {
                            baseDir.DateTimeMode = baseDir.ParseDateTimeMode(stringValue);
                            baseDir.SetDateTimeTo = stringValue;
                            break;
                        }
                    }
                }

                // -cwd
                hasValue = false;
                foreach (string key in new string[] { "cwd" })
                {
                    if (MdlArg.ContainsKey(namedArgs, key))
                    {
                        stringValue = MdlArg.GetValue(namedArgs, key);
                        if (!string.IsNullOrEmpty(stringValue))
                        {
                            hasValue = true;
                            baseDir.WorkDir = stringValue;
                            break;
                        }
                        if (!hasValue)
                        {
                            baseDir.WorkDir = baseDir.Path;
                        }
                    }
                }

                // リスト追加
                _targetList.Add(baseDir);
                _baseDir = baseDir;
            }
            // -----------------------------------------------------------------
            // 設定ファイルが指定された場合
            // -----------------------------------------------------------------
            else
            {
                _configFile.Delimiter = _delimiter;
                _configFile.Timeout = _timeout;
                isOk = _configFile.ReadConfig(_configPath, true);
                if (isOk)
                {
                    if (0 == _targetList.Count)
                    {
                        ConsoleWriteLine(MdlConst.LVL_E, $"{METHOD_NAME} NO DEF-LINES FOUND : {_configPath}");
                    }
                }
                else
                {
                    ConsoleWriteLine(MdlConst.LVL_E, $"{METHOD_NAME} FAILED TO _configFile.ReadConfig() : {_configPath}");
                }
            }

            // -----------------------------------------------------------------
            // Sort Option：
            // -----------------------------------------------------------------
            foreach (string key in new string[] { "sort" })
            {
                if (MdlArg.ContainsKey(namedArgs, key))
                {
                    stringValue = MdlArg.GetValue(namedArgs, key);
                    if (!string.IsNullOrEmpty(stringValue))
                    {
                        _sortType = MdlFile.GetSortTypeNum(stringValue);
                        break;
                    }
                }
            }
            foreach (string key in new string[] { "desc" })
            {
                if (MdlArg.ContainsKey(namedArgs, key))
                {
                    _isAscending = false;
                }
            }
            foreach (string key in new string[] { "asc" })
            {
                if (MdlArg.ContainsKey(namedArgs, key))
                {
                    _isAscending = true;
                }
            }
            foreach (string key in new string[] { "show-dirs" })
            {
                if (MdlArg.ContainsKey(namedArgs, key))
                {
                    _isShowDirList = true;
                }
            }
            foreach (string key in new string[] { "show-files" })
            {
                if (MdlArg.ContainsKey(namedArgs, key))
                {
                    _isShowFileList = true;
                }
            }

            // -----------------------------------------------------------------
            // 掃除
            // -----------------------------------------------------------------
            namedArgs.Clear();

            // -----------------------------------------------------------------
            // END
            // -----------------------------------------------------------------
            return isOk;
        }

        /// <summary>
        /// アプリケーションの使用方法（コマンドラインオプションおよび各設定項目の現在値または設定例）をログに出力します。
        /// </summary>
        /// <returns>なし</returns>
        /// <example>
        /// <code>
        /// appArg.Usage();
        /// </code>
        /// </example>
        public void Usage()
        {
            string label = (_isExample ? "例" : "現在値");
            _logger.WriteLine(MdlConst.LVL_NONE, "");
            _logger.WriteLine(MdlConst.LVL_NONE, "Usage : " + _exeDir + Path.DirectorySeparatorChar + _exeBaseName + ".exe [Option] [Option]...");
            _logger.WriteLine(MdlConst.LVL_NONE, "");
            _logger.WriteLine(MdlConst.LVL_NONE, "■Execution Option：");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -clean|-exec   ：対象一覧の削除                          （" + label + "=" + (_isExample ? @"true" : !_isList) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -list          ：対象一覧の表示（実行取消）              （" + label + "=" + (_isExample ? @"false" : _isList) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -no-rm         ：非削除モード・削除前実行コマンド実行    （" + label + "=" + (_isExample ? @"false" : !_baseDir.IsRm) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "■Config File Option：");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -c|-conf path  ：設定ファイルのパス                      （" + label + "=" + (_isExample ? @"C:\Tool\Infra\conf\FileClean.conf" : _configPath) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -delimiter str ：設定区切り文字                          （" + label + "=" + (_isExample ? @"|" : _delimiter) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "■Non Config File Option：");
            _logger.WriteLine(MdlConst.LVL_NONE, "  Basic Option：");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -action|-a act ：delete | gendel                         （" + label + "=" + (_isExample ? @"delete" : _baseDir.GetActionString(_baseDir.ActionCode) + " CODE=" + _baseDir.ActionCode) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -path|-f path  ：対象ディレクトリパス                    （" + label + "=" + (_isExample ? @"C:\Log" : _baseDir.Path) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "  Filter Option：");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -type fdes     ：対象：f=file|d=dir|e=emptydir|s=symdir  （" + label + "=" + (_isExample ? @"f" : _baseDir.TargetType) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -sym           ：シンボリックリンク判定フラグ            （" + label + "=" + (_isExample ? @"true" : _baseDir.IsSymLink) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -term|-days val：削除対象ファイル更新経過日数            （" + label + "=" + (_isExample ? @"30" : _baseDir.Term.ToString("F1") + "：" + MdlDate.GetFormattedDate(_baseDir.ThresholdDate, "yyyy/MM/dd HH:mm:ss")) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -date yyyyMMdd ：削除対象ファイル更新日付                （" + label + "=" + (_isExample ? MdlDate.GetFormattedDate(DateTime.Now, "yyyyMMdd") : MdlDate.GetFormattedDate(_baseDir.ThresholdDate, "yyyyMMdd")) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -new           ：経過日数(-term)以内を削除する場合       （" + label + "=" + (_isExample ? @"false" : _baseDir.IsNew) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -min  value    ：最小ディレクトリ階層                    （" + label + "=" + (_isExample ? @"0" : _baseDir.MinDepth) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -max  value    ：最大ディレクトリ階層                    （" + label + "=" + (_isExample ? @"3" : _baseDir.MaxDepth) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -id 正規表現   ：絞り込みディレクトリ名(カンマ区切り)    （" + label + "=" + (_isExample ? @"^log$,^tmp$" : string.Join("|", _baseDir.IncDirsList.ToArray()) + "]") + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -xd 正規表現   ：除外ディレクトリ名(カンマ区切り)        （" + label + "=" + (_isExample ? @"^bin$,^conf$" : string.Join("|", _baseDir.ExcDirsList.ToArray()) + "]") + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -if 正規表現   ：絞り込みファイル名(カンマ区切り)        （" + label + "=" + (_isExample ? @"\.log$,\.dat$" : string.Join("|", _baseDir.IncFilesList.ToArray()) + "]") + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -xf 正規表現   ：除外ファイル名(カンマ区切り)            （" + label + "=" + (_isExample ? @"\.exe$,\.dll$" : string.Join("|", _baseDir.ExcFilesList.ToArray()) + "]") + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -idorxd        ：-id or -xdフラグ                        （" + label + "=" + (_isExample ? @"false" : _baseDir.IsDirFilterOr) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -no-id-rec     ：-id結果の階層下への非適用フラグ         （" + label + "=" + (_isExample ? @"false" : !_baseDir.IsIncHitRecursive) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -no-xd-rec     ：-xd結果の階層下への非適用フラグ         （" + label + "=" + (_isExample ? @"false" : !_baseDir.IsExcHitRecursive) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -dq            ：対象一覧表示時ダブルクォーテーション囲み（" + label + "=" + (_isExample ? @"true" : !_baseDir.IsDq) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "  Generation Delete Option：");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -gen int       ：保存世代数                              （" + label + "=" + (_isExample ? @"10" : _baseDir.Generation) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "  DateTime Option：");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -ctime         ：更新日付ではなく作成日で評価            （" + label + "=" + (_isExample ? @"false" : _baseDir.IsCreationTime) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -name          ：ファイル名に含む日付で評価              （" + label + "=" + (_isExample ? @"false" : _baseDir.IsDateByName) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "  Command Option：");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -mkdir path    ：削除前ディレクトリ作成                  （" + label + "=" + (_isExample ? @"D:\Backup\TargetName\_RELPATH_" : _baseDir.MkdirPath) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -precmd cmd    ：削除前実行コマンド                      （" + label + "=" + (_isExample ? @"C:\Progra~1\7-Zip\7z.exe a -y -snl {}.zip {}" : _baseDir.PreRmCmd) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -cwd [path]    ：ワーキングディレクトリ                  （" + label + "=" + (_isExample ? @"C:\Tool\Work" : _baseDir.WorkDir) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -ts-to path    ：日付設定先パス                          （" + label + "=" + (_isExample ? @"{}.zip" : _baseDir.SetDateTimeTo) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -ts n|t|y|f    ：日付：n=今、t=今日、y=昨日、f=FILE属性  （" + label + "=" + (_isExample ? @"f" : _baseDir.GetDateTimeModeString(_baseDir.DateTimeMode)) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "  Subfolder Sorting Option：");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -sort type     ：ソート=none|name|ctime|mtime            （" + label + "=" + (_isExample ? @"none"  : MdlFile.GetSortTypeName(_sortType)) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -desc          ：降順フラグ                              （" + label + "=" + (_isExample ? @"false" : !_isAscending) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "■Exit Code Option：");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -na-mkdir      ：-pathが存在しない場合のMKDIRフラグ      （" + label + "=" + (_isExample ? @"false" : _baseDir.IsMkRmBaseDir) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -na-i|w|e      ：-pathが存在しない場合の終了コード       （" + label + "=" + (_isExample ? @"false" : _baseDir.NaRetCode) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -err-i|w|e     ：削除失敗時の終了コード                  （" + label + "=" + (_isExample ? @"false" : _baseDir.ErrRetCode) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "■Other Option      ：");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -v|-vv|-brief  ：冗長表示|簡素表示                       （" + label + "=" + (_isExample ? @"2" : _verbose) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -no-diff       ：更新のみ表示の取消                       (" + label + "=" + (_isExample ? @"false" : !_isDiff) + "）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -console mode  ：メッセージ表示 off|stdout|stderr");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -ldir path     ：ログ出力先ディレクトリパス（日付付ファイル名で出力）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -log  path     ：ログ出力ファイルパス      （-ldirより優先）");
            _logger.WriteLine(MdlConst.LVL_NONE, "   -h-sample      ：Usageの表示時に値例表示フラグ");
            _logger.WriteLine(MdlConst.LVL_NONE, "   --show-sample-config|-help-conf：サンプル設定ファイル表示フラグ");
            _logger.WriteLine(MdlConst.LVL_NONE, "");
            _logger.WriteLine(MdlConst.LVL_NONE, "Return Code : " + MdlConst.LVL_I + ":SUCCESS / " + MdlConst.LVL_W + ":WARN / " + MdlConst.LVL_E + ":ERROR");
            _logger.WriteLine(MdlConst.LVL_NONE, "");
        }

        /// <summary>
        /// 指定されたエラーレベルとメッセージをログに書き込みます。
        /// </summary>
        /// <param name="errorLevel">エラーレベル（例: <see cref="MdlConst.LVL_NONE"/>, <see cref="MdlConst.LVL_I"/>, <see cref="MdlConst.LVL_E"/>）。</param>
        /// <param name="message">ログに書き込むメッセージ文字列。</param>
        /// <returns>なし</returns>
        /// <example>
        /// <code>
        /// appArg.ConsoleWriteLine(MdlConst.LVL_I, "処理を開始します。");
        /// </code>
        /// </example>
        public void ConsoleWriteLine(int errorLevel, string message)
        {
            _logger.WriteLine(errorLevel, message);
        }

        /// <summary>
        /// 指定された <see cref="ClsBaseDir"/> オブジェクトの定義情報（動作条件、対象ディレクトリ、削除条件等）をコンソール・ログに出力します。
        /// </summary>
        /// <param name="baseDir">出力対象の <see cref="ClsBaseDir"/> オブジェクト。</param>
        /// <returns>なし</returns>
        /// <example>
        /// <code>
        /// appArg.PrintDefinition(baseDir);
        /// </code>
        /// </example>
        public void PrintDefinition(ClsBaseDir baseDir)
        {
            ArgumentNullException.ThrowIfNull(baseDir);

            ConsoleWriteLine(MdlConst.LVL_NONE, $"# 定義    ：行番号= {baseDir.LineNo.ToString().PadLeft(4, '0')} ACTION={baseDir.GetActionString(baseDir.ActionCode)} / フラグ：実行={baseDir.IsExec} 削除={baseDir.IsRm} 期間評価={baseDir.IsTerm} Verbose={baseDir.Verbose}");
            ConsoleWriteLine(MdlConst.LVL_NONE, $"# 削除対象：ファイル={baseDir.IsRmFile} ディレクトリ={baseDir.IsRmDir} 空フォルダ={baseDir.IsRmEmptyDir} SYMLINK={baseDir.IsRmSymlink}");
            if (baseDir.IsBaseDir)
            {
                ConsoleWriteLine(MdlConst.LVL_NONE, $"# DIR パス：{baseDir.Path}");
            }
            else
            {
                ConsoleWriteLine(MdlConst.LVL_NONE, $"# FILEパス：= {baseDir.Path}");
            }
            switch (baseDir.ActionCode)
            {
                case ClsBaseDir.ACTION_GEN_DELETE:
                    ConsoleWriteLine(MdlConst.LVL_NONE, $"# 保存世代：{baseDir.Generation:N0}");
                    break;
                default:
                    if (baseDir.IsNew)
                    {
                        ConsoleWriteLine(MdlConst.LVL_NONE, $"# 保存期間：{baseDir.Term:F1} DAYS[{MdlDate.GetFormattedDate(baseDir.ThresholdDate, "yyyy/MM/dd HH:mm:ss")}]より新しいファイルを対象");
                    }
                    else
                    {
                        ConsoleWriteLine(MdlConst.LVL_NONE, $"# 保存期間：{baseDir.Term:F1} DAYS[{MdlDate.GetFormattedDate(baseDir.ThresholdDate, "yyyy/MM/dd HH:mm:ss")}]より古いファイルを対象");
                    }
                    break;
            }
            ConsoleWriteLine(MdlConst.LVL_NONE, $"# 検索階層：{baseDir.MinDepth} ～ {baseDir.MaxDepth}");
            ConsoleWriteLine(MdlConst.LVL_NONE, $"# 絞込条件： DIR = [{string.Join("|", baseDir.IncDirsList)}] / FILE = [{string.Join("|", baseDir.IncFilesList)}] / idrec = {baseDir.IsIncHitRecursive}");
            ConsoleWriteLine(MdlConst.LVL_NONE, $"# 除外条件： DIR = [{string.Join("|", baseDir.ExcDirsList)}] / FILE = [{string.Join("|", baseDir.ExcFilesList)}] / xdrec = {baseDir.IsExcHitRecursive}");
        }

        // --------------------------------------------------------------------
        /// <summary>
        /// サンプル設定ファイルの内容と各項目の解説をコンソール・ログに出力します。
        /// </summary>
        /// <returns>なし</returns>
        /// <example>
        /// <code>
        /// appArg.ShowSampleConfig();
        /// </code>
        /// </example>
        // --------------------------------------------------------------------
        public void ShowSampleConfig()
        {
            _logger.WriteLine(MdlConst.LVL_NONE, @"################################################################################");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 00：対象ホスト名CSVリスト：ALL、または正規表現（例：^SRVAP\d+$,^SRVBT\d+$）");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 01：処理実行フラグ：0=実行しない、1=実行する");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 02：処理対象タイプ：削除対象    ：F=ファイル削除、E=空フォルダ削除、D=フォルダ削除、SRM=シンボリックリンク削除、S：シンボリックリンク判定");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#                     日数判定方法：C=作成日付で判定、NAME=ファイル名で判定");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#                     経過日数    ：DAYS=TODAY、TERM=NOW");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#                     削除モード  ：GEN=世代保存削除、NORM=削除しない");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#                     日付設定    ：TSX（TSN=NOW、TST=TODAY、TSY=YESTERDAY、TSF=FILE）");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#                     削除エラー時：ERR-I=正常終了、ERR-W=警告終了(初期値)、削除ERR-W=異常終了");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#                     パス非存在時：NA-I=正常終了(初期値)、NA-W=警告終了、NA-W=異常終了、NA-MKDIR=パス作成");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#                     事前コマンド：NORMAL=常に正常終了、NEGATIVE=負値はエラー、W数値=警告閾値、E数値=異常閾値、CWD=パスにCHDIR、SHOW-PARAM=情報表示");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#                                   P数値=優先度、PRERM=タイムスタンプ設定先存在時事前削除有無、TIMEOUT数値=タイムアウト（秒）");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#                                   M-CMD=cmd.exe /c 事前コマンド、M-PS=powershell -command 事前コマンド; exit $LASTEXITCODE");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 03：経過日数|保存世代");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 04：パス");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 05：最小階層数");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 06：最大階層数");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 07：絞込：フォルダ名");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 08：絞込：ファイル名");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 09：除外：フォルダ名");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 10：除外：ファイル名");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 11：ファイル削除前コマンド");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 12：ファイル削除前作成ディレクトリ");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 13：日付設定先パス");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# ※ファイル削除前コマンド／ファイル削除前作成ディレクトリの文字列置換マクロ");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   {}、_PATH_    ：ファイルフルパス");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   _RELPATH_     ：削除対象ベースパスからの相対パス");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   _RELFLAT_     ：相対パスのパス区切り文字列「\」「/」を「_」に変換したもの");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   _BASEDIR_     ：削除対象ベースパス");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   _DIR_         ：ファイルフルパスの親ディレクトリパス");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   _RELDIR_      ：ファイル相対パスの親ディレクトリパス");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   _RELDIRFLAT_  ：ファイル相対パスの親ディレクトリパスのパス区切り文字列「\」「/」を「_」に変換したもの");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   _FILENAME_    ：ファイル名（拡張子付き）");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   _BASENAME_    ：ファイル名（拡張子無し）");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   _COMPUTERNAME_：コンピュータ名");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   %y%Y%m%d%w    ：日付");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   %H%M%S%pid    ：時刻／PID");
            _logger.WriteLine(MdlConst.LVL_NONE, @"################################################################################");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#--------------+-----+---------------------+------+------------------+-----+-----+------------------+---------------+------------------+-------------------+----------------------------------------------------------------------+---------------------------+------------------------+");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# サーバ共通：ログ                                                                                                                                                                                                                                                                     |");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#--------------+-----+---------------------+------+------------------+-----+-----+------------------+---------------+------------------+-------------------+----------------------------------------------------------------------+---------------------------+------------------------+");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# ホストリスト | FLG | OPTIONS             | 期間 | パス             | MIN | MAX | 絞込ディレクトリ | 絞込ファイル  | 除外ディレクトリ | 除外ファイル      | 事前実行コマンド                                                     | 事前作成ディレクトリパス  | タイムスタンプ設定先   |");
            _logger.WriteLine(MdlConst.LVL_NONE, @"ALL            |   1 | F,DAYS,TSF,P4,PRERM |    2 | C:\Log           |     |     |                  |               |                  | \.zip$,^ys\.log   | C:\Progra~1\7-Zip\7z.exe a -y -snl ""_DIR_\_BASENAME_.zip"" ""{}""       |                           | ""_DIR_\_BASENAME_.zip"" |");
            _logger.WriteLine(MdlConst.LVL_NONE, @"ALL            |   1 | F,DAYS              |    7 | C:\Log           |     |     |                  | \.zip$        |                  |                   | C:\Tool\Infra\bin.cur\FsFileUtil.exe -f ""{}"" -t ""\\FILESEVER\Backup\Log\%y\%m\_COMPUTERNAME_\_RELPATH_\_FILENAME_"" | |    |");
            _logger.WriteLine(MdlConst.LVL_NONE, @"AP             |   1 | F,NORM              |    0 | C:\Log\webapps   |     |     |                  | ^App\.log$    |                  |                   | C:\Tool\Infra\bin.cur\FsFileUtil.exe -f ""{}"" -a rotate -k 10         |                           |                        |");
            _logger.WriteLine(MdlConst.LVL_NONE, @"ALL            |   1 | F,DAYS,TSF,P4,PRERM |    2 | C:\Log\webapps   |     |     |                  | ^App\.log     |                  | \.zip$,^App\.log$ | C:\Progra~1\7-Zip\7z.exe a -y -snl ""_DIR_\App.%Y%m%d.zip"" ""{}""       |                           | ""_DIR_\App.%Y%m%d.zip"" |");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#--------------+-----+---------------------+------+------------------+-----+-----+------------------+---------------+------------------+-------------------+----------------------------------------------------------------------+---------------------------+------------------------+");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# BATCHサーバ：リリースバックアップ                                                                                                                                                                                                                                                    |");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#--------------+-----+---------------------+------+------------------+-----+-----+------------------+---------------+------------------+-------------------+----------------------------------------------------------------------+---------------------------+------------------------+");
            _logger.WriteLine(MdlConst.LVL_NONE, @"BATCH          |   1 | D,GEN,TSF           |   10 | C:\job\_backup   |     |   0 | _\d{8}_\d{6}$    |               |                  |                   | C:\Progra~1\7-Zip\7z.exe a -y -snl ""{}.zip"" ""{}""                     |                           | ""{}.zip""               |");
            _logger.WriteLine(MdlConst.LVL_NONE, @"BATCH          |   1 | F,GEN               |   30 | C:\job\_backup   |     |   0 |                  | \.zip$        |                  |                   |                                                                      |                           |                        |");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#--------------+-----+---------------------+------+------------------+-----+-----+------------------+---------------+------------------+-------------------+----------------------------------------------------------------------+---------------------------+------------------------+");
            _logger.WriteLine(MdlConst.LVL_NONE, @"################################################################################");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#◆サンプル設定ファイルの解説");
            _logger.WriteLine(MdlConst.LVL_NONE, @"################################################################################");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 1)C:\Logのファイルのうち、今から２日前より古いファイルをzip圧縮し（元のファイルのタイムスタンプをzipファイルに適用）、圧縮が正常終了した場合は元のファイルを削除");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   ホストリスト：ALL⇒全サーバを対象");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   OPTIONS：F⇒ファイルを対象、DAYS⇒２日間経過したファイルを対象、TSF⇒元のファイルのタイムスタンプをzipファイルに適用、P4⇒圧縮時のプロセス優先度を４番とデフォルトの３番から１つ下げる、PRERM⇒ZIPファイルが既に存在する場合は削除");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   除外ファイル：ファイル名が「\.zip$,^App\.log」に該当するファイルは対象外とし、該当しないファイルを対象とする");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   削除ファイル⇒C:\Log\tomcat\catalina.log.20230225");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   圧縮ファイル⇒C:\Log\tomcat\catalina.log.20230225.zip");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 2)C:\Logのファイルのうち、７日間経過したZIPファイルをファイルサーバへコピーし、コピーが成功したらコピー元のファイルを削除");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   ホストリスト：ALL⇒全サーバを対象");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   OPTIONS：F⇒ファイルを対象、DAYS⇒今から７日前より古いファイルを対象");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   絞込ファイル：ファイル名が「\.zip$」に該当するファイルを対象とし、該当しないファイルは対象外とする");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   移動元ファイル名⇒C:\Log\tomcat\catalina.log.20230225.zip");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   移動先ファイル名⇒\\FILESEVER\Backup\Log\%y\%m\_COMPUTERNAME_\_RELPATH_\_FILENAME_⇒\\FILESEVER\Backup\Log\2023\02\SERVER001\tomcat\catalina.log.20230225.zip");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 3)C:\Log\webapps\App.logをC:\Log\*\App.log.1に名前を変更し、C:\Log\webapps\App.log.10まで10世代保持");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   ホストリスト：AP⇒ホスト名が正規表現「AP」に該当するサーバ");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   OPTIONS：F⇒ファイルを対象、NORM⇒該当ファイルを削除しない（事前実行コマンドでリネームするので）");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   絞込ファイル：ファイル名が「^ys\.log$」に該当するファイルを対象とし、該当しないファイルは対象外とする");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 4)C:\Log\webapps\App.log.1ファイルのうち、２日間経過したファイルをzip圧縮し（元のファイルのタイムスタンプをzipファイルに適用）、圧縮が正常終了した場合は元のファイルを削除");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   ホストリスト：ALL⇒全サーバを対象");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   OPTIONS：F⇒ファイルを対象、DAYS⇒２日間経過したファイルを対象、TSF⇒元のファイルのタイムスタンプをzipファイルに適用、P4⇒圧縮時のプロセス優先度を４番とデフォルトの３番から１つ下げる、PRERM⇒ZIPファイルが既に存在する場合は削除");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   絞込ファイル：ファイル名が「^App\.log」に該当するファイルを対象とし、該当しないファイルは対象外とする");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   除外ファイル：ファイル名が「\.zip$,^App\.log$」に該当するファイルは対象外とし、該当しないファイルを対象とする");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   ※^App\.log.1～^App\.log.10を^App\.log.1～^App\.log.10");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   ※圧縮ファイル名は、App.%Y%m%d.zipとファイル名に日付をつける");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   ※ZIPファイルのファイルサーバへの退避は、2)で対象となる");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 5)C:\job\_backupにサブディレクトリ名がbat_20230225_100000と日付_日時を含む場合、最新の10世代保持し、それより古い世代のディレクトリをzip圧縮し（元のディレクトリ名の日付_日時zipファイルのタイムスタンプに適用）、圧縮が正常終了した場合は元のファイルを削除");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   ホストリスト：BATCH⇒ホスト名が正規表現「BATCH」に該当するサーバ");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   OPTIONS：D⇒ディレクトリを対象、GEN⇒世代保存削除、TSF⇒元のファイルのタイムスタンプをzipファイルに適用");
            _logger.WriteLine(MdlConst.LVL_NONE, @"#   絞込ファイル：ディレクトリ名が「_\d{8}_\d{6}$」に該当するディレクトリを対象とし、該当しないディレクトリは対象外とする");
            _logger.WriteLine(MdlConst.LVL_NONE, @"# 6)C:\job\_backup\xxx_yyyymmdd_HHMMSS.zipファイルのうち、10世代より古いZIPファイルがあれば削除");
            _logger.WriteLine(MdlConst.LVL_NONE, @"################################################################################");
        }

    }
}
