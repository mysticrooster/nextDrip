package com.eveningoutpost.dexdrip.utils;

import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import lombok.val;

/**
 * Pins the key, stored type and default of every value preference declared in the settings XMLs.
 *
 * <p>The settings layout is reorganised into category files, but the contract is that no preference
 * key, type or default may change. This test parses the XML directly and compares the result with
 * the {@code settings_structure_fixture.tsv} baseline captured from the pre-reorganisation tree.
 *
 * <p>It is a plain JUnit test: it reads the XML from the module source tree and never inflates a
 * preference, so it needs no Robolectric, Android context or resource compilation.
 */
public class SettingsStructureTest {

    private static final String FIXTURE_RESOURCE = "/settings_structure_fixture.tsv";
    private static final String ANDROID_NS = "http://schemas.android.com/apk/res/android";

    /** The exact set of key/type/default tuples matches the captured baseline. */
    @Test
    public void preferenceStructureMatchesBaseline() throws Exception {
        // :: Setup
        val expected = loadFixture();

        // :: Act
        val actual = collectStructure();

        // :: Verify
        assertWithMessage("the set of keys with a stored default is unchanged")
                .that(actual.keySet()).containsExactlyElementsIn(expected.keySet());
        for (val entry : expected.entrySet()) {
            val key = entry.getKey();
            assertWithMessage("type of " + key).that(actual.get(key)[0]).isEqualTo(entry.getValue()[0]);
            assertWithMessage("default of " + key).that(actual.get(key)[1]).isEqualTo(entry.getValue()[1]);
        }
    }

    private static Map<String, String[]> loadFixture() throws IOException {
        val map = new TreeMap<String, String[]>();
        try (InputStream is = SettingsStructureTest.class.getResourceAsStream(FIXTURE_RESOURCE)) {
            assertWithMessage("the baseline fixture " + FIXTURE_RESOURCE + " is on the test classpath")
                    .that(is).isNotNull();
            try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                readRows(reader, map);
            }
        }
        assertWithMessage("the baseline fixture is not empty").that(map).isNotEmpty();
        return map;
    }

    private static void readRows(Reader reader, Map<String, String[]> into) throws IOException {
        val buffered = new java.io.BufferedReader(reader);
        String line;
        while ((line = buffered.readLine()) != null) {
            if (line.isEmpty()) continue;
            val parts = line.split("\t", -1);
            assertWithMessage("fixture row has three columns: " + line).that(parts.length).isEqualTo(3);
            into.put(parts[0], new String[]{parts[1], parts[2]});
        }
    }

    private static Map<String, String[]> collectStructure() throws Exception {
        val map = new TreeMap<String, String[]>();
        for (val file : locateSettingsXmls()) {
            parseFile(file, map);
        }
        return map;
    }

    private static void parseFile(File file, Map<String, String[]> into)
            throws XmlPullParserException, IOException {
        val factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        val parser = factory.newPullParser();
        try (Reader reader = new FileReader(file)) {
            parser.setInput(reader);
            int event = parser.getEventType();
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    val defaultValue = parser.getAttributeValue(ANDROID_NS, "defaultValue");
                    if (defaultValue != null) {
                        val key = parser.getAttributeValue(ANDROID_NS, "key");
                        assertWithMessage("every preference with a defaultValue has a key in " + file.getName())
                                .that(key).isNotNull();
                        into.put(key, new String[]{storedType(parser.getName()), defaultValue});
                    }
                }
                event = parser.next();
            }
        }
    }

    /** Maps the XML element to the type the legacy preference framework stores for it. */
    private static String storedType(String elementName) {
        switch (elementName) {
            case "CheckBoxPreference":
            case "SwitchPreference":
                return "Boolean";
            case "SeekBarPreference":
                return "Integer";
            default:
                return "String";
        }
    }

    /** The settings XMLs, in a stable order, from the module source tree or the repository root. */
    private static List<File> locateSettingsXmls() {
        val dir = locateXmlDir();
        val files = new ArrayList<File>();
        val children = dir.listFiles();
        assertWithMessage("the settings xml directory exists: " + dir.getAbsolutePath())
                .that(children).isNotNull();
        for (val child : children) {
            val name = child.getName();
            if (!name.endsWith(".xml")) continue;
            if (name.equals("pref_headers.xml")) continue;
            if (name.startsWith("pref_") || name.equals("xdrip_plus_prefs.xml") || name.equals("xdrip_plus_defaults.xml")) {
                files.add(child);
            }
        }
        Collections.sort(files);
        assertWithMessage("the settings xmls were found; working directory is " + new File(".").getAbsolutePath())
                .that(files).isNotEmpty();
        return files;
    }

    private static File locateXmlDir() {
        val fromModule = new File("src/main/res/xml");
        if (fromModule.isDirectory()) return fromModule;
        return new File("app/src/main/res/xml");
    }
}
