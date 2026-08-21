using System;
using FileCleaner.Class;
using Xunit;

namespace FileCleaner.Tests
{
    public class ClsBaseDirTests
    {
        [Fact]
        public void DefaultConstructor_InitializesDefaultValues()
        {
            var baseDir = new ClsBaseDir();

            Assert.Equal(ClsBaseDir.ACTION_DELETE, baseDir.ActionCode);
            Assert.Equal(0, baseDir.LineNo);
            Assert.Equal(0, baseDir.Verbose);
            Assert.True(baseDir.IsOk);
            Assert.True(baseDir.IsExec);
            Assert.True(baseDir.IsRm);
            Assert.Equal("f", baseDir.TargetType);
            Assert.Equal(ClsBaseDir.GENERATION, baseDir.Generation);
            Assert.Empty(baseDir.IncDirsList);
            Assert.Empty(baseDir.IncFilesList);
            Assert.Empty(baseDir.ExcDirsList);
            Assert.Empty(baseDir.ExcFilesList);
            Assert.Empty(baseDir.TargetList);
        }

        [Theory]
        [InlineData(ClsBaseDir.DATETIME_TODAY, "t")]
        [InlineData(ClsBaseDir.DATETIME_YESTERDAY, "y")]
        [InlineData(ClsBaseDir.DATETIME_FILEINFO, "f")]
        [InlineData(ClsBaseDir.DATETIME_NOW, "n")]
        [InlineData(999, "n")]
        public void GetDateTimeModeString_ReturnsExpectedString(int mode, string expected)
        {
            var baseDir = new ClsBaseDir();
            Assert.Equal(expected, baseDir.GetDateTimeModeString(mode));
        }

        [Theory]
        [InlineData("t", ClsBaseDir.DATETIME_TODAY)]
        [InlineData("today", ClsBaseDir.DATETIME_TODAY)]
        [InlineData("y", ClsBaseDir.DATETIME_YESTERDAY)]
        [InlineData("yesterday", ClsBaseDir.DATETIME_YESTERDAY)]
        [InlineData("f", ClsBaseDir.DATETIME_FILEINFO)]
        [InlineData("file", ClsBaseDir.DATETIME_FILEINFO)]
        [InlineData("invalid", ClsBaseDir.DATETIME_NOW)]
        public void ParseDateTimeMode_ReturnsExpectedInt(string input, int expected)
        {
            var baseDir = new ClsBaseDir();
            Assert.Equal(expected, baseDir.ParseDateTimeMode(input));
        }

        [Theory]
        [InlineData(ClsBaseDir.ACTION_GEN_DELETE, "gendel")]
        [InlineData(ClsBaseDir.ACTION_DELETE, "delete")]
        [InlineData(99, "delete")]
        public void GetActionString_ReturnsExpectedString(int actionCode, string expected)
        {
            var baseDir = new ClsBaseDir();
            Assert.Equal(expected, baseDir.GetActionString(actionCode));
        }

        [Theory]
        [InlineData("gendel", ClsBaseDir.ACTION_GEN_DELETE)]
        [InlineData("delete", 0)]
        [InlineData("unknown", 0)]
        public void ParseAction_ReturnsExpectedInt(string input, int expected)
        {
            var baseDir = new ClsBaseDir();
            Assert.Equal(expected, baseDir.ParseAction(input));
        }
    }
}
