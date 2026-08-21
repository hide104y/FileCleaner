using System;
using System.Collections.Generic;
using CmnClsLib.Module;

// 2026/08/08 Gemini 3.6 Flash (High) Review & Modified

namespace FileCleaner.Class
{
    public class ClsBaseDir
    {
        public const int ACTION_DELETE = 0;
        public const int ACTION_GEN_DELETE = 1;
        public const int GENERATION = 10;
        public const int DATETIME_NOW = 0;
        public const int DATETIME_TODAY = 1;
        public const int DATETIME_YESTERDAY = 2;
        public const int DATETIME_FILEINFO = 3;
        public const int EXEC_MODE_NORMAL = 0;
        public const int EXEC_MODE_CMD = 1;
        public const int EXEC_MODE_PS = 2;
        public const int EXEC_MODE_PSC = 3;
        public const int EXEC_MODE_EXE = 4;

        public int ActionCode { get; set; } = ACTION_DELETE;
        public int LineNo { get; set; } = 0;
        public int Verbose { get; set; } = 0;
        public int NaRetCode { get; set; } = MdlConst.LVL_I;
        public int ErrRetCode { get; set; } = MdlConst.LVL_W;
        public int ReturnCode { get; set; } = MdlConst.LVL_I;
        public bool IsOk { get; set; } = true;
        public bool IsExec { get; set; } = true;
        public bool IsRm { get; set; } = true;
        public string TargetType { get; set; } = "f";
        public string Path { get; set; } = "";
        public bool IsBaseDir { get; set; } = false;
        public bool IsTerm { get; set; } = false;
        public bool IsDays { get; set; } = false;
        public bool IsNew { get; set; } = false;
        public double Term { get; set; } = 0.0;
        public DateTime ThresholdDate { get; set; }
        public bool IsRmFile { get; set; } = false;
        public bool IsRmEmptyDir { get; set; } = false;
        public bool IsRmDir { get; set; } = false;
        public bool IsRmSymlink { get; set; } = false;
        public bool IsSymLink { get; set; } = false;
        public bool IsDq { get; set; } = false;
        public bool IsDiff { get; set; } = true;
        public string MkdirPath { get; set; } = "";
        public bool IsMkDir { get; set; } = false;
        public bool IsMkRmBaseDir { get; set; } = false;
        public string WorkDir { get; set; } = "";
        public string PreRmCmd { get; set; } = "";
        public bool IsPreRmCmd { get; set; } = false;
        public bool IsPreRmFile { get; set; } = false;
        public int ExecModeCode { get; set; } = EXEC_MODE_EXE;
        public int Priority { get; set; } = 3;
        public int WarnThreshold { get; set; } = MdlConst.INT_NULL;
        public int ErrorThreshold { get; set; } = MdlConst.INT_NULL;
        public int Timeout { get; set; } = 3600;
        public bool IsErrorAtNegativeValue { get; set; } = false;
        public bool IsAlwaysNormal { get; set; } = false;
        public bool IsShowCmd { get; set; } = false;
        public bool IsShowOutput { get; set; } = false;
        public bool IsShowExitCode { get; set; } = false;
        public bool IsShowCmdParam { get; set; } = false;
        public bool IsSetDateTime { get; set; } = false;
        public string SetDateTimeTo { get; set; } = "";
        public int DateTimeMode { get; set; } = DATETIME_NOW;
        public bool IsCreationTime { get; set; } = false;
        public bool IsDateByName { get; set; } = false;
        public ulong MinDepth { get; set; } = 0;
        public ulong MaxDepth { get; set; } = MdlConst.ULNG_MAX;
        public bool IsRegIncBasename { get; set; } = true;
        public bool IsRegExcBasename { get; set; } = true;
        public bool IsIncHitRecursive { get; set; } = true;
        public bool IsExcHitRecursive { get; set; } = true;
        public bool IsDirFilterOr { get; set; } = false;
        public List<string> IncDirsList { get; set; } = [];
        public List<string> IncFilesList { get; set; } = [];
        public List<string> ExcDirsList { get; set; } = [];
        public List<string> ExcFilesList { get; set; } = [];
        public int Generation { get; set; } = GENERATION;
        public List<string> TargetList { get; set; } = [];

        /// <summary>
        /// <see cref="ClsBaseDir"/> クラスの新しいインスタンスを初期化します。
        /// </summary>
        /// <example>
        /// <code>
        /// var baseDir = new ClsBaseDir();
        /// </code>
        /// </example>
        public ClsBaseDir()
        {
        }

        /// <summary>
        /// 日時モードを表す数値コードから、対応する文字列コード（"n", "t", "y", "f"）を取得します。
        /// </summary>
        /// <param name="dateTimeMode">日時モード数値コード（例: <see cref="DATETIME_TODAY"/>）</param>
        /// <returns>日時モードを表す文字列コード</returns>
        /// <example>
        /// <code>
        /// var baseDir = new ClsBaseDir();
        /// string modeStr = baseDir.GetDateTimeModeString(ClsBaseDir.DATETIME_TODAY); // "t"
        /// </code>
        /// </example>
        public string GetDateTimeModeString(int dateTimeMode) => dateTimeMode switch
        {
            DATETIME_TODAY => "t",
            DATETIME_YESTERDAY => "y",
            DATETIME_FILEINFO => "f",
            _ => "n"
        };

        /// <summary>
        /// 日時モードを表す文字列コード（"t", "today", "y", "yesterday", "f", "file"）を数値コードに変換します。
        /// </summary>
        /// <param name="modeString">日時モード文字列コード</param>
        /// <returns>日時モードを表す数値コード（該当しない場合は <see cref="DATETIME_NOW"/>）</returns>
        /// <example>
        /// <code>
        /// var baseDir = new ClsBaseDir();
        /// int mode = baseDir.ParseDateTimeMode("today"); // DATETIME_TODAY
        /// </code>
        /// </example>
        public int ParseDateTimeMode(string modeString) => modeString switch
        {
            "t" or "today" => DATETIME_TODAY,
            "y" or "yesterday" => DATETIME_YESTERDAY,
            "f" or "file" => DATETIME_FILEINFO,
            _ => DATETIME_NOW
        };

        /// <summary>
        /// アクションコード数値から対応するアクション文字列（"delete", "gendel"）を取得します。
        /// </summary>
        /// <param name="actionCode">アクション数値コード（例: <see cref="ACTION_GEN_DELETE"/>）</param>
        /// <returns>アクションを表す文字列コード</returns>
        /// <example>
        /// <code>
        /// var baseDir = new ClsBaseDir();
        /// string actionStr = baseDir.GetActionString(ClsBaseDir.ACTION_GEN_DELETE); // "gendel"
        /// </code>
        /// </example>
        public string GetActionString(int actionCode) => actionCode switch
        {
            ACTION_GEN_DELETE => "gendel",
            _ => "delete"
        };

        /// <summary>
        /// アクション文字列コード（"gendel" 等）からアクション数値コードに変換します。
        /// </summary>
        /// <param name="actionString">アクション文字列コード</param>
        /// <returns>アクション数値コード（該当しない場合は 0）</returns>
        /// <example>
        /// <code>
        /// var baseDir = new ClsBaseDir();
        /// int action = baseDir.ParseAction("gendel"); // ACTION_GEN_DELETE
        /// </code>
        /// </example>
        public int ParseAction(string actionString) => actionString switch
        {
            "gendel" => ACTION_GEN_DELETE,
            _ => 0
        };

    }
}
