package implementation.gui.controller;

import implementation.Partition;
import implementation.gui.NeighborsInterface;
import implementation.gui.model.NeighborButton;
import implementation.gui.model.VisualCandidate;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

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
    ScrollPane scrollPane;
    @FXML
    FlowPane candidates;
    @FXML
    Button cutButton;
    @FXML
    Label cutLabel;
    @FXML
    CheckBox caseSensBox;

    private Model md;

    private Partition partition;

    private SimpleBooleanProperty modelLoaded = new SimpleBooleanProperty(false);

    private List<String> subjectsList;

    private SimpleBooleanProperty partitionAvailable = new SimpleBooleanProperty(true);

    private AtomicBoolean anytimeCut = new AtomicBoolean(false);

    private List<String> formats() {
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
        pane.setPrefSize(1920, 1080);

        md = ModelFactory.createDefaultModel();

        format.setItems(new SortedList<String>(FXCollections.observableList(formats())));
        format.setValue("TURTLE");

        final FileChooser fileChooser = new FileChooser();
        fileFindButton.setOnMouseClicked(mouseEvent -> {
            File file = fileChooser.showOpenDialog(NeighborsInterface.stage);
            if (file != null) {
                filenameField.textProperty().setValue(file.getAbsolutePath());
            }
        });

        modelLoadButton.setOnMouseClicked(mouseEvent -> {
            String filename = filenameField.getText();
            try {
                candidates.getChildren().clear();
                md.removeAll();
                md.read(new FileInputStream(filename), null, format.getValue());
                md.write(System.out, format.getValue());

                subjectsList = new ArrayList<>();
                ResIterator iter = md.listSubjects();
                while (iter.hasNext()) {
                    subjectsList.add(iter.nextResource().getURI());
                }
                PriorityQueue<String> queue = new PriorityQueue<>(subjectsList);
                while (!queue.isEmpty()) {
                    BorderPane visual = NeighborsController.this.candidateVisual(queue.poll());
                    visual.minWidthProperty().bind((scrollPane.widthProperty()));
                    candidates.getChildren().add(visual);
                }
                modelLoaded.setValue(true);
            } catch (FileNotFoundException e) {
                TitledPane err = new TitledPane();
                err.setText("File not found");
                err.setContent(new Text(e.getMessage()));
                candidates.getChildren().add(err);
                e.printStackTrace();
            }
        });

        partitionResults.visibleProperty().bind(modelLoaded);

        filterSubjectsField.setOnKeyPressed(keyEvent -> {
            if (keyEvent.getCode().equals(KeyCode.ENTER)) {
                filter(filterSubjectsField.getText());
            }
        });
        filterSubjectsButton.setOnMouseClicked(mouseEvent -> filter(filterSubjectsField.getText()));

        partitionCandidates.autosize();

        cutLabel.setVisible(false);
        cutLabel.setText("/!\\ Algorithm will stop early, please deactivate before running new partition /!\\");
        cutButton.setOnMouseClicked(mouseEvent -> {
            if (!anytimeCut.get()) {
                cutButton.setStyle("-fx-background-color: red");
                anytimeCut.set(true);
                cutLabel.setVisible(true);
            } else {
                cutButton.setStyle("");
                anytimeCut.set(false);
                cutLabel.setVisible(false);
            }
        });

        NeighborsInterface.exit.addListener(changeListener -> {
            if (NeighborsInterface.exit.get()) anytimeCut.set(true);
        });
    }

    private void filter(String filter) {
        candidates.getChildren().clear();
        List<String> filteredList = new ArrayList<>();
        if (!caseSensBox.isSelected()) filter = filter.toLowerCase();
        for (String s : subjectsList) {
            String s2 = "";
            if (!caseSensBox.isSelected()) s2 = s.toLowerCase();
            if (s2.contains(filter)) {
                filteredList.add(s);
            }
        }
        PriorityQueue<String> queue = new PriorityQueue<>(filteredList);
        while (!queue.isEmpty()) {
            BorderPane visual = NeighborsController.this.candidateVisual(queue.poll());
            visual.minWidthProperty().bind((scrollPane.widthProperty()));
            candidates.getChildren().add(visual);
        }
    }

    public BorderPane candidateVisual(String uri) {
        NeighborButton button = new NeighborButton(uri, partitionAccordion, md, partition, partitionAvailable, anytimeCut);
        VisualCandidate res = new VisualCandidate(uri, md, button);
        return res;
    }

}
