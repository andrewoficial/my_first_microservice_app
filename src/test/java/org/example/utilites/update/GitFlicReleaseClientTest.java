package org.example.utilites.update;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GitFlicReleaseClientTest {

    @Test
    public void isGitFlicUrl() {
        assertTrue(GitFlicReleaseClient.isGitFlicUrl(
                "https://gitflic.ru/project/andrewkantser/elephant-monitor/release?sort=TIME&direction=DESC"));
        assertTrue(GitFlicReleaseClient.isGitFlicUrl(
                "https://api.gitflic.ru/project/andrewkantser/elephant-monitor/release"));
        assertFalse(GitFlicReleaseClient.isGitFlicUrl(
                "https://api.github.com/repos/andrewoficial/my_first_microservice_app/releases"));
    }

    @Test
    public void toListUrlConvertsWebToApi() {
        String in = "https://gitflic.ru/project/andrewkantser/elephant-monitor/release?sort=TIME&direction=DESC";
        String expected = "https://api.gitflic.ru/project/andrewkantser/elephant-monitor/release";
        assertEquals(expected, GitFlicReleaseClient.toListUrl(in));
    }

    @Test
    public void parseApiJsonList() {
        String json = "{\n" +
                "  \"_embedded\": {\n" +
                "    \"releaseTagModelList\": [\n" +
                "      {\n" +
                "        \"tagName\": \"v1.8.49\",\n" +
                "        \"description\": \"notes here\",\n" +
                "        \"attachmentFiles\": [\n" +
                "          {\n" +
                "            \"name\": \"Elephant-Monitor-1.8.49.jar\",\n" +
                "            \"link\": \"https://gitflic.ru/project/u/p/release/r/file/f\"\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}";
        List<Release> list = GitFlicReleaseClient.parseApiJson(json);
        assertEquals(1, list.size());
        assertEquals("1.8.49", list.get(0).version);
        assertEquals("notes here", list.get(0).notes);
        assertTrue(list.get(0).hasDownload());
        assertEquals("Elephant-Monitor-1.8.49.jar", list.get(0).resolveFileName());
    }

    @Test
    public void releaseResolveFileNameForDownloadSegment() {
        Release r = new Release("1.2.3", "",
                "https://gitflic.ru/project/a/b/release/uuid1/uuid2/download");
        assertEquals("Elephant-Monitor-1.2.3.jar", r.resolveFileName());
    }
}
