import org.example.utilites.MyUtilities;
import org.example.utilites.ProgramUpdater;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionChecker {
    @Test
    public void versionParser() {
        ProgramUpdater checker = new ProgramUpdater();

        assertFalse(checker.isAvailableNewVersion("1.2.3", "1.2.4"));
        assertFalse(checker.isAvailableNewVersion("1.2.3", "1.4.3"));
        assertFalse(checker.isAvailableNewVersion("1.2.3-Beta", "1.2.3"));
        assertFalse(checker.isAvailableNewVersion("1.2.3", "1.2.3"));
        assertFalse(checker.isAvailableNewVersion("4.2.3", "4.3.1"));

        assertTrue( checker.isAvailableNewVersion("4.2.3", "4.2.2"));

        assertTrue( checker.isAvailableNewVersion("4.2.3", "3.3.4"));
        assertTrue( checker.isAvailableNewVersion("4.2.3", "4.2.3-Beta"));

        assertFalse(checker.isAvailableNewVersion(null, "4.2.3-Beta"));
        assertFalse(checker.isAvailableNewVersion("kjhkjh897", "4.2.3-Beta"));
        assertFalse(checker.isAvailableNewVersion("4.2.3", null));
        assertFalse(checker.isAvailableNewVersion("4.2.3", "234234iuoiu"));
        assertFalse(checker.isAvailableNewVersion(null, null));


    }
}
