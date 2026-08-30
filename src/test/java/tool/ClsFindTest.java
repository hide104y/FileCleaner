package tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tool.cmnclslib.cls.ClsLogger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ClsFind} の単体テストクラスです。
 */
public class ClsFindTest {

    @TempDir
    Path tempDir;

    private ClsLogger logger;
    private ClsAppArg appArg;
    private ClsFind clsFind;

    @BeforeEach
    void setUp() {
        logger = new ClsLogger();
        logger.setValueByKey(ClsLogger.IS_CONSOLE, "false");
        appArg = new ClsAppArg(logger);
        clsFind = new ClsFind(logger, appArg);
    }

    @Test
    @DisplayName("存在しないパスを指定した場合はスキップされ、trueを返すこと")
    void execute_NonExistentPath_SkipsAndReturnsTrue() {
        ClsBaseDir baseDir = new ClsBaseDir();
        baseDir.setPath(tempDir.resolve("non_existent_folder").toString());
        baseDir.setBaseDir(true);

        boolean result = clsFind.execute(baseDir);

        assertTrue(result, "存在しないパスはスキップされて正常終了すること");
    }

    @Test
    @DisplayName("非実行モード（リスト表示）では対象ファイルが削除されないこと")
    void execute_ListMode_DoesNotDeleteFiles() throws IOException {
        Path testFile = Files.createFile(tempDir.resolve("sample.txt"));
        Files.writeString(testFile, "hello");

        ClsBaseDir baseDir = new ClsBaseDir();
        baseDir.setPath(tempDir.toString());
        baseDir.setBaseDir(true);
        baseDir.setExec(false);
        baseDir.setRmFile(true);
        baseDir.setTerm(false); // 期間指定なし＝全対象

        boolean result = clsFind.execute(baseDir);

        assertTrue(result, "リストモード実行は成功すること");
        assertTrue(Files.exists(testFile), "リストモードではファイルが削除されないこと");
    }

    @Test
    @DisplayName("削除実行モードでは条件に一致するファイルが削除されること")
    void execute_ExecMode_DeletesMatchingFiles() throws IOException {
        Path testFile = Files.createFile(tempDir.resolve("delete_me.txt"));
        Files.writeString(testFile, "delete me");

        ClsBaseDir baseDir = new ClsBaseDir();
        baseDir.setPath(tempDir.toString());
        baseDir.setBaseDir(true);
        baseDir.setExec(true);
        baseDir.setRmFile(true);
        baseDir.setTerm(false);

        boolean result = clsFind.execute(baseDir);

        assertTrue(result, "削除実行が成功すること");
        assertFalse(Files.exists(testFile), "条件に一致したファイルが削除されていること");
    }

    @Test
    @DisplayName("空ディレクトリ削除フラグが有効な場合、空ディレクトリが削除されること")
    void execute_RmEmptyDir_DeletesEmptyDirectory() throws IOException {
        Path emptySubDir = Files.createDirectory(tempDir.resolve("empty_sub"));

        ClsBaseDir baseDir = new ClsBaseDir();
        baseDir.setPath(tempDir.toString());
        baseDir.setBaseDir(true);
        baseDir.setExec(true);
        baseDir.setRmEmptyDir(true);
        baseDir.setTerm(false);

        boolean result = clsFind.execute(baseDir);

        assertTrue(result, "空ディレクトリ削除実行が成功すること");
        assertFalse(Files.exists(emptySubDir), "空ディレクトリが削除されていること");
    }

    @Test
    @DisplayName("除外ファイルフィルタに一致するファイルは削除されないこと")
    void execute_ExcludeFilter_PreservesExcludedFiles() throws IOException {
        Path targetFile = Files.createFile(tempDir.resolve("delete.log"));
        Path preserveFile = Files.createFile(tempDir.resolve("keep.txt"));

        ClsBaseDir baseDir = new ClsBaseDir();
        baseDir.setPath(tempDir.toString());
        baseDir.setBaseDir(true);
        baseDir.setExec(true);
        baseDir.setRmFile(true);
        baseDir.setTerm(false);
        baseDir.setExcFilesList(List.of(".*\\.txt"));

        boolean result = clsFind.execute(baseDir);

        assertTrue(result, "フィルタ付き削除実行が成功すること");
        assertFalse(Files.exists(targetFile), "対象ファイルは削除されること");
        assertTrue(Files.exists(preserveFile), "除外ファイルは保持されること");
    }

    @Test
    @DisplayName("単一ファイルを対象パスに指定した場合に正常に削除されること")
    void execute_SingleFileTarget_DeletesSuccessfully() throws IOException {
        Path singleFile = Files.createFile(tempDir.resolve("single.log"));

        ClsBaseDir baseDir = new ClsBaseDir();
        baseDir.setPath(singleFile.toString());
        baseDir.setBaseDir(false);
        baseDir.setExec(true);
        baseDir.setRmFile(true);
        baseDir.setTerm(false);

        boolean result = clsFind.execute(baseDir);

        assertTrue(result, "単一ファイル削除が成功すること");
        assertFalse(Files.exists(singleFile), "単一ファイルが削除されていること");
    }

    @Test
    @DisplayName("事前コマンド（DryRun）が設定されている場合に正常に評価されること")
    void execute_PreRmCmdDryRun_ExecutesSuccessfully() throws IOException {
        Path testFile = Files.createFile(tempDir.resolve("precmd_test.log"));
        Files.writeString(testFile, "test data");

        ClsBaseDir baseDir = new ClsBaseDir();
        baseDir.setPath(tempDir.toString());
        baseDir.setBaseDir(true);
        baseDir.setExec(false); // DryRun
        baseDir.setRmFile(true);
        baseDir.setTerm(false);
        baseDir.setPreRmCmd("echo {}");
        baseDir.setPreRmCmd(true);

        boolean result = clsFind.execute(baseDir);

        assertTrue(result, "DryRun時の事前コマンド評価が成功すること");
        assertTrue(Files.exists(testFile), "DryRunではファイルが削除されないこと");
    }
}
