package implementation.gui.controller;

import implementation.gui.model.VisualPrefixes;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ResIterator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class ModelLoad implements Runnable {

    private List<String> subjectsList;
    private String file;
    private Model md;
    private String format;
    private NeighborsController controller;

    private StringProperty state;
    private BooleanProperty modelLoaded;

    public ModelLoad(String filename, String format, Model md, NeighborsController origin, List<String> subjects, BooleanProperty loaded) {
        super();
        file = filename;
        this.format = format;
        this.md = md;
        this.subjectsList = subjects;
        this.modelLoaded = loaded;
        state = new SimpleStringProperty("");
        controller=origin;
    }

    public StringProperty stateProperty() {
        return state;
    }

    @Override
    public void run() {
        try {
            ObservableList<Node> children = controller.candidates.getChildren();
            Platform.runLater(() -> children.clear());

            Platform.runLater(() -> state.setValue("Reading File"));
            md.removeAll();
            md.read(new FileInputStream(file), null, format);
            md.write(System.out, format);

            Platform.runLater(() -> state.setValue("Building List"));
            subjectsList.clear();
            ResIterator iter = md.listSubjects();
            while (iter.hasNext()) {
                subjectsList.add(iter.nextResource().getURI());
            }
            PriorityQueue<String> queue = new PriorityQueue<>(subjectsList);

            Platform.runLater(() -> state.setValue("Building Visuals"));
            while (!queue.isEmpty()) {
                String uri = queue.poll();
                BorderPane visual = controller.candidateVisual(uri);
                Platform.runLater(() ->visual.minWidthProperty().bind((controller.scrollPane.widthProperty())));
                Platform.runLater(() ->controller.candidates.getChildren().add(visual));
            }
            Platform.runLater(() ->controller.partitionCandidates.setTop(new VisualPrefixes(md.getNsPrefixMap(), modelLoaded)));

            Platform.runLater(() -> state.setValue("Model Loaded"));
            modelLoaded.setValue(true);

        } catch (FileNotFoundException e) {
            TitledPane err = new TitledPane();
            err.setText("File not found");
            err.setContent(new Text(e.getMessage()));
            Platform.runLater(() -> controller.candidates.getChildren().add(err));
            e.printStackTrace();
        } finally {
            Thread.currentThread().interrupt();
        }
    }
}
