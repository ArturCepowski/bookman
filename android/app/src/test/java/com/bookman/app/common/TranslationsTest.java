package com.bookman.app.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Set;
import java.util.TreeSet;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** REQ-14: every user-visible text exists in English (default) and Polish. */
public class TranslationsTest {

    @Test
    public void REQ14_everyEnglishStringHasAPolishTranslation() throws Exception {
        Set<String> english = names("src/main/res/values/strings.xml");
        Set<String> polish = names("src/main/res/values-pl/strings.xml");

        assertTrue(english.size() > 50);
        assertEquals(english, polish);
    }

    @Test
    public void REQ14_languageCodesMapToLocales() {
        assertEquals("pl", AppLanguage.tagFor("PL"));
        assertEquals("en", AppLanguage.tagFor("EN"));
        assertEquals("en", AppLanguage.tagFor(null));
    }

    private static Set<String> names(String path) throws Exception {
        NodeList nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(path))
                .getDocumentElement().getChildNodes();
        Set<String> names = new TreeSet<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element element && !"false".equals(element.getAttribute("translatable"))) {
                names.add(element.getTagName() + ":" + element.getAttribute("name"));
            }
        }
        return names;
    }
}
