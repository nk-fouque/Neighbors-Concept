package implementation.gui.controller;

import implementation.Cluster;
import implementation.Partition;
import implementation.gui.NeighborsInterface;
import implementation.gui.model.CopyButton;
import implementation.gui.model.NeighborButton;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
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
    }

    private void filter(String filter) {
        candidates.getChildren().clear();
        List<String> filteredList = new ArrayList<>();
        for (String s : subjectsList) {
            if (s.contains(filter)) {
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

    public static TitledPane clusterVisual(Cluster c) {
        TitledPane res = new TitledPane();
        res.setText("Extentional Distance : " + c.getRelaxDistance());
        if (c.getAvailableQueryElements().size() != 0) {
            res.textFillProperty().setValue(Paint.valueOf("#cd7777"));
        } else {
            res.textFillProperty().setValue(Paint.valueOf("#484848"));
        }
        BorderPane pane = new BorderPane();
        res.setContent(pane);

        Text similitude = new Text("Similitude : \n" + c.getRelaxQueryElements().toString().replace(",", "\n"));

        Text neighbors = new Text("\nNeighbors : \n" + c.getAnswersList().toString().replace(',', '\n'));

        VBox texts = new VBox();
        texts.getChildren().addAll(similitude,neighbors);
        pane.setLeft(texts);

        pane.setRight(new CopyButton(texts));

        res.autosize();
        return res;
    }

    public BorderPane candidateVisual(String uri) {
        BorderPane res = new BorderPane();
        Label text = new Label("  " + uri);
        text.getStyleClass().add("sparklis-blue");
        res.setLeft(text);
        res.setRight(new NeighborButton(uri, partitionAccordion, md, partition, partitionAvailable, anytimeCut));
        return res;
    }

}
