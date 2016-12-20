package recog;

import javafx.application.Application;
import javafx.stage.Stage;
import recog.service.Trainer;
import recog.ui.MainController;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        try {
            URL.setURLStreamHandlerFactory(new CpURLStreamHandlerFactory());
            Class.forName(Trainer.class.getName());
        } catch (Error e) {
            e.printStackTrace();
        }
        MainController controller = new MainController(null);
        controller.showWindow();
        controller.getStage().setTitle("한글 인식기");
    }


    public static void main(String[] args) {
        launch(args);
    }
}
