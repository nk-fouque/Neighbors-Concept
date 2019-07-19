package implementation.gui.controller;

import implementation.algorithms.Partition;
import implementation.gui.NeighborsInterface;
import implementation.gui.model.NeighborButton;
import implementation.gui.model.VisualCandidate;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.io.File;
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
    Button fileFindButton;
    @FXML
    TextField filenameField;
    @FXML
    ChoiceBox<String> format;
    @FXML
    Button modelLoadButton;

    @FXML
    TextField filterSubjectsField;
    @FXML
    Button filterSubjectsButton;
    @FXML
    CheckBox caseSensBox;

    @FXML
    BorderPane partitionCandidates;
    @FXML
    ScrollPane scrollPane;
    @FXML
    FlowPane candidates;

    @FXML
    BorderPane partitionResults;
    @FXML
    Accordion partitionAccordion;

    @FXML
    Spinner<Integer> timeLimit;
    @FXML
    Button cutButton;
    @FXML
    Label cutLabel;

    private Model md;

    private Partition partition;

    private BooleanProperty modelLoaded = new SimpleBooleanProperty(false);

    private List<String> subjectsList;

    private BooleanProperty partitionAvailable = new SimpleBooleanProperty(true);

    private AtomicBoolean anytimeCut = new AtomicBoolean(false);

    /**
     * @return List of Jena supported RDF formats
     */
    private static List<String> formats() {
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

        format.setItems(new SortedList<>(FXCollections.observableList(formats())));
        format.setValue("TURTLE");

        final FileChooser fileChooser = new FileChooser();
        fileFindButton.setOnMouseClicked(mouseEvent -> {
            File file = fileChooser.showOpenDialog(NeighborsInterface.stage);
            if (file != null) {
                filenameField.textProperty().setValue(file.getAbsolutePath());
            }
        });

        subjectsList = new ArrayList<>();
        modelLoadButton.setOnMouseClicked(mouseEvent -> {
            String filename = filenameField.getText();
            ModelLoad loader = new ModelLoad(filename, format.getValue(), md, this, subjectsList, modelLoaded);
            Thread load = new Thread(loader);
            Label modelState = new Label();
            modelState.textProperty().bindBidirectional(loader.stateProperty());
            partitionCandidates.setTop(new ToolBar(modelState));
            candidates.getChildren().clear();
            load.start();
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
                cutActivate();
            } else {
                cutDeactivate();
            }
        });

        NeighborsInterface.exit.addListener(changeListener -> {
            if (NeighborsInterface.exit.get()) anytimeCut.set(true);
        });
    }

    /**
     * Only shows subjects with a certain text in them
     *
     * @param filter the string that must be present in the results
     */
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

    /**
     * @param uri The uri of the Node we want to represnet in the CandidateVisual
     * @return A Border pane with the uri on the left and a button on the right
     */
    public BorderPane candidateVisual(String uri) {
        NeighborButton button = new NeighborButton(uri, partitionAccordion, md, partitionAvailable, anytimeCut, this, timeLimit.getValue());
        VisualCandidate res = new VisualCandidate(uri, md, button);
        return res;
    }

    /**
     * Changes everything that needs to be changed when the algorithm stopper is activated
     */
    public void cutActivate() {
        anytimeCut.set(true);

        cutButton.setStyle("-fx-background-color: red");
        cutLabel.setVisible(true);
    }

    /**
     * Changes everything that needs to be changed when the algorithm stopper is deactivated
     */
    public void cutDeactivate() {
        anytimeCut.set(false);

        cutButton.setStyle("");
        cutLabel.setVisible(false);
    }

}
