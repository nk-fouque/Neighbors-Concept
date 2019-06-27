package implementation.gui.controller;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.reasoner.ReasonerRegistry;
import org.apache.jena.sparql.core.Var;
import implementation.Cluster;
import implementation.Partition;
import implementation.gui.NeighborsInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.*;

public class NeighborsController implements Initializable {

    @FXML
    GridPane pane;
    @FXML
    TextField filenameField;
    @FXML
    Button modelLoadButton;
    @FXML
    Text modelText;
    @FXML
    ChoiceBox<String> format;
    @FXML
    Button fileFindButton;
    @FXML
    ChoiceBox<String> subjects;
    @FXML
    Button partitionButton;
    @FXML
    Text partitionText;
    @FXML
    BorderPane partitionResults;
    @FXML
    TextField filterSubjectsField;
    @FXML
    Button filterSubjectsButton;
    @FXML
    Accordion partitionAccordion;

    private Model md;

    private Partition partition;

    private SimpleBooleanProperty modelLoaded=new SimpleBooleanProperty(false);

    private List<String> subjectsList;

    private List<String> formats(){
        List<String> res = new ArrayList<>();
        res.add("TURTLE");
        res.add("NTRIPLES");
        res.add("N3");
        res.add("RDFXML");
        res.add("RDFJSON");
        return res;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pane.prefWidthProperty().setValue(1920);
        pane.prefHeightProperty().setValue(1080);
        modelLoadButton.setText("Load RDF File");
        md = ModelFactory.createDefaultModel();

        format.setItems(new SortedList<String>(FXCollections.observableList(formats())));
        format.setValue("TURTLE");

        final FileChooser fileChooser= new FileChooser();
        fileFindButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                File file = fileChooser.showOpenDialog(NeighborsInterface.stage);
                if (file != null) {
                    filenameField.textProperty().setValue(file.getAbsolutePath());
                }
            }
        });

        modelLoadButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                String filename = filenameField.getText();
                try {
                    md.read(new FileInputStream(filename), null, format.getValue());
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    md.write(System.out,format.getValue());
                    modelText.setText(baos.toString());

                    subjectsList = new ArrayList<>();
                    ResIterator iter = md.listSubjects();
                    while (iter.hasNext()){
                        subjectsList.add(iter.nextResource().getURI());
                    }
                    subjects.setItems(FXCollections.observableList(subjectsList));
                    subjects.autosize();
                    modelLoaded.setValue(true);
                } catch (FileNotFoundException e) {
                    modelText.setText("File not found");
                    e.printStackTrace();
                }
            }
        });

        partitionResults.visibleProperty().bind(modelLoaded);
        partitionButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                partitionText.setText("Loading...");
                Model saturated = ModelFactory.createInfModel(ReasonerRegistry.getRDFSReasoner(), md);

                String uriTarget = md.expandPrefix(subjects.getValue());
                Map<String, Var> keys = new HashMap<>();
                String QueryString = Partition.initialQueryString(uriTarget, md, keys);

                // Printing the result just to show that we find it back
                Query q = QueryFactory.create(QueryString);
                QueryExecution qe = QueryExecutionFactory.create(q, saturated);
                ResultSetFormatter.out(System.out, qe.execSelect(), q);

                // Creation of the Partition
                partition = new Partition(q, md, saturated, keys);
                partition.partitionAlgorithm();
                for(Cluster c : partition.getNeighbors()){
                    partitionAccordion.getPanes().add(clusterVisual(c));
                }
                partitionAccordion.autosize();
            }
        });

        filterSubjectsField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode().equals(KeyCode.ENTER)){
                    filter(filterSubjectsField.getText(),subjects);
                }
            }
        });
        filterSubjectsButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                filter(filterSubjectsField.getText(),subjects);
            }
        });

    }

    private void filter(String filter, ChoiceBox<String> filtered){
        List<String> res = new ArrayList<>();
        for (String s : subjectsList){
            if (s.contains(filter)){
                res.add(s);
            }
        }
        filtered.setItems(FXCollections.observableList(res));
    }

    public static TitledPane clusterVisual(Cluster c){
        TitledPane res =new TitledPane();
        res.setText("Extentional Distance : "+c.getRelaxDistance());
        BorderPane pane = new BorderPane();
        res.setContent(pane);
        pane.setTop(new Text("Similitude : \n"+c.getRelaxQueryElements().toString().replace(",","\n")));
        pane.setLeft(new Text("\nNeighbors : \n"+c.getAnswersList().toString()));
        res.autosize();
        return res;
    }

}
