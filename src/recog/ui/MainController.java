package recog.ui;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.FileChooser;
import recog.common.CandidateLetter;
import recog.common.Segment;
import recog.common.SegmentGroup;
import recog.service.Trainer;
import recog.util.HanUtil;

import java.awt.Rectangle;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainController extends BaseController {
    @FXML
    private Pane inputPane, inputPane2, candidateBox;
    @FXML
    private ScrollPane scrollPane1, scrollPane2;
    @FXML
    private Button clearButton, trainButton, testButton, recogClearButton, copyButton, saveButton;
    @FXML
    private RadioButton jaumButton, moumButton;
    @FXML
    private TextArea chainText, normText;
    @FXML
    private TextField trainText, recogText;
    @FXML
    private Pane normPane;
    private Path tempPath;
    private Segment tempSegment;
    private List<Segment> segments = new ArrayList<>();
    private boolean startDrag;
    private Point2D anchorPoint;

    private LineTo tempLineTo;
    private Button[] candidateButtons;
    private boolean isLetterSelected;

    public MainController(BaseController parent) {
        super(parent, "main.fxml");
    }

    @Override
    protected void initController(boolean initialize) {
        inputPane.setOnMousePressed((event) -> {
            MainController.this.mousePressed(event);
        });
        inputPane.setOnMouseDragged((event) -> {
            try {
                MainController.this.mouseDragged(event);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        });
        inputPane.setOnMouseReleased((event) -> {
            try {
                MainController.this.mouseReleased(event);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        });
        inputPane2.setOnMousePressed((event) -> {
            MainController.this.mousePressed(event);
        });
        inputPane2.setOnMouseDragged((event) -> {
            try {
                MainController.this.mouseDragged(event);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        });
        inputPane2.setOnMouseReleased((event) -> {
            try {
                MainController.this.mouseReleased(event);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        });

        scrollPane1.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane1.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane2.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane2.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        inputPane.prefWidthProperty().bind(scrollPane1.widthProperty());
        inputPane.prefHeightProperty().bind(scrollPane1.heightProperty());
        inputPane2.prefWidthProperty().bind(scrollPane2.widthProperty());
        inputPane2.prefHeightProperty().bind(scrollPane2.heightProperty());

        candidateButtons = candidateBox.lookupAll(".candidateButton").toArray(new Button[0]);
        for(Button candidateButton : candidateButtons) {
            candidateButton.setOnAction((e)->{
                String append = ((Button)e.getSource()).getText();
                recogText.setText(recogText.getText() + (append.isEmpty() ? " " : append));
                isLetterSelected = true;
            });
        }
    }

    public void mouseReleased(MouseEvent event) throws CloneNotSupportedException {
        Pane pane = (Pane) event.getSource();
        if (tempPath == null) {
            return;
        }

        if (event.getX() < 0 || event.getX() > pane.getWidth() || event.getY() < 0
                || event.getY() > pane.getHeight() - 20) {
            return;
        }

        segments.add((Segment) tempSegment.clone());
        checkPoint(pane, event.getX(), event.getY());
        char d = tempSegment.getDirection();
        chainText.setText(chainText.getText() + d);

        List<Segment> normSegments = Trainer.normalize(segments);

        String normString = "";
        for(Segment seg : normSegments) {
            if(seg == null) {
                normString += "&";
            } else {
                normString += seg.getDirection();
            }
        }
        normText.setText(normString);
        drawNormalizedSegments(normPane, normSegments);

        if(normSegments.size() == 0) {
            return;
        }

        List<CandidateLetter> recognize = Trainer.recognize(new SegmentGroup(normSegments));

        int count = 0;
        for(Button candidateButton : candidateButtons) {
            candidateButton.setManaged(false);
            candidateButton.setVisible(false);
        }

        for(CandidateLetter letter : recognize) {
            if(++count > 4) {
                break;
            }

            candidateButtons[count - 1].setManaged(true);
            candidateButtons[count - 1].setVisible(true);
            candidateButtons[count - 1].setText(String.valueOf(letter.getLetter()));
        }
        candidateButtons[count].setManaged(true);
        candidateButtons[count].setVisible(true);
        candidateButtons[count].setText("");
    }

    private void drawNormalizedSegments(Pane pane, List<Segment> normSegments) {
        int i;
        Rectangle rect = new Rectangle();
        rect.x = 0x7fffffff;
        rect.y = 0x7fffffff;

        int maxX = 0, maxY = 0;
        for(i=0;i<normSegments.size();i++) {
            Segment segment = normSegments.get(i);
            if(segment == null) {
                continue;
            }
            if(rect.x > segment.getX()) {
                rect.x = (int) segment.getX();
            }
            if(rect.x > segment.getEndX()) {
                rect.x = (int) segment.getEndX();
            }
            if(rect.y > segment.getY()) {
                rect.y = (int) segment.getY();
            }
            if(rect.y > segment.getEndY()) {
                rect.y = (int) segment.getEndY();
            }
            if(maxX < segment.getX()) {
                maxX = (int) segment.getX();
            }
            if(maxX < segment.getEndX()) {
                maxX = (int) segment.getEndX();
            }
            if(maxY < segment.getY()) {
                maxY = (int) segment.getY();
            }
            if(maxY < segment.getEndY()) {
                maxY = (int) segment.getEndY();
            }
            if(segment.getDirection() == '9') {
                rect.x -= segment.getDx();
                rect.y -= segment.getDy();
            }
        }

        rect.width = maxX - rect.x;
        rect.height = maxY - rect.y;

        double canvasWidth = ((Pane)pane.getParent()).getWidth() - 30;
        double canvasHeight = ((Pane)pane.getParent()).getHeight() - 30;

        double widthRatio = Math.max(0.01, canvasWidth / rect.width);
        double heightRatio = Math.max(0.01, canvasHeight / rect.height);

        pane.getChildren().clear();

        Path path = new Path();
        for(i=0;i<normSegments.size();i++) {
            Segment segment = normSegments.get(i);
            if(segment == null) {
                continue;
            } else if(segment.getDirection() == '9') {
                Ellipse ellipse = new Ellipse((segment.getX() - rect.x) * widthRatio, (segment.getY() - rect.y) * heightRatio, segment.getDx() * widthRatio, segment.getDy() * heightRatio);
                ellipse.setFill(Color.TRANSPARENT);
                ellipse.setStroke(Color.BLACK);
                ellipse.setStrokeWidth(3);
                pane.getChildren().add(ellipse);
                continue;
            }
            path.getElements().add(new MoveTo((segment.getX() - rect.x) * widthRatio, (segment.getY() - rect.y) * heightRatio));
            path.getElements().add(new LineTo((segment.getEndX() - rect.x) * widthRatio, (segment.getEndY() - rect.y) * heightRatio));
            path.setStrokeWidth(3);
        }

        pane.getChildren().add(path);
    }

    public void mouseDragged(MouseEvent event) throws CloneNotSupportedException {
        Pane pane = (Pane) event.getSource();
        if (event.getX() < 0 || event.getX() > pane.getWidth() || event.getY() < 0
                || event.getY() > pane.getHeight() - 20) {
            return;
        }

        Color color1 = Color.BLUE;

        if (startDrag) {
            tempPath = new Path();
            tempPath.setStroke(color1);
            tempPath.setStrokeWidth(3);
            tempPath.getElements().add(new MoveTo(event.getX(), event.getY()));

            pane.getChildren().add(tempPath);
            tempLineTo = new LineTo(event.getX(), event.getY());
            tempPath.getElements().add(tempLineTo);

            tempSegment = new Segment();
            tempSegment.setX(event.getX());
            tempSegment.setY(event.getY());
            startDrag = false;

            checkPoint(pane, event.getX(), event.getY());
            if(segments.size() > 0) {
                chainText.setText(chainText.getText() + "&");
                segments.add(null);
            }
        } else {
            tempLineTo.setX(event.getX());
            tempLineTo.setY(event.getY());

            double dx = event.getX() - anchorPoint.getX();
            double dy = event.getY() - anchorPoint.getY();
            tempSegment.setDx(dx);
            tempSegment.setDy(dy);

            if(dx*dx + dy*dy > 200) {
                segments.add((Segment) tempSegment.clone());
                checkPoint(pane, event.getX(), event.getY());
                char d = tempSegment.getDirection();
                chainText.setText(chainText.getText() + d);

                tempSegment.setX(event.getX());
                tempSegment.setY(event.getY());
            }
        }
    }

    private void checkPoint(Pane pane, double x, double y) {
        anchorPoint = new Point2D(x, y);
        tempLineTo = new LineTo(x, y);
        tempPath.getElements().add(tempLineTo);
        Circle circle = new Circle();
        circle.setCenterX(x);
        circle.setCenterY(y);
        circle.setRadius(4);
        circle.setStroke(Color.BLUE);
        circle.setStrokeWidth(3);
        circle.setFill(Color.WHITE);
        pane.getChildren().add(circle);
    }

    public void mousePressed(MouseEvent event) {
        Pane pane = (Pane) event.getSource();
        if (event.getX() < 0 || event.getX() > pane.getWidth() || event.getY() < 0
                || event.getY() > pane.getHeight()) {
            return;
        }

        if(pane == inputPane && inputPane2.getChildren().size() > 0) {
            if(candidateButtons[1].isVisible() && !isLetterSelected) {
                candidateButtons[0].fire();
            }
            clearButton.fire();
        } else if(pane == inputPane2 && inputPane.getChildren().size() > 0) {
            if(candidateButtons[1].isVisible() && !isLetterSelected) {
                candidateButtons[0].fire();
            }
            clearButton.fire();
        }

        anchorPoint = new Point2D(event.getX(), event.getY());
        startDrag = true;
        tempPath = null;
    }

    public void onClick(Event e) {
        Object source = e.getSource();

        if(source == clearButton) {
            segments.clear();
            inputPane.getChildren().clear();
            inputPane2.getChildren().clear();
            normPane.getChildren().clear();
            chainText.clear();
            normText.clear();
            trainText.clear();
            isLetterSelected = false;
        } else if(source == trainButton) {
            if(trainText.getText().isEmpty() || segments.size() == 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("Please draw letter.");
                alert.showAndWait();
                return;
            } else {
                try {
                    char letter = trainText.getText().charAt(0);
                    String chainCode = normText.getText();

                    if(HanUtil.isJongsung(letter) || HanUtil.isChosung(letter) || HanUtil.isJungsung(letter)) {
                        String fileName = Trainer.JAUM_FILENAME;
                        if(HanUtil.isJungsung(letter)) {
                            fileName = Trainer.MOUM_FILENAME;
                        }

                        if(Trainer.train(letter, chainCode)) {
                            PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName, true), "euc-kr"));
                            writer.println(letter + chainCode);
                            writer.flush();
                            writer.close();
                        }

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setContentText("Finish");
                        alert.showAndWait();
                    } else {
                        List<Segment> normSegments = Trainer.normalize(segments);
                        RecognizeController controller = new RecognizeController(this, normSegments, letter);
                        controller.showWindow();
                        drawNormalizedSegments(controller.getNormPane(), normSegments);
                    }

                    clearButton.fire();
                } catch (IOException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(ex.getClass().toString());
                    alert.setContentText(ex.getLocalizedMessage());
                    alert.showAndWait();
                }

            }
        } else if(source == testButton) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choose file");
            fileChooser.setInitialDirectory(new File(System.getProperty("user.dir")));
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Test data files (*.txt)", "*.txt");
            fileChooser.getExtensionFilters().add(extFilter);
            List<File> files = fileChooser.showOpenMultipleDialog(stage);
            if(files != null && files.size() > 0) {
                int allCount = 0;
                int okCount = 0;
                for (File file : files) {
                    try {
                        int[] scores = Trainer.testFromCoordinateFile(new FileInputStream(file));
                        allCount += scores[1];
                        okCount += scores[0];
                    } catch (FileNotFoundException e1) {
                        e1.printStackTrace();
                    }
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Recognize Test Result");
                alert.setContentText(String.format("Of the total %d records, %d are correct. \nAccuracy : %.1f%%", allCount, okCount, okCount / (double) allCount * 100));
                alert.showAndWait();
            }
        } else if(source == recogClearButton) {
            recogText.clear();
        } else if(source == copyButton) {
            String text = recogText.getText();
            Map<DataFormat, Object> content = new HashMap<>();
            content.put(DataFormat.PLAIN_TEXT, text);
            Clipboard.getSystemClipboard().setContent(content);
        } else if(source == saveButton) {
            if(trainText.getText().isEmpty() || segments.size() == 0) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("Please input letter to train");
                alert.showAndWait();
                return;
            } else {
                try {
                    char letter = trainText.getText().charAt(0);
                    String fileName = "coord.txt";
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName, true), "euc-kr"));
                    writer.println(letter + " " + new SegmentGroup(segments).toCoordString());
                    writer.flush();
                    writer.close();

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setContentText("Added to coord.txt");
                    alert.showAndWait();

                    clearButton.fire();
                } catch (IOException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle(ex.getClass().toString());
                    alert.setContentText(ex.getLocalizedMessage());
                    alert.showAndWait();
                }

            }
        }
    }
}
