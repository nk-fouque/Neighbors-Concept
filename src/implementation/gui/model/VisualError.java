package implementation.gui.model;

import javafx.scene.control.TitledPane;
import javafx.scene.text.Text;

public class VisualError {
    public static TitledPane standardError(Exception e){
        e.printStackTrace();
        TitledPane error = new TitledPane();
        error.setText("Something went wrong :/");
        error.setContent(new Text("See stack trace for details"));
        return error;
    }

    public static TitledPane partitionError(){
        TitledPane error = new TitledPane();
        error.setText("Something went wrong :/");
        error.setContent(new Text("Error during partition \n" +
                "Details should be found in stack trace"));
        return error;
    }
}
