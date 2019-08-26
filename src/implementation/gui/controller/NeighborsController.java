package implementation.gui.controller;

import implementation.algorithms.Cluster;
import implementation.gui.NeighborsInterface;
import implementation.gui.model.*;
import implementation.utils.CollectionsModel;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main controller
 *
 * @author nk-fouque
 */
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
    CheckBox ignoreBNodeBox;

    @FXML
    Button previousNavigation;
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
    VBox partitionResultsBox;

    @FXML
    Spinner<Integer> timeLimit;
    @FXML
    Spinner<Integer> depthLimit;
    @FXML
    Button cutButton;
    @FXML
    Label cutLabel;
    @FXML
    Label finalState;
    @FXML
    Button furtherButton;
    @FXML
    Button fullExportButton;

    @FXML
    TextField selectedNodeField;
    @FXML
    Button partitionButton;
    @FXML
    ChoiceBox<String> sortMode;

    private Model md;

    public CollectionsModel colMd;

    public ObservablePartition partition;

    private BooleanProperty modelLoaded = new SimpleBooleanProperty(false);

    BooleanProperty partitionAvailable = new SimpleBooleanProperty(false);

    private AtomicBoolean anytimeCut = new AtomicBoolean(false);

    private StringProperty loadingState = new SimpleStringProperty("");

    private IntegerProperty blankNodesCounter = new SimpleIntegerProperty(0);

    private Stack<SearchHistoryElement> history = new Stack<>();

    private SearchHistoryElement currentSearch = new SearchHistoryElement(false, true, false, "");


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

    private static List<String> sortModes() {
        List<String> res = new ArrayList<>();
        res.add("Extensional Distance");
        res.add("Number of Relaxations");
        return res;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pane.setPrefSize(1920, 1080);

        md = ModelFactory.createDefaultModel();

        format.setItems(new SortedList<>(FXCollections.observableList(formats())));
        format.setValue("TURTLE");

        final FileChooser loadChooser = new FileChooser();
        fileFindButton.setOnMouseClicked(mouseEvent -> {


            File file = loadChooser.showOpenDialog(NeighborsInterface.stage);
            if (file != null) {
                filenameField.textProperty().setValue(file.getAbsolutePath());
            }
        });

        modelLoadButton.setOnMouseClicked(mouseEvent -> {
            filterSubjectsField.setText("");
            String filename = filenameField.getText();
            modelLoaded.setValue(false);
            ModelLoad loader = new ModelLoad(filename, format.getValue(), md, this, modelLoaded, blankNodesCounter);
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
                click(filterSubjectsButton);
            }
        });
        filterSubjectsField.prefWidthProperty().setValue(300);
        filterSubjectsField.disableProperty().bind(modelLoaded.not());
        filterSubjectsButton.setOnMouseClicked(mouseEvent -> {
            if (!filterSubjectsButton.isDisabled())
                search(filterSubjectsField.getText());
        });
        filterSubjectsButton.disableProperty().bind(modelLoaded.not());
        caseSensBox.disableProperty().bind(modelLoaded.not());
        caseSensBox.setOnMouseClicked(mouseEvent -> click(filterSubjectsButton));
        ignoreBNodeBox.setOnMouseClicked(mouseEvent -> click(filterSubjectsButton));
        ignoreBNodeBox.setSelected(true);

        safeModeBox.setSelected(true);

        previousNavigation.setOnMouseClicked(mouseEvent -> {
            if (!history.empty()) {
                SearchHistoryElement search = history.pop();
                currentSearch = search;
                filterSubjectsField.textProperty().setValue(search.getFilter());
                filter(search);
            }
        });

        sortMode.setItems(new SortedList<>(FXCollections.observableList(sortModes())));
        sortMode.setValue("Extensional Distance");
        sortMode.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                if (partitionAvailable.get()) {
                    PriorityQueue<VisualCluster> queue = new PriorityQueue<>(new VisualClusterComparator(sortMode.getValue()));
                    partitionResultsBox.getChildren().forEach(node -> {
                        if (node instanceof VisualCluster) {
                            queue.add((VisualCluster) node);
                        }
                    });
                    partitionResultsBox.getChildren().clear();
                    while (!queue.isEmpty()) {
                        partitionResultsBox.getChildren().add(queue.poll());
                    }
                }
            }
        });
        sortMode.autosize();

        cutLabel.setVisible(false);
        cutLabel.setText("/!\\ Algorithm will stop soon /!\\");
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
            partitionResultsBox.getChildren().clear();
            TitledPane loading = new TitledPane();
            loading.setText("Loading neighbors for " + selectedNodeField.textProperty().get() + " please wait");
            partitionResultsBox.getChildren().add(loading);
            Runnable algo = new PartitionRun(md, selectedNodeField.textProperty().get(), partitionResultsBox, partitionAvailable, anytimeCut, this, loading, depthLimit);
            Thread thread = new Thread(algo);
            if (timeLimit.getValue() > 0) {
                Thread timeOut = timeOut(timeLimit.getValue().intValue());
                timeOut.start();
            }
            thread.start();
        });

        furtherButton.visibleProperty().bind(partitionAvailable);
        furtherButton.setOnMouseClicked(mouseEvent -> {
            partitionResultsBox.getChildren().clear();
            if (partition == null) {
                Platform.runLater(() -> partitionResultsBox.getChildren().clear());
                TitledPane error = new TitledPane();
                error.setText("No Partition to resume");
                error.setContent(new Text("A partition must have been interrupted manually or by time limit to be resumed"));
                Platform.runLater(() -> partitionResultsBox.getChildren().add(error));
            } else {
                furtherPartition(partition.furtherPartitioningcandidates());
            }
        });

        FileChooser saveChooser = new FileChooser();
        FileChooser.ExtensionFilter jsonFilter = new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json");
        saveChooser.getExtensionFilters().add(jsonFilter);
        saveChooser.setInitialFileName("partition_results.json");
        fullExportButton.setOnMouseClicked(mouseEvent -> {
            if (partition!=null){
                //Show save file dialog
                File file = saveChooser.showSaveDialog(NeighborsInterface.stage);

                if (file != null) {
                    try {
                        PrintWriter writer;
                        writer = new PrintWriter(file);
                        writer.println(partition.toJson());
                        writer.close();
                    } catch (IOException ex) {
                        Logger.getLogger(NeighborsController.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                Platform.runLater(() -> partitionResultsBox.getChildren().clear());
                TitledPane error = new TitledPane();
                error.setText("No results to export");
                error.setContent(new Text("Start a partition to be able to export its results"));
                Platform.runLater(() -> partitionResultsBox.getChildren().add(error));
            }


        });
        fullExportButton.visibleProperty().bind(partitionAvailable);

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
        blankNodesCounter.set(0);
        List<Resource> filteredList = new ArrayList<>();
        final String actualFilter;
        if (!caseSensBox.isSelected()) actualFilter = filter.toLowerCase();
        else actualFilter = filter;
        colMd.getSaturatedGraph().listSubjects().toList().parallelStream().forEachOrdered(resource -> {
            if (!resource.isAnon() || !ignoreBNodeBox.isSelected()) {
                String s = resource.toString();
                String s2;
                String s3;
                if (!caseSensBox.isSelected()) {
                    s2 = s.toLowerCase();
                    s3 = colMd.shortform(s).toLowerCase();
                } else {
                    s2 = s;
                    s3 = colMd.shortform(s);
                }
                if (s2.contains(actualFilter) || s3.contains(actualFilter)) {
                    filteredList.add(resource);
                    if (resource.isAnon()) blankNodesCounter.set(blankNodesCounter.get() + 1);
                }
            }
        });
        safePrompt(filteredList);
    }

    protected void search(String filter) {
        history.push(new SearchHistoryElement(currentSearch));
        currentSearch = new SearchHistoryElement(caseSensBox.isSelected(), safeModeBox.isSelected(), ignoreBNodeBox.isSelected(), filter);
        filter(filter);
    }

    private void filter(SearchHistoryElement search) {
        caseSensBox.setSelected(search.isCaseSens());
        safeModeBox.setSelected(search.isSafeMode());
        ignoreBNodeBox.setSelected(search.isbNode());
        filter(search.getFilter());
    }

    /**
     * @param uri The uri of the Node we want to represent in the CandidateVisual
     * @return A Border pane with the uri on the left and a button on the right
     */
    public BorderPane candidateVisual(String uri) {
        VisualCandidate res = new VisualCandidate(uri, colMd, selectedNodeField, filterSubjectsField);
        return res;
    }

    /**
     * @param id The AnonID of the Node we want to represent in the CandidateVisual
     * @return A Border pane with the uri on the left and a button on the right
     */
    public BorderPane bNodeVisual(String id) {
        VisualCandidate res = new VisualBNode(id, colMd, selectedNodeField, filterSubjectsField);
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

    /**
     * Prompts candidates for partitioning respecting the safe mode limit
     *
     * @param subjectsList
     */
    public void safePrompt(List<Resource> subjectsList) {
        if (subjectsList.size() <= safeModeLimit.getValue() || !safeModeBox.isSelected()) {
            subjectsList.sort(Comparator.comparing(Resource::toString));
            Platform.runLater(() -> loadingState.setValue("Building Visuals"));
            subjectsList.parallelStream().forEachOrdered(resource -> {
                if (resource.isURIResource()) {
                    BorderPane visual = candidateVisual(resource.getURI());
                    Platform.runLater(() -> visual.minWidthProperty().bind((scrollPane.widthProperty())));
                    Platform.runLater(() -> candidates.getChildren().add(visual));
                } else if (resource.isAnon()) {
                    BorderPane visual = bNodeVisual(resource.getId().toString());
                    Platform.runLater(() -> visual.minWidthProperty().bind((scrollPane.widthProperty())));
                    Platform.runLater(() -> candidates.getChildren().add(visual));
                }
            });
            Platform.runLater(() -> partitionCandidates.setTop(new VisualPrefixes(md.getNsPrefixMap(), modelLoaded)));
        } else {
            TitledPane err = new TitledPane();
            err.setText("Too many results");
            String text = "Search gave " + subjectsList.size() + " results";
            if (blankNodesCounter.get() != 0) {
                text += "\n(" + blankNodesCounter.get() + " anonymous nodes)";
            }
            text += "\nCurrent Safe Mode limit is " + safeModeLimit.getValue() + " results" +
                    "\nTry refining your filter or disabling/increasing Safe Mode limit" +
                    "\n \n/!\\ Disabling Safe Mode can make the application freeze /!\\";

            err.setContent(new Text(text));
            Platform.runLater(() -> candidates.getChildren().add(err));

            Button firstResults = new Button("Show first " + safeModeLimit.getValue() + " results");
            firstResults.setOnMouseClicked(mouseEvent -> {
                subjectsList.sort(Comparator.comparing(Resource::toString));
                List<Resource> truncated = subjectsList.subList(0, safeModeLimit.getValue() - 1);
                safePrompt(truncated);
                firstResults.setVisible(false);
            });
            Platform.runLater(() -> candidates.getChildren().add(firstResults));
        }
    }

    public static void click(Button button) {
        button.getOnMouseClicked().handle(
                new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0, MouseButton.NONE,
                        1, false, false, false, false, false, false, false, false, false, false, null));
    }

    public void furtherPartition(Collection<Cluster> clusters) {
        Runnable algo = new PartitionAdditional(this, clusters, partitionResultsBox, anytimeCut);
        Thread thread = new Thread(algo);
        if (timeLimit.getValue() > 0) {
            Thread timeOut = timeOut(timeLimit.getValue().intValue());
            timeOut.start();
        }
        thread.start();
    }

}
