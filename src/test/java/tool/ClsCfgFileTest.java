package tool;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tool.cmnclslib.cls.ClsLogger;
import tool.cmnclslib.mdl.MdlFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ClsCfgFile} の単体テストクラスです。
 */
public class ClsCfgFileTest {

    private ClsLogger logger;
    private String tmpDir;

    @BeforeEach
    public void setUp() {
        logger = new ClsLogger();
        logger.setValueByKey(ClsLogger.IS_CONSOLE, "false");
        tmpDir = new File(new File(new File(System.getProperty("java.io.tmpdir"), "UnitTest"), "FileCleaner"), "ClsCfgFile").getPath();
        File dir = new File(tmpDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @AfterEach
    public void tearDown() {
        File dir = new File(tmpDir);
        if (dir.exists()) {
            MdlFile.deleteRecursively(tmpDir);
        }
    }

    @Test
    public void constructor_Initialization_Success() {
        ClsCfgFile cfgFile = new ClsCfgFile(logger);
        assertNotNull(cfgFile.getTargetList());
        assertNotNull(cfgFile.getListStr());
        assertEquals(0, cfgFile.getVerbose());
        assertEquals(3600, cfgFile.getTimeout());
        assertEquals(";|", cfgFile.getDelimiter());
    }

    @Test
    public void getRegexGroupValue_ValidInput_ReturnsGroupValue() {
        String value = ClsCfgFile.getRegexGroupValue("W30", "^W(?<VAL>[0-9]+)$", "VAL");
        assertEquals("30", value);
    }

    @Test
    public void getRegexGroupValue_InvalidInput_ReturnsEmptyString() {
        String value = ClsCfgFile.getRegexGroupValue("INVALID", "^W(?<VAL>[0-9]+)$", "VAL");
        assertEquals("", value);
    }

    @Test
    public void parseCsvToList_WildcardAndNormal_AddsToTargetList() {
        ClsCfgFile cfgFile = new ClsCfgFile(logger);
        boolean result = cfgFile.parseCsvToList("*.log,test.txt");

        assertTrue(result);
        assertTrue(cfgFile.getListStr().contains(".*.log"));
        assertTrue(cfgFile.getListStr().contains("test.txt"));
    }

    @Test
    public void addTarget_ValidLine_AddsBaseDirToTargetList() {
        ClsCfgFile cfgFile = new ClsCfgFile(logger);
        // 01:LineNo; 02:IsExec; 03:Type; 04:Term/Gen; 05:Path; 06:Min; 07:Max
        String validLine = "100;1;ALL;7;" + tmpDir + ";0;10";

        boolean result = cfgFile.addTarget(validLine);

        assertTrue(result);
        assertEquals(1, cfgFile.getTargetList().size());
        ClsBaseDir target = cfgFile.getTargetList().get(0);
        assertEquals(100, target.getLineNo());
        assertTrue(target.isExec());
        assertTrue(target.isRmFile());
        assertTrue(target.isRmDir());
    }

    @Test
    public void addTarget_InvalidLineNo_ReturnsFalse() {
        ClsCfgFile cfgFile = new ClsCfgFile(logger);
        String invalidLine = "0;1;ALL;7;C:\\tmp;0;10";

        boolean result = cfgFile.addTarget(invalidLine);

        assertFalse(result);
        assertTrue(cfgFile.getTargetList().isEmpty());
    }

    @Test
    public void readConfig_ValidConfigFile_ReadsAndAddsTargets() throws Exception {
        ClsCfgFile cfgFile = new ClsCfgFile(logger);
        String filePath = new File(tmpDir, "test_config.cfg").getPath();
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filePath), StandardCharsets.UTF_8)) {
            writer.write("# Comment Line\n");
            writer.write("ALL ; 100;1;F;30;" + tmpDir + ";0;5\n");
            writer.write("LOCALHOST ; 200;1;D;15;" + tmpDir + ";1;2\n");
        }

        boolean result = cfgFile.readConfig(filePath, true);

        assertTrue(result);
        assertEquals(2, cfgFile.getTargetList().size());
    }
}
