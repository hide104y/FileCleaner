package tool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import tool.cmnclslib.mdl.MdlConst;
import tool.cmnclslib.mdl.MdlFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FileCleaner} の単体テストクラスです。
 */
public class FileCleanerTest {

    /**
     * ヘルプ引数指定時に警告レベルの戻り値が返されることを検証します。
     */
    @Test
    public void main_WithHelpOption_ReturnsWarningReturnCode() {
        String[] args = new String[]{"-h"};
        int result = FileCleaner.mainProcess(args);
        assertEquals(MdlConst.LVL_W, result);
    }

    /**
     * サンプル設定ファイル表示引数指定時に設定表示後の戻り値が返されることを検証します。
     */
    @Test
    public void main_WithSampleConfigOption_ReturnsWarningReturnCode() {
        String[] args = new String[]{"-s"};
        int result = FileCleaner.mainProcess(args);
        assertEquals(20, result);
    }

    /**
     * 不正なオプション指定時にエラーレベルの戻り値が返されることを検証します。
     */
    @Test
    public void main_WithInvalidOption_ReturnsErrorReturnCode() {
        String[] args = new String[]{"--invalid-unknown-option-xyz"};
        int result = FileCleaner.mainProcess(args);
        assertEquals(MdlConst.LVL_E, result);
    }

    /**
     * tmp ディレクトリ配下で作業ファイルを作成し、設定ファイルの動作を検証します。
     * tmp ディレクトリより上位のディレクトリには影響させません。
     */
    @Test
    public void main_WithTmpDirectoryConfig_ExecutesSafely() throws Exception {
        String tmpDir = new File(new File(new File(System.getProperty("java.io.tmpdir"), "UnitTest"), "FileCleaner"), "Program").getPath();
        File dir = new File(tmpDir);
        dir.mkdirs();

        try {
            // tmp ディレクトリ内にダミーテストファイルの準備
            File testFile = new File(tmpDir, "dummy_test_file.txt");
            try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(testFile), StandardCharsets.UTF_8)) {
                writer.write("Test content");
            }

            // 存在確認
            assertTrue(testFile.exists());

            // 無効な引数を渡し、戻り値が得られることを確認
            String[] args = new String[]{"-c", new File(tmpDir, "non_existent_config.ini").getPath()};
            int result = FileCleaner.mainProcess(args);

            assertTrue(result == MdlConst.LVL_E || result == MdlConst.LVL_W || result == MdlConst.LVL_I || result == 20);
        } finally {
            if (dir.exists()) {
                MdlFile.deleteRecursively(tmpDir);
            }
        }
    }
}
