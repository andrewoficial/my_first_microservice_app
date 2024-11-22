import org.example.utilites.MyUtilities;
import org.example.utilites.ProgramUpdater;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionChecker {
    @Test
    public void versionParser() {
        ProgramUpdater checker = new ProgramUpdater();

        assertTrue( ! checker.isAvailableNewVersion("1.2.3", "1.2.4"));
        assertTrue( ! checker.isAvailableNewVersion("1.2.3", "1.4.3"));
        assertTrue( ! checker.isAvailableNewVersion("1.2.3-Beta", "1.2.3"));
        assertTrue( ! checker.isAvailableNewVersion("1.2.3", "1.2.3"));
        assertTrue( ! checker.isAvailableNewVersion("4.2.3", "4.3.1"));

        assertTrue( checker.isAvailableNewVersion("4.2.3", "4.2.2"));

        assertTrue( checker.isAvailableNewVersion("4.2.3", "3.3.4"));
        assertTrue( checker.isAvailableNewVersion("4.2.3", "4.2.3-Beta"));

        assertTrue( ! checker.isAvailableNewVersion(null, "4.2.3-Beta"));
        assertTrue( ! checker.isAvailableNewVersion("kjhkjh897", "4.2.3-Beta"));
        assertTrue( ! checker.isAvailableNewVersion("4.2.3", null));
        assertTrue( ! checker.isAvailableNewVersion("4.2.3", "234234iuoiu"));
        assertTrue( ! checker.isAvailableNewVersion(null, null));


    }
}
