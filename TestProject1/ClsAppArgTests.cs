using System;
using System.IO;
using CmnClsLib.Class;
using FileCleaner.Class;
using Xunit;

namespace FileCleanerClsAppArgTests
{
    public class ClsAppArgTests
    {
        private ClsLogger CreateLogger()
        {
            var logger = new ClsLogger();
            logger.SetValueByKey(ClsLogger.IS_CONSOLE, "false"); // コンソール出力オフ（テスト時の標準出力を汚さないため）
            return logger;
        }

        [Fact]
        public void Constructor_NullLogger_ThrowsArgumentNullException()
        {
            Assert.Throws<ArgumentNullException>(() => new ClsAppArg(null!));
        }

        [Fact]
        public void Constructor_ValidLogger_InitializesProperties()
        {
            var logger = CreateLogger();
            var sut = new ClsAppArg(logger);

            Assert.NotNull(sut);
            Assert.Equal(0, sut.UsageFlag);
            Assert.True(sut.IsList);
            Assert.True(sut.IsDiff);
            Assert.True(sut.IsAscending);
        }

        [Fact]
        public void Parse_NullArgs_ThrowsArgumentNullException()
        {
            var logger = CreateLogger();
            var sut = new ClsAppArg(logger);

            Assert.Throws<ArgumentNullException>(() => sut.Parse(null!));
        }

        [Fact]
        public void Parse_BasicPathAndTerm_ParsesSuccessfully()
        {
            var logger = CreateLogger();
            var sut = new ClsAppArg(logger);
            string tempDir = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "tmp_test_dir");
            Directory.CreateDirectory(tempDir);

            try
            {
                string[] args = ["-path", tempDir, "-term", "14", "-clean"];
                bool result = sut.Parse(args);

                Assert.True(result);
                Assert.False(sut.IsList); // -clean が指定されたため false
                Assert.Single(sut.TargetList);
                Assert.Equal(14, sut.TargetList[0].Term);
            }
            finally
            {
                if (Directory.Exists(tempDir))
                {
                    Directory.Delete(tempDir, true);
                }
            }
        }

        [Fact]
        public void Parse_HelpOption_SetsUsageFlag()
        {
            var logger = CreateLogger();
            var sut = new ClsAppArg(logger);
            string[] args = ["-help-conf"];

            bool result = sut.Parse(args);

            Assert.True(result);
            Assert.Equal(ClsAppArg.USAGE_SHOW_SAMPLE_CONFIG, sut.UsageFlag);
        }

        [Fact]
        public void PrintDefinition_NullBaseDir_ThrowsArgumentNullException()
        {
            var logger = CreateLogger();
            var sut = new ClsAppArg(logger);

            Assert.Throws<ArgumentNullException>(() => sut.PrintDefinition(null!));
        }

        [Fact]
        public void PrintDefinition_ValidBaseDir_ExecutesWithoutException()
        {
            var logger = CreateLogger();
            var sut = new ClsAppArg(logger);
            var baseDir = new ClsBaseDir
            {
                Path = @"C:\TestPath",
                Term = 7
            };

            var exception = Record.Exception(() => sut.PrintDefinition(baseDir));

            Assert.Null(exception);
        }

        [Fact]
        public void Usage_ExecutesWithoutException()
        {
            var logger = CreateLogger();
            var sut = new ClsAppArg(logger);

            var exception = Record.Exception(() => sut.Usage());

            Assert.Null(exception);
        }

        [Fact]
        public void ShowSampleConfig_ExecutesWithoutException()
        {
            var logger = CreateLogger();
            var sut = new ClsAppArg(logger);

            var exception = Record.Exception(() => sut.ShowSampleConfig());

            Assert.Null(exception);
        }
    }
}
