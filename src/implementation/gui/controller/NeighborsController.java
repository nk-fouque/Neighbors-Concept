package implementation.gui.controller;

import implementation.algorithms.Partition;
import implementation.gui.NeighborsInterface;
import implementation.gui.model.VisualCandidate;
import implementation.gui.model.VisualPrefixes;
import implementation.utils.CollectionsModel;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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
    CheckBox safeModeBox;
    @FXML
    Spinner<Integer> safeModeLimit;

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
    @FXML
    Label finalState;
    @FXML
    Spinner<Integer> depthLimit;

    @FXML
    TextField selectedNodeField;
    @FXML
    Button partitionButton;

    private Model md;

    public CollectionsModel colMd;

    private Partition partition;

    private BooleanProperty modelLoaded = new SimpleBooleanProperty(false);

    private List<String> subjectsList;

    BooleanProperty partitionAvailable = new SimpleBooleanProperty(false);

    private AtomicBoolean anytimeCut = new AtomicBoolean(false);

    private StringProperty loadingState = new SimpleStringProperty("");

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
            filterSubjectsField.setText("");
            String filename = filenameField.getText();
            modelLoaded.setValue(false);
            ModelLoad loader = new ModelLoad(filename, format.getValue(), md, this, subjectsList, modelLoaded);
            Thread load = new Thread(loader);
            loader.stateProperty().bindBidirectional(loadingState);
            Label modelState = new Label();
            modelState.textProperty().bindBidirectional(loadingState);
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
        filterSubjectsField.prefWidthProperty().setValue(300);
        filterSubjectsField.disableProperty().bind(modelLoaded.not());
        filterSubjectsButton.setOnMouseClicked(mouseEvent -> filter(filterSubjectsField.getText()));
        filterSubjectsButton.disableProperty().bind(modelLoaded.not());
        caseSensBox.disableProperty().bind(modelLoaded.not());

        safeModeBox.setSelected(true);

        cutLabel.setVisible(false);
        cutLabel.setText("/!\\ Algorithm will stop early /!\\");
        cutButton.disableProperty().bind(partitionAvailable);
        cutButton.setOnMouseClicked(mouseEvent -> {
            if (!anytimeCut.get()) {
                cutActivate();
            } else {
                cutDeactivate();
            }
        });

        finalState.visibleProperty().bind(partitionAvailable);

        selectedNodeField.autosize();
        partitionButton.disableProperty().bind(partitionAvailable.not());
        partitionButton.setOnMouseClicked(mouseEvent -> {
            partitionAccordion.getPanes().clear();
            TitledPane loading = new TitledPane();
            loading.setText("Loading neighbors for " + selectedNodeField.textProperty().get() + " please wait");
            partitionAccordion.getPanes().add(loading);
            Runnable algo = new PartitionRun(md, selectedNodeField.textProperty().get(), partitionAccordion, partitionAvailable, anytimeCut, this, loading, depthLimit);
            Thread thread = new Thread(algo);
            if (timeLimit.getValue() > 0) {
                Thread timeOut = timeOut(timeLimit.getValue().intValue());
                timeOut.start();
            }
            thread.start();
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
            String s2;
            String s3;
            if (!caseSensBox.isSelected()) {
                s2 = s.toLowerCase();
                s3 = colMd.getGraph().shortForm(s).toLowerCase();
            } else {
                s2 = s;
                s3 = colMd.getGraph().shortForm(s);
            }
            if (s2.contains(filter) || s3.contains(filter)) {
                filteredList.add(s);
            }
        }
        safePrompt(filteredList);
    }

    /**
     * @param uri The uri of the Node we want to represnet in the CandidateVisual
     * @return A Border pane with the uri on the left and a button on the right
     */
    public BorderPane candidateVisual(String uri) {
        Button button = new Button();
        button.textProperty().setValue("Select this node");
        button.setOnMouseClicked(mouseEvent -> {
            selectedNodeField.setText(uri);
            selectedNodeField.autosize();
        });
        VisualCandidate res = new VisualCandidate(uri, colMd, button, filterSubjectsField);
        return res;
    }

    /**
     * Changes everything that needs to be changed when the algorithm stopper is activated
     */
    public void cutActivate() {
        if (!partitionAvailable.getValue()) {
            anytimeCut.set(true);

            cutButton.setStyle("-fx-background-color: red");
            cutLabel.setVisible(true);
        }
    }

    /**
     * Changes everything that needs to be changed when the algorithm stopper is deactivated
     */
    public void cutDeactivate() {
        anytimeCut.set(false);

        cutButton.setStyle("");
        cutLabel.setVisible(false);
    }

    private Thread timeOut(int timeLimit) {
        Thread res = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(timeLimit * 1000);
                    cutActivate();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        return res;
    }


    public void safePrompt(List<String> subjectsList) {
        if (subjectsList.size() <= safeModeLimit.getValue() || !safeModeBox.isSelected()) {
            PriorityQueue<String> queue = new PriorityQueue<>(subjectsList);

            Platform.runLater(() -> loadingState.setValue("Building Visuals"));
            while (!queue.isEmpty()) {
                String uri = queue.poll();
                BorderPane visual = candidateVisual(uri);
                Platform.runLater(() -> visual.minWidthProperty().bind((scrollPane.widthProperty())));
                Platform.runLater(() -> candidates.getChildren().add(visual));
            }
            Platform.runLater(() -> partitionCandidates.setTop(new VisualPrefixes(md.getNsPrefixMap(), modelLoaded)));

            Platform.runLater(() -> partitionAvailable.setValue(true));
        } else {
            TitledPane err = new TitledPane();
            err.setText("Too many results");
            err.setContent(new Text("Search gave " + subjectsList.size() + " results " +
                    "\nCurrent Safe Mode limit is " + safeModeLimit.getValue() + " results" +
                    "\nTry refining your filter or disabling/increasing Safe Mode limit" +
                    "\n \n/!\\ Disabling Safe Mode can make the application slow /!\\"));
            Platform.runLater(() -> candidates.getChildren().add(err));
        }


    }


}
