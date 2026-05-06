package pl.rsi.cinema;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainWindow extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        // Ładujemy MainWindow.fxml, który sam sobie zainicjalizuje BookingController
        Parent root = FXMLLoader.load(getClass().getResource("/MainWindow.fxml"));
        Scene scene = new Scene(root);

        stage.setTitle("Cinema Booking");
        stage.setScene(scene);

        stage.setWidth(1280);
        stage.setHeight(820);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}