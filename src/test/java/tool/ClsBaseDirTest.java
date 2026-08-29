package tool;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ClsBaseDir} の単体テストクラスです。
 */
public class ClsBaseDirTest {

    @Test
    public void defaultConstructor_InitializesDefaultValues() {
        ClsBaseDir baseDir = new ClsBaseDir();

        assertEquals(ClsBaseDir.ACTION_DELETE, baseDir.getActionCode());
        assertEquals(0, baseDir.getLineNo());
        assertEquals(0, baseDir.getVerbose());
        assertTrue(baseDir.isOk());
        assertTrue(baseDir.isExec());
        assertTrue(baseDir.isRm());
        assertEquals("f", baseDir.getTargetType());
        assertEquals(ClsBaseDir.GENERATION, baseDir.getGeneration());
        assertTrue(baseDir.getIncDirsList().isEmpty());
        assertTrue(baseDir.getIncFilesList().isEmpty());
        assertTrue(baseDir.getExcDirsList().isEmpty());
        assertTrue(baseDir.getExcFilesList().isEmpty());
        assertTrue(baseDir.getTargetList().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "1, t",
        "2, y",
        "3, f",
        "0, n",
        "999, n"
    })
    public void getDateTimeModeString_ReturnsExpectedString(int mode, String expected) {
        ClsBaseDir baseDir = new ClsBaseDir();
        assertEquals(expected, baseDir.getDateTimeModeString(mode));
    }

    @ParameterizedTest
    @CsvSource({
        "t, 1",
        "today, 1",
        "y, 2",
        "yesterday, 2",
        "f, 3",
        "file, 3",
        "invalid, 0"
    })
    public void parseDateTimeMode_ReturnsExpectedInt(String input, int expected) {
        ClsBaseDir baseDir = new ClsBaseDir();
        assertEquals(expected, baseDir.parseDateTimeMode(input));
    }

    @ParameterizedTest
    @CsvSource({
        "1, gendel",
        "0, delete",
        "99, delete"
    })
    public void getActionString_ReturnsExpectedString(int actionCode, String expected) {
        ClsBaseDir baseDir = new ClsBaseDir();
        assertEquals(expected, baseDir.getActionString(actionCode));
    }

    @ParameterizedTest
    @CsvSource({
        "gendel, 1",
        "delete, 0",
        "unknown, 0"
    })
    public void parseAction_ReturnsExpectedInt(String input, int expected) {
        ClsBaseDir baseDir = new ClsBaseDir();
        assertEquals(expected, baseDir.parseAction(input));
    }
}
