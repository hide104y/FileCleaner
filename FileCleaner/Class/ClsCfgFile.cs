using System;
using System.Text;
using System.IO;
using System.Text.RegularExpressions;
using System.Collections.Generic;
using CmnClsLib.Class;
using CmnClsLib.Module;

// 2026/08/08 Gemini 3.6 Flash (High) Review & Modified

namespace FileCleaner.Class
{
    public class ClsCfgFile
    {
        private readonly ClsLogger _logger;

        public List<ClsBaseDir> TargetList { get; set; } = [];
        public List<string> ListStr { get; set; } = [];
        public int Verbose { get; set; } = 0;
        public int Timeout { get; set; } = 3600;
        public string MachineName { get; set; } = Environment.MachineName.ToUpper();
        public string Delimiter { get; set; } = @";|";

        /// <summary>
        /// <see cref="ClsCfgFile"/> クラスの新しいインスタンスを初期化します。
        /// </summary>
        /// <param name="logger">ログ出力を行う <see cref="ClsLogger"/> インスタンス。</param>
        /// <example>
        /// <code>
        /// var logger = new ClsLogger();
        /// var cfgFile = new ClsCfgFile(logger);
        /// </code>
        /// </example>
        public ClsCfgFile(ClsLogger logger)
        {
            Encoding.RegisterProvider(CodePagesEncodingProvider.Instance);
            _logger = logger;
        }

        /// <summary>
        /// 指定された設定ファイルを読み込み、設定内容を解析してターゲットリストに追加します。
        /// </summary>
        /// <param name="configFilePath">読み込む設定ファイルのパス。</param>
        /// <param name="callAddTarget">ターゲット追加処理を行う場合は <c>true</c>。ファイル構造チェックのみを行う場合は <c>false</c>。</param>
        /// <returns>読み込みおよび解析が成功した場合は <c>true</c>、例外が発生した場合は <c>false</c>。</returns>
        /// <example>
        /// <code>
        /// var logger = new ClsLogger();
        /// var cfgFile = new ClsCfgFile(logger);
        /// bool success = cfgFile.ReadConfig(@"C:\config\cleaner.cfg", true);
        /// </code>
        /// </example>
        public bool ReadConfig(string configFilePath, bool callAddTarget)
        {
            const string METHOD_NAME = "[ClsCfgFile.ReadConfig()]";
            int currentLineNumber = 0;
            bool isSuccess = true;
            string regexPattern = $@"^\s*(?<KEY>[^#{Delimiter}]+)\s*[{Delimiter}]\s*(?<VAL>.+)\s*$";
            Regex regex = new(regexPattern);

            try
            {
                using StreamReader reader = new(configFilePath, MdlFile.DetectFileEncoding(configFilePath) ?? Encoding.Default);
                string? currentLine;
                while ((currentLine = reader.ReadLine()) != null)
                {
                    currentLineNumber++;
                    string trimmedLine = currentLine.Trim();
                    if (Verbose > 5)
                    {
                        _logger.WriteLine(MdlConst.LVL_DEBUG, $"{METHOD_NAME}[{currentLineNumber:D4}] LINE = {trimmedLine}");
                    }
                    Match match = regex.Match(trimmedLine);
                    if (match.Success)
                    {
                        string[] keys = match.Groups["KEY"].Value.Split(',', StringSplitOptions.TrimEntries);
                        bool isMatch = Array.Exists(keys, target =>
                            target.Equals("ALL", StringComparison.OrdinalIgnoreCase) ||
                            target.Equals("LOCALHOST", StringComparison.OrdinalIgnoreCase) ||
                            Regex.IsMatch(MachineName, target, RegexOptions.IgnoreCase));

                        if (isMatch && callAddTarget)
                        {
                            AddTarget($"{currentLineNumber};{match.Groups["VAL"].Value.Trim()}");
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                isSuccess = false;
                _logger.WriteLine(MdlConst.LVL_E, $"{METHOD_NAME}[LINE:{currentLineNumber}] Exception : {ex.Message}");
            }
            return isSuccess;
        }

        /// <summary>
        /// 設定ファイルから読み込んだ1行の文字列を解析し、ターゲット情報を設定リストに追加します。
        /// </summary>
        /// <param name="line">設定ファイルの行文字列。</param>
        /// <returns>解析およびリスト追加が正常に行われた場合は <c>true</c>、構文エラーや行番号が無効な場合は <c>false</c>。</returns>
        /// <example>
        /// <code>
        /// var logger = new ClsLogger();
        /// var cfgFile = new ClsCfgFile(logger);
        /// bool result = cfgFile.AddTarget("0001;1;ALL;7;C:\\Logs;0;0");
        /// </code>
        /// </example>
        public bool AddTarget(string line)
        {
            const string METHOD_NAME = "[ClsCfgFile.AddTarget()]";
            ClsBaseDir baseDir = new()
            {
                Timeout = Timeout,
                Verbose = Verbose
            };
            int columnIndex = 0;
            Regex delimiterRegex = new($@"\s*[{Delimiter}]\s*");
            bool isDate = false;
            bool isOk = true;

            string[] lineParts = delimiterRegex.Split(line);

            // 01:IDX-00：削除対象タイプフラグ
            string stringValue = lineParts.Length > 0 ? lineParts[columnIndex].Trim() : "";

            if (!string.IsNullOrEmpty(stringValue) && MdlUtil.IsNumeric(stringValue))
            {
                baseDir.LineNo = int.Parse(stringValue);
                baseDir.Verbose = Verbose > 2 ? Verbose : MdlUtil.ParseInt(stringValue, 1) - 1;
            }
            if (baseDir.LineNo == 0) return false;

            string formattedLineNumber = baseDir.LineNo.ToString("D4");

            if (Verbose > 4)
            {
                _logger.WriteLine(MdlConst.LVL_DEBUG, $"{METHOD_NAME}[{formattedLineNumber}] HIT = {string.Join("|", lineParts)}");
            }

            // 必須行
            if (lineParts.Length > 2)
            {
                // 02:IDX-01：削除実行フラグ
                columnIndex++;
                stringValue = lineParts.Length > columnIndex ? lineParts[columnIndex].Trim() : "";
                if (MdlUtil.IsNumeric(stringValue))
                {
                    int execFlagValue = MdlUtil.ParseInt(stringValue, 0);
                    baseDir.IsExec = execFlagValue > 0;
                }

                // 03:IDX-02：削除対象タイプ
                columnIndex++;
                stringValue = lineParts.Length > columnIndex ? lineParts[columnIndex].Trim().ToUpper() : "";
                if (!string.IsNullOrEmpty(stringValue))
                {
                    foreach (string element in MdlUtil.ParseCsvToList(null, stringValue, @"[,\/]", Verbose, true))
                    {
                        string typeString = element.Trim().ToUpper();
                        switch (typeString)
                        {
                            case "ALL":
                                baseDir.IsRmFile = true;
                                baseDir.IsRmDir = true;
                                baseDir.IsRmEmptyDir = true;
                                baseDir.IsRmSymlink = true;
                                baseDir.IsSymLink = true;
                                break;
                            case "F":
                                baseDir.IsRmFile = true;
                                break;
                            case "D":
                                baseDir.IsRmDir = true;
                                break;
                            case "E":
                                baseDir.IsRmEmptyDir = true;
                                break;
                            case "S":
                                baseDir.IsSymLink = true;
                                break;
                            case "SRM":
                                baseDir.IsRmSymlink = true;
                                break;
                            case "C":
                                baseDir.IsCreationTime = true;
                                break;
                            case "NAME":
                                baseDir.IsDateByName = true;
                                break;
                            case "NIR":
                                baseDir.IsIncHitRecursive = false;
                                break;
                            case "NXR":
                                baseDir.IsExcHitRecursive = false;
                                break;
                            case "NORM":
                                baseDir.IsRm = false;
                                break;
                            case "DAYS":
                                baseDir.IsDays = true;
                                break;
                            case "DATE":
                                isDate = true;
                                break;
                            case "GEN":
                                baseDir.ActionCode = ClsBaseDir.ACTION_GEN_DELETE;
                                baseDir.MaxDepth = 0;
                                break;
                            case "TSN":
                                baseDir.DateTimeMode = ClsBaseDir.DATETIME_NOW;
                                break;
                            case "TST":
                                baseDir.DateTimeMode = ClsBaseDir.DATETIME_TODAY;
                                break;
                            case "TSY":
                                baseDir.DateTimeMode = ClsBaseDir.DATETIME_YESTERDAY;
                                break;
                            case "TSF":
                                baseDir.DateTimeMode = ClsBaseDir.DATETIME_FILEINFO;
                                break;
                            case "NA-MKDIR":
                                baseDir.NaRetCode = MdlConst.LVL_I;
                                baseDir.IsMkRmBaseDir = true;
                                break;
                            case "NA-I":
                                baseDir.NaRetCode = MdlConst.LVL_I;
                                break;
                            case "NA-W":
                                baseDir.NaRetCode = MdlConst.LVL_W;
                                break;
                            case "NA-E":
                                baseDir.NaRetCode = MdlConst.LVL_E;
                                break;
                            case "ERR-I":
                                baseDir.ErrRetCode = MdlConst.LVL_I;
                                break;
                            case "ERR-W":
                                baseDir.ErrRetCode = MdlConst.LVL_W;
                                break;
                            case "ERR-E":
                                baseDir.ErrRetCode = MdlConst.LVL_E;
                                break;
                            case "NORMAL":
                                baseDir.IsAlwaysNormal = true;
                                break;
                            case "NEGATIVE":
                                baseDir.IsErrorAtNegativeValue = true;
                                break;
                            case "CWD":
                                baseDir.WorkDir = "objBaseDir.Path";
                                break;
                            case "CWD-MKDIR":
                                baseDir.WorkDir = "objBaseDir.StrMkdirPath";
                                break;
                            case "SHOW-PARAM":
                                baseDir.IsShowCmdParam = true;
                                break;
                            case "PRERM":
                                baseDir.IsPreRmFile = true;
                                break;
                            case "M-EXE":
                                baseDir.ExecModeCode = ClsBaseDir.EXEC_MODE_EXE;
                                break;
                            case "M-CMD":
                                baseDir.ExecModeCode = ClsBaseDir.EXEC_MODE_CMD;
                                break;
                            case "M-PS":
                                baseDir.ExecModeCode = ClsBaseDir.EXEC_MODE_PS;
                                break;
                            default:
                                string warnVal = GetRegexGroupValue(typeString, @"^W(?<VAL>[0-9]+)$", "VAL");
                                if (!string.IsNullOrEmpty(warnVal))
                                {
                                    baseDir.WarnThreshold = MdlUtil.ParseInt(warnVal, 0);
                                }
                                string errVal = GetRegexGroupValue(typeString, @"^E(?<VAL>[0-9]+)$", "VAL");
                                if (!string.IsNullOrEmpty(errVal))
                                {
                                    baseDir.ErrorThreshold = MdlUtil.ParseInt(errVal, 0);
                                }
                                string priorityVal = GetRegexGroupValue(typeString, @"^P(?<VAL>[0-9])$", "VAL");
                                if (!string.IsNullOrEmpty(priorityVal))
                                {
                                    baseDir.Priority = Math.Min(MdlUtil.ParseInt(priorityVal, 0), 5);
                                }
                                string timeoutVal = GetRegexGroupValue(typeString, @"^TIMEOUT(?<VAL>[0-9])$", "VAL");
                                if (!string.IsNullOrEmpty(timeoutVal))
                                {
                                    baseDir.Timeout = MdlUtil.ParseInt(timeoutVal, Timeout);
                                }
                                break;
                        }
                    }
                }

                // 04:IDX-03：経過日数／保存世代
                columnIndex++;
                stringValue = lineParts.Length > columnIndex ? lineParts[columnIndex].Trim() : "";
                if (!string.IsNullOrEmpty(stringValue))
                {
                    if (baseDir.ActionCode == ClsBaseDir.ACTION_GEN_DELETE)
                    {
                        baseDir.Generation = MdlUtil.ParseInt(stringValue, ClsBaseDir.GENERATION);
                    }
                    else
                    {
                        if (isDate)
                        {
                            if (MdlDate.TryParseDateTime(stringValue, out DateTime parsedDate))
                            {
                                baseDir.IsTerm = true;
                                baseDir.ThresholdDate = parsedDate;
                                baseDir.Term = (DateTime.Now - baseDir.ThresholdDate).Days;
                            }
                        }
                        else
                        {
                            baseDir.IsTerm = true;
                            baseDir.Term = MdlUtil.ParseDouble(stringValue, 0.0);
                            if (baseDir.Term < 0)
                            {
                                baseDir.IsNew = true;
                                baseDir.Term = Math.Abs(baseDir.Term);
                            }
                            baseDir.ThresholdDate = baseDir.IsDays
                                ? DateTime.Today.AddDays(-1.0 * baseDir.Term)
                                : DateTime.Now.AddDays(-1.0 * baseDir.Term);
                        }
                    }
                }

                // 05:IDX-04：パス
                columnIndex++;
                stringValue = lineParts.Length > columnIndex ? lineParts[columnIndex].Trim() : "";
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
                        bool exists = false;
                        if (baseDir.IsMkRmBaseDir)
                        {
                            if (MdlFile.CreateDirectory(stringValue) <= MdlFile.OK_MKDIR_HANTEI)
                            {
                                baseDir.IsBaseDir = true;
                                exists = true;
                            }
                            else
                            {
                                _logger.WriteLine(baseDir.NaRetCode, $"{METHOD_NAME}[LINE:{formattedLineNumber}][COL:05:PATH] FAILED TO MKDIR : {stringValue}");
                            }
                        }
                        if (!exists)
                        {
                            baseDir.IsExec = false;
                            baseDir.ReturnCode = baseDir.NaRetCode;
                            _logger.WriteLine(baseDir.NaRetCode, $"{METHOD_NAME}[LINE:{formattedLineNumber}][COL:05:PATH] NO SUCH A FILE OR DIRECTORY : {stringValue}");
                        }
                        break;
                }

                // 06:IDX-05：MIN
                columnIndex++;
                if (lineParts.Length > columnIndex)
                {
                    stringValue = lineParts[columnIndex].Trim();
                    if (!string.IsNullOrEmpty(stringValue))
                    {
                        if (MdlUtil.IsNumeric(stringValue))
                        {
                            baseDir.MinDepth = (ulong)Math.Abs(int.Parse(stringValue));
                        }
                        else
                        {
                            baseDir.IsOk = false;
                            _logger.WriteLine(MdlConst.LVL_E, $"{METHOD_NAME}[LINE:{formattedLineNumber}][COL:06:MIN] SYNTAX ERROR (NOT NUMERIC) : {stringValue}");
                        }
                    }
                }

                // 07:IDX-06：MAX
                columnIndex++;
                if (lineParts.Length > columnIndex)
                {
                    stringValue = lineParts[columnIndex].Trim();
                    if (!string.IsNullOrEmpty(stringValue))
                    {
                        if (MdlUtil.IsNumeric(stringValue))
                        {
                            baseDir.MaxDepth = (ulong)Math.Abs(int.Parse(stringValue));
                        }
                        else
                        {
                            baseDir.IsOk = false;
                            _logger.WriteLine(MdlConst.LVL_E, $"{METHOD_NAME}[LINE:{formattedLineNumber}][COL:07:MAX] SYNTAX ERROR (NOT NUMERIC) : {stringValue}");
                        }
                    }
                }

                // 08:IDX-07：INC:DIRS
                columnIndex++;
                baseDir.IsDirFilterOr = true;
                if (lineParts.Length > columnIndex)
                {
                    stringValue = lineParts[columnIndex].Trim();
                    if (!string.IsNullOrEmpty(stringValue))
                    {
                        ListStr = baseDir.IncDirsList;
                        if (!ParseCsvToList(stringValue))
                        {
                            _logger.WriteLine(MdlConst.LVL_E, $"{METHOD_NAME}[LINE:{formattedLineNumber}][COL:08:INC DIR] SYNTAX ERROR : {stringValue}");
                            baseDir.IsOk = false;
                        }
                    }
                }

                // 09:IDX-08：INC:FILES
                columnIndex++;
                if (lineParts.Length > columnIndex)
                {
                    stringValue = lineParts[columnIndex].Trim();
                    if (!string.IsNullOrEmpty(stringValue))
                    {
                        ListStr = baseDir.IncFilesList;
                        if (!ParseCsvToList(stringValue))
                        {
                            _logger.WriteLine(MdlConst.LVL_E, $"{METHOD_NAME}[LINE:{formattedLineNumber}][COL:09:INC FILE] SYNTAX ERROR : {stringValue}");
                            baseDir.IsOk = false;
                        }
                    }
                }

                // 10:IDX-09：EXC:DIRS
                columnIndex++;
                if (lineParts.Length > columnIndex)
                {
                    stringValue = lineParts[columnIndex].Trim();
                    if (!string.IsNullOrEmpty(stringValue))
                    {
                        ListStr = baseDir.ExcDirsList;
                        if (!ParseCsvToList(stringValue))
                        {
                            _logger.WriteLine(MdlConst.LVL_E, $"{METHOD_NAME}[LINE:{formattedLineNumber}][COL:10:EXC DIR] SYNTAX ERROR : {stringValue}");
                            baseDir.IsOk = false;
                        }
                    }
                }

                // 11:IDX-10：EXC:FILES
                columnIndex++;
                if (lineParts.Length > columnIndex)
                {
                    stringValue = lineParts[columnIndex].Trim();
                    if (!string.IsNullOrEmpty(stringValue))
                    {
                        ListStr = baseDir.ExcFilesList;
                        if (!ParseCsvToList(stringValue))
                        {
                            _logger.WriteLine(MdlConst.LVL_E, $"{METHOD_NAME}[LINE:{formattedLineNumber}][COL:11:EXC FILE] SYNTAX ERROR : {stringValue}");
                            baseDir.IsOk = false;
                        }
                    }
                }

                // 12:IDX-11：ファイル削除前コマンド
                columnIndex++;
                if (lineParts.Length > columnIndex)
                {
                    stringValue = lineParts[columnIndex].Trim();
                    if (!string.IsNullOrEmpty(stringValue))
                    {
                        baseDir.PreRmCmd = stringValue;
                        baseDir.IsPreRmCmd = true;
                    }
                }

                // 13:IDX-12：ファイル削除前作成ディレクトリ
                columnIndex++;
                if (lineParts.Length > columnIndex)
                {
                    stringValue = MdlUtil.TrimQuotes(lineParts[columnIndex]);
                    if (!string.IsNullOrEmpty(stringValue))
                    {
                        baseDir.MkdirPath = stringValue;
                        baseDir.IsMkDir = true;
                    }
                }

                // 14:IDX-13：日付設定先
                columnIndex++;
                if (lineParts.Length > columnIndex)
                {
                    stringValue = MdlUtil.TrimQuotes(lineParts[columnIndex]);
                    if (!string.IsNullOrEmpty(stringValue))
                    {
                        baseDir.SetDateTimeTo = stringValue;
                        baseDir.IsSetDateTime = true;
                    }
                }

                // 調整
                if (baseDir.WorkDir == "objBaseDir.Path") baseDir.WorkDir = baseDir.Path;
                if (baseDir.WorkDir == "objBaseDir.StrMkdirPath") baseDir.WorkDir = baseDir.MkdirPath;
            }
            // 不具合行
            else
            {
                _logger.WriteLine(MdlConst.LVL_E, $"{METHOD_NAME}[LINE:{formattedLineNumber}] SYNTAX ERROR : {line}");
                isOk = false;
            }

            TargetList.Add(baseDir);

            if (!baseDir.IsOk) isOk = false;

            return isOk;
        }

        /// <summary>
        /// CSV形式の文字列をリストに変換して格納します。
        /// </summary>
        /// <param name="csvString">CSV形式の文字列。</param>
        /// <returns>処理が正常に完了した場合は <c>true</c>、失敗した場合は <c>false</c>。</returns>
        /// <example>
        /// <code>
        /// var logger = new ClsLogger();
        /// var cfgFile = new ClsCfgFile(logger);
        /// bool success = cfgFile.ParseCsvToList("*.log,*.tmp,*.bak");
        /// </code>
        /// </example>
        public bool ParseCsvToList(string csvString)
        {
            if (string.IsNullOrEmpty(csvString)) return true;

            const string METHOD_NAME = "[ClsCfgFile.ParseCsvToList()]";
            string[] csvArray = csvString.Split(',');

            foreach (string element in csvArray)
            {
                string tempString = element.StartsWith('*') ? "." + element : element;

                try
                {
                    ListStr.Add(tempString);
                }
                catch
                {
                    _logger.WriteLine(MdlConst.LVL_W, $"{METHOD_NAME} SYNTAX ERROR : {csvString}");
                    return false;
                }
            }

            return true;
        }

        /// <summary>
        /// 指定された正規表現パターンに基づいて、入力文字列からキーに対応する値を抽出します。
        /// </summary>
        /// <param name="input">入力文字列。</param>
        /// <param name="pattern">正規表現パターン。</param>
        /// <param name="key">抽出する値のキー名。</param>
        /// <returns>キーに対応する抽出された値。一致しない場合は空文字列。</returns>
        /// <example>
        /// <code>
        /// string value = ClsCfgFile.GetRegexGroupValue("W30", @"^W(?&lt;VAL&gt;[0-9]+)$", "VAL");
        /// // value は "30" となります
        /// </code>
        /// </example>
        public static string GetRegexGroupValue(string input, string pattern, string key)
        {
            Match match = Regex.Match(input, pattern);
            return match.Success ? match.Groups[key].Value : "";
        }

    }
}
