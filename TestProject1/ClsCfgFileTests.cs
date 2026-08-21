using System;
using System.IO;
using Xunit;
using FileCleaner.Class;
using CmnClsLib.Class;

namespace FileCleaner.Tests
{
    public class ClsCfgFileTests : IDisposable
    {
        private readonly ClsLogger _logger;
        private readonly string _tmpDir;

        public ClsCfgFileTests()
        {
            _logger = new ClsLogger();
            // ls ${Env:USERPROFILE}\AppData\Local\Temp\UnitTest
            _tmpDir = Path.Combine(System.IO.Path.GetTempPath(), @"UnitTest", @"FileCleaner", @"ClsCfgFile");
            if (!Directory.Exists(_tmpDir))
            {
                Directory.CreateDirectory(_tmpDir);
            }
        }

        public void Dispose()
        {
            if (Directory.Exists(_tmpDir))
            {
                try
                {
                    Directory.Delete(_tmpDir, true);
                }
                catch
                {
                    // テスト実行後のクリーンアップ失敗は無視
                }
            }
        }

        [Fact]
        public void Constructor_Initialization_Success()
        {
            var cfgFile = new ClsCfgFile(_logger);
            Assert.NotNull(cfgFile.TargetList);
            Assert.NotNull(cfgFile.ListStr);
            Assert.Equal(0, cfgFile.Verbose);
            Assert.Equal(3600, cfgFile.Timeout);
            Assert.Equal(@";|", cfgFile.Delimiter);
        }

        [Fact]
        public void GetRegexGroupValue_ValidInput_ReturnsGroupValue()
        {
            string value = ClsCfgFile.GetRegexGroupValue("W30", @"^W(?<VAL>[0-9]+)$", "VAL");
            Assert.Equal("30", value);
        }

        [Fact]
        public void GetRegexGroupValue_InvalidInput_ReturnsEmptyString()
        {
            string value = ClsCfgFile.GetRegexGroupValue("INVALID", @"^W(?<VAL>[0-9]+)$", "VAL");
            Assert.Equal("", value);
        }

        [Fact]
        public void ParseCsvToList_WildcardAndNormal_AddsToTargetList()
        {
            var cfgFile = new ClsCfgFile(_logger);
            bool result = cfgFile.ParseCsvToList("*.log,test.txt");

            Assert.True(result);
            Assert.Contains(".*.log", cfgFile.ListStr);
            Assert.Contains("test.txt", cfgFile.ListStr);
        }

        [Fact]
        public void AddTarget_ValidLine_AddsBaseDirToTargetList()
        {
            var cfgFile = new ClsCfgFile(_logger);
            // 01:LineNo; 02:IsExec; 03:Type; 04:Term/Gen; 05:Path; 06:Min; 07:Max
            string validLine = $"100;1;ALL;7;{_tmpDir};0;10";

            bool result = cfgFile.AddTarget(validLine);

            Assert.True(result);
            Assert.Single(cfgFile.TargetList);
            var target = cfgFile.TargetList[0];
            Assert.Equal(100, target.LineNo);
            Assert.True(target.IsExec);
            Assert.True(target.IsRmFile);
            Assert.True(target.IsRmDir);
        }

        [Fact]
        public void AddTarget_InvalidLineNo_ReturnsFalse()
        {
            var cfgFile = new ClsCfgFile(_logger);
            string invalidLine = "0;1;ALL;7;C:\\tmp;0;10";

            bool result = cfgFile.AddTarget(invalidLine);

            Assert.False(result);
            Assert.Empty(cfgFile.TargetList);
        }

        [Fact]
        public void ReadConfig_ValidConfigFile_ReadsAndAddsTargets()
        {
            var cfgFile = new ClsCfgFile(_logger);
            string filePath = Path.Combine(_tmpDir, "test_config.cfg");
            File.WriteAllLines(filePath, new[]
            {
                "# Comment Line",
                "ALL ; 100;1;F;30;" + _tmpDir + ";0;5",
                "LOCALHOST ; 200;1;D;15;" + _tmpDir + ";1;2"
            });

            bool result = cfgFile.ReadConfig(filePath, true);

            Assert.True(result);
            Assert.Equal(2, cfgFile.TargetList.Count);
        }
    }
}
