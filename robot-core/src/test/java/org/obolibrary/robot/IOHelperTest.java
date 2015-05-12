package org.obolibrary.robot;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import com.github.jsonldjava.core.Context;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.PrefixManager;

/**
 * Tests for {@link IOHelper}.
 */
public class IOHelperTest extends CoreTest {
    /**
     * Test getting the default context.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testContext() throws IOException {
        IOHelper ioh = new IOHelper();
        Context context = ioh.getContext();

        assertEquals("Check GO prefix",
                "http://purl.obolibrary.org/obo/GO_",
                context.getPrefixes(false).get("GO"));
    }

    /**
     * Test fancier JSON-LD contexts.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testContextHandling() throws IOException {
        String json = "{\n"
                    + "  \"@context\" : {\n"
                    + "    \"foo\" : \"http://example.com#\",\n"
                    + "    \"bar\" : {\n"
                    + "      \"@id\": \"http://example.com#\",\n"
                    + "      \"@type\": \"@id\"\n"
                    + "    }\n"
                    + "  }\n"
                    + "}";

        IOHelper ioh = new IOHelper();
        Context context = ioh.parseContext(json);
        ioh.setContext(context);

        Map<String, String> expected = new HashMap<String, String>();
        expected.put("foo", "http://example.com#");
        expected.put("bar", "http://example.com#");
        assertEquals("Check JSON prefixes", expected, ioh.getPrefixes());
    }

    /**
     * Test prefix maps.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testPrefixHandling() throws IOException {
        IOHelper ioh = new IOHelper(false);
        Map<String, String> expected = new HashMap<String, String>();
        assertEquals("Check no prefixes", expected, ioh.getPrefixes());

        ioh.addPrefix("foo", "http://example.com#");
        expected.put("foo", "http://example.com#");
        assertEquals("Check foo prefix", expected, ioh.getPrefixes());

        String json = "{\n"
                    + "  \"@context\" : {\n"
                    + "    \"foo\" : \"http://example.com#\"\n"
                    + "  }\n"
                    + "}";
        assertEquals("Check JSON-LD", json, ioh.getContextString());

        ioh.addPrefix("bar: http://example.com#");
        expected.put("bar", "http://example.com#");
        assertEquals("Check no prefixes", expected, ioh.getPrefixes());
    }

    /**
     * Test the default prefix manager.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testPrefixManager() throws IOException {
        IOHelper ioh = new IOHelper();
        PrefixManager pm = ioh.getPrefixManager();

        assertEquals("Check GO CURIE",
                "http://purl.obolibrary.org/obo/GO_12345",
                pm.getIRI("GO:12345").toString());
    }

    /**
     * Test getting terms from strings.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testReadTerms() throws IOException {
        IOHelper ioh = new IOHelper();
        ioh.addPrefix("foo", "http://example.com#");
        ioh.addPrefix("definition", "http://example.com#definition");

        String input = "http://purl.obolibrary.org/obo/GO_1\n"
                     + "obo:GO_2\n"
                     + "    \n" // blank line
                     + "# line comment\n"
                     + "GO:3 # trailing comment\n"
                     + "foo:4\n"
                     + "definition\n";

        Set<String> terms = new HashSet<String>();
        terms.add("http://purl.obolibrary.org/obo/GO_1");
        terms.add("obo:GO_2");
        terms.add("GO:3");
        terms.add("foo:4");
        terms.add("definition");

        Set<String> actualTerms = ioh.extractTerms(input);
        assertEquals("Check terms", terms, actualTerms);

        Set<IRI> iris = new HashSet<IRI>();
        iris.add(IRI.create("http://purl.obolibrary.org/obo/GO_1"));
        iris.add(IRI.create("http://purl.obolibrary.org/obo/GO_2"));
        iris.add(IRI.create("http://purl.obolibrary.org/obo/GO_3"));
        iris.add(IRI.create("http://example.com#4"));
        iris.add(IRI.create("http://example.com#definition"));

        Set<IRI> actualIRIs = ioh.createIRIs(terms);
        assertEquals("Check converted IRIs", iris, actualIRIs);
    }

    /**
     * Test creating IRIs and Literals.
     *
     * @throws IOException on file problem
     */
    @Test
    public void testOntologyAnnotations() throws IOException {
        IOHelper ioh = new IOHelper();
        OWLLiteral literal;

        assertEquals(IRI.create(base), ioh.createIRI(base));

        literal = ioh.createLiteral("FOO");
        assertEquals("FOO", OntologyHelper.getValue(literal));

        literal = ioh.createTypedLiteral("100", "xsd:integer");
        assertEquals("100", literal.getLiteral());
        assertEquals(ioh.createIRI("xsd:integer"),
                literal.getDatatype().getIRI());

        literal = ioh.createTaggedLiteral("100", "en");
        assertEquals("100", literal.getLiteral());
        assertEquals("en", literal.getLang());
    }

}
