package tool;

import java.io.File;
import org.junit.jupiter.api.Test;
import tool.cmnclslib.cls.ClsLogger;
import tool.cmnclslib.mdl.MdlFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ClsAppArg} の単体テストクラスです。
 */
public class ClsAppArgTest {

    private ClsLogger createLogger() {
        ClsLogger logger = new ClsLogger();
        logger.setValueByKey(ClsLogger.IS_CONSOLE, "false");
        return logger;
    }

    @Test
    public void constructor_NullLogger_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new ClsAppArg(null));
    }

    @Test
    public void constructor_ValidLogger_InitializesProperties() {
        ClsLogger logger = createLogger();
        ClsAppArg sut = new ClsAppArg(logger);

        assertNotNull(sut);
        assertEquals(0, sut.getUsageFlag());
        assertTrue(sut.isList());
        assertTrue(sut.isDiff());
        assertTrue(sut.isAscending());
    }

    @Test
    public void parse_NullArgs_ThrowsNullPointerException() {
        ClsLogger logger = createLogger();
        ClsAppArg sut = new ClsAppArg(logger);

        assertThrows(NullPointerException.class, () -> sut.parse(null));
    }

    @Test
    public void parse_BasicPathAndTerm_ParsesSuccessfully() {
        ClsLogger logger = createLogger();
        ClsAppArg sut = new ClsAppArg(logger);
        String tempDir = new File(new File(System.getProperty("java.io.tmpdir")), "tmp_test_dir").getPath();
        new File(tempDir).mkdirs();

        try {
            String[] args = new String[]{"-path", tempDir, "-term", "14", "-clean"};
            boolean result = sut.parse(args);

            assertTrue(result);
            assertFalse(sut.isList()); // -clean が指定されたため false
            assertEquals(1, sut.getTargetList().size());
            assertEquals(14.0, sut.getTargetList().get(0).getTerm());
        } finally {
            File dir = new File(tempDir);
            if (dir.exists()) {
                MdlFile.deleteRecursively(tempDir);
            }
        }
    }

    @Test
    public void parse_HelpOption_SetsUsageFlag() {
        ClsLogger logger = createLogger();
        ClsAppArg sut = new ClsAppArg(logger);
        String[] args = new String[]{"-help-conf"};

        boolean result = sut.parse(args);

        assertTrue(result);
        assertEquals(ClsAppArg.USAGE_SHOW_SAMPLE_CONFIG, sut.getUsageFlag());
    }

    @Test
    public void printDefinition_NullBaseDir_ThrowsNullPointerException() {
        ClsLogger logger = createLogger();
        ClsAppArg sut = new ClsAppArg(logger);

        assertThrows(NullPointerException.class, () -> sut.printDefinition(null));
    }

    @Test
    public void printDefinition_ValidBaseDir_ExecutesWithoutException() {
        ClsLogger logger = createLogger();
        ClsAppArg sut = new ClsAppArg(logger);
        ClsBaseDir baseDir = new ClsBaseDir();
        baseDir.setPath("C:\\TestPath");
        baseDir.setTerm(7);

        assertDoesNotThrow(() -> sut.printDefinition(baseDir));
    }

    @Test
    public void usage_ExecutesWithoutException() {
        ClsLogger logger = createLogger();
        ClsAppArg sut = new ClsAppArg(logger);

        assertDoesNotThrow(sut::usage);
    }

    @Test
    public void showSampleConfig_ExecutesWithoutException() {
        ClsLogger logger = createLogger();
        ClsAppArg sut = new ClsAppArg(logger);

        assertDoesNotThrow(sut::showSampleConfig);
    }
}
