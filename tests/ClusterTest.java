import implementation.Cluster;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.vocabulary.RDFS;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


class ClusterTest {
    private Query qry;
    private Model md;
    private Cluster c;

    @BeforeEach
    void setUp() {
        BasicConfigurator.configure();
        Logger.getLogger("org.apache.jena").setLevel(Level.INFO);
        md = ModelFactory.createDefaultModel();
        md.createResource("http://rien")
                .addProperty(RDFS.label,"Test");
        md.createResource("http://presque.rien")
                .addProperty(RDFS.label,"Test2");
        System.out.println(md.size());
        qry = QueryFactory.create("SELECT ?x ?lab WHERE " +
                "{?x <"+RDFS.label.toString()+"> ?lab.}");

        c = new Cluster(qry,md);
    }

    @Test
    void testConstr(){
        assertNotNull(c);
        assertEquals(2,c.getProj().size());
        assertTrue(c.getRelaxQueryElements().isEmpty());
        assertEquals(1,c.getAvailableQueryElements().size());
        assertEquals(md,c.getMapping());
        assertEquals(md,c.getAnswers());
    }


}