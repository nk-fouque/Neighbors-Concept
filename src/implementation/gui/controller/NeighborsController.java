package implementation.gui.controller;

import implementation.gui.model.NeighborButton;
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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import implementation.Cluster;
import implementation.Partition;
import implementation.gui.NeighborsInterface;

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
    ChoiceBox<String> format;
    @FXML
    Button fileFindButton;
    @FXML
    BorderPane partitionResults;
    @FXML
    TextField filterSubjectsField;
    @FXML
    Button filterSubjectsButton;
    @FXML
    Accordion partitionAccordion;
    @FXML
    BorderPane partitionCandidates;
    @FXML
    Accordion candidatesAccordion;

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
        pane.setPrefSize(1920,1080);
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
                    md.write(System.out, format.getValue());

                    subjectsList = new ArrayList<>();
                    ResIterator iter = md.listSubjects();
                    while (iter.hasNext()) {
                        subjectsList.add(iter.nextResource().getURI());
                    }
                    candidatesAccordion.getPanes().clear();
                    PriorityQueue<String> queue = new PriorityQueue<>(subjectsList);
                    while (!queue.isEmpty()) {
                        candidatesAccordion.getPanes().add(NeighborsController.this.candidateVisual(queue.poll()));
                    }
                    modelLoaded.setValue(true);
                } catch (FileNotFoundException e) {
                    TitledPane err = new TitledPane();
                    err.setText("File not found");
                    err.setContent(new Text(e.getMessage()));
                    candidatesAccordion.getPanes().add(err);
                    e.printStackTrace();
                }
            }
        });

        partitionResults.visibleProperty().bind(modelLoaded);

        filterSubjectsField.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent keyEvent) {
                if (keyEvent.getCode().equals(KeyCode.ENTER)){
                    filter(filterSubjectsField.getText(),candidatesAccordion);
                }
            }
        });
        filterSubjectsButton.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                filter(filterSubjectsField.getText(),candidatesAccordion);
            }
        });

        partitionCandidates.autosize();

    }

    private void filter(String filter, Accordion filtered){
        filtered.getPanes().clear();
        List<String> filteredList = new ArrayList<>();
        for (String s : subjectsList){
            if (s.contains(filter)){
                filteredList.add(s);
            }
        }
        for (String s : filteredList){
            filtered.getPanes().add(candidateVisual(s));
        }
    }

    public static TitledPane clusterVisual(Cluster c){
        TitledPane res =new TitledPane();
        res.setText("Extentional Distance : "+c.getRelaxDistance());
        BorderPane pane = new BorderPane();
        res.setContent(pane);
        pane.setTop(new Text("Similitude : \n"+c.getRelaxQueryElements().toString().replace(",","\n")));
        pane.setLeft(new Text("\nNeighbors : \n"+c.getAnswersList().toString().replace(',','\n')));
        res.autosize();
        return res;
    }

    public TitledPane candidateVisual(String uri){
        TitledPane res =new TitledPane();
        res.setText(uri);
        BorderPane pane = new BorderPane();
        res.setContent(pane);
        pane.setCenter(new NeighborButton(uri,partitionAccordion,md,partition));
        return res;
    }

}
