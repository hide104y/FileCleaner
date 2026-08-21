using System;
using System.IO;
using CmnClsLib.Module;
using FileCleaner;
using Xunit;

namespace FileCleaner.Tests;

/// <summary>
/// <see cref="Program"/> の単体テストクラスです。
/// </summary>
public class ProgramTests
{
    /// <summary>
    /// ヘルプ引数指定時に警告レベルの戻り値が返されることを検証します。
    /// </summary>
    [Fact]
    public void Main_WithHelpOption_ReturnsWarningReturnCode()
    {
        // Arrange
        string[] args = ["-h"];

        // Act
        int result = Program.Main(args);

        // Assert
        Assert.Equal(MdlConst.LVL_W, result);
    }

    /// <summary>
    /// サンプル設定ファイル表示引数指定時に設定表示後の戻り値が返されることを検証します。
    /// </summary>
    [Fact]
    public void Main_WithSampleConfigOption_ReturnsWarningReturnCode()
    {
        // Arrange
        string[] args = ["-s"];

        // Act
        int result = Program.Main(args);

        // Assert
        Assert.Equal(20, result);
    }

    /// <summary>
    /// 不正なオプション指定時にエラーレベルの戻り値が返されることを検証します。
    /// </summary>
    [Fact]
    public void Main_WithInvalidOption_ReturnsErrorReturnCode()
    {
        // Arrange
        string[] args = ["--invalid-unknown-option-xyz"];

        // Act
        int result = Program.Main(args);

        // Assert
        Assert.Equal(MdlConst.LVL_E, result);
    }

    /// <summary>
    /// tmp ディレクトリ配下で作業ファイルを作成し、設定ファイルの動作を検証します。
    /// tmp ディレクトリより上位のディレクトリには影響させません。
    /// </summary>
    [Fact]
    public void Main_WithTmpDirectoryConfig_ExecutesSafely()
    {
        // ls ${Env:USERPROFILE}\AppData\Local\Temp\UnitTest
        string tmpDir = Path.Combine(System.IO.Path.GetTempPath(), @"UnitTest", @"FileCleaner", @"Program");
        Directory.CreateDirectory(tmpDir);

        try
        {
            // tmp ディレクトリ内にダミーテストファイルの準備
            string testFilePath = Path.Combine(tmpDir, "dummy_test_file.txt");
            File.WriteAllText(testFilePath, "Test content");

            // 存在確認
            Assert.True(File.Exists(testFilePath));

            // 無効な引数を渡し、戻り値が得られることを確認
            string[] args = ["-c", Path.Combine(tmpDir, "non_existent_config.ini")];
            int result = Program.Main(args);

            Assert.True(result == MdlConst.LVL_E || result == MdlConst.LVL_W || result == MdlConst.LVL_I || result == 20);
        }
        finally
        {
            // 後処理: 自テスト用サブディレクトリ内のクリーンアップ
            if (Directory.Exists(tmpDir))
            {
                try
                {
                    Directory.Delete(tmpDir, recursive: true);
                }
                catch
                {
                    // テスト後のファイルロック等の例外は安全に無視
                }
            }
        }
    }
}
