package implementation.gui.controller;

import implementation.utils.CollectionsModel;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.TitledPane;
import javafx.scene.text.Text;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RiotException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * Runnable for back thread loading the Model, to avoid unresponsive interface during the loading
 *
 * @author nk-fouque
 */
public class ModelLoad implements Runnable {

    /**
     * The list of subjects stored in the controller
     */
    private List<String> subjectsList;
    /**
     * The absolute path of the RDF file to be loaded on the system
     */
    private String file;
    /**
     * The Model to be modified inside the Controller
     */
    private Model md;

    /**
     * The format of the RDF file as a Jena-understandable string
     */
    private String format;

    /**
     * The controller the loader has been called by, to know where to display results and states
     */
    private NeighborsController controller;

    /**
     * Simple string property to show users that the loader is working
     */
    private StringProperty state;

    /**
     * Property used by the controller to know if a controller has been loaded
     */
    private BooleanProperty modelLoaded;

    /**
     * Property used by the controller to know how many blank nodes there are
     */
    private IntegerProperty blankNodesCounter;

    /**
     * Base constructor
     *
     * @param filename          The absolute path of the RDF file on the system
     * @param format            The format of the RDF file as a Jena-understandable string
     * @param md                The Model to be modified inside the Controller
     * @param origin            The Controller the loader has been called by
     * @param subjects          The list of subjects stored in the controller
     * @param loaded            Property used by the controller to know if a controller has been loaded
     * @param blankNodesCounter
     */
    public ModelLoad(String filename, String format, Model md, NeighborsController origin, List<String> subjects, BooleanProperty loaded, IntegerProperty blankNodesCounter) {
        super();
        file = filename;
        this.format = format;
        this.md = md;
        this.subjectsList = subjects;
        this.modelLoaded = loaded;
        state = new SimpleStringProperty("");
        controller = origin;
        this.blankNodesCounter = new SimpleIntegerProperty();
        this.blankNodesCounter.bindBidirectional(blankNodesCounter);
    }

    /**
     * String to show what step of the loading is currently being done
     */
    public StringProperty stateProperty() {
        return state;
    }

    @Override
    public void run() {
        try {
            ObservableList<Node> children = controller.candidates.getChildren();
            Platform.runLater(children::clear);

            Platform.runLater(() -> state.setValue("Reading File"));
            md.removeAll();
            md.read(new FileInputStream(file), null, format);
//            md.write(System.out, format);

            Platform.runLater(() -> state.setValue("Building List"));
            subjectsList.clear();
            blankNodesCounter.set(0);
            controller.colMd = new CollectionsModel(md, null);
            Platform.runLater(() -> controller.search(""));
            Platform.runLater(() -> state.setValue("Model Loaded"));
            Platform.runLater(() -> modelLoaded.setValue(true));
            Platform.runLater(() -> controller.partitionAvailable.setValue(true));

        } catch (FileNotFoundException e) {
            TitledPane err = new TitledPane();
            err.setText("File not found");
            err.setContent(new Text(e.getMessage()));
            Platform.runLater(() -> controller.candidates.getChildren().add(err));
            e.printStackTrace();
        } catch (RiotException e) {
            TitledPane err = new TitledPane();
            err.setText("Problem while parsing file");
            err.setContent(new Text(e.getMessage()));
            Platform.runLater(() -> controller.candidates.getChildren().add(err));
            e.printStackTrace();
        } finally {
            Thread.currentThread().interrupt();
        }
    }
}
