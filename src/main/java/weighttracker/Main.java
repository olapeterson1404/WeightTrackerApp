package weighttracker;

import javafx.application.Application;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        ResourceBundle bundle = ResourceBundle.getBundle("ui.messages", new Locale("sv", "SE"));
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/main.fxml"), bundle);
        Pane root = loader.load();
        Scene scene = new Scene(root, 960, 720);
        stage.setTitle("Weight Tracker");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
