package recog.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import recog.common.Segment;
import recog.common.SegmentGroup;
import recog.service.Trainer;
import recog.util.HanUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class RecognizeController extends BaseController {
    @FXML
    private Button trainButton;
    @FXML
    private ComboBox<Character> chosungList, jungsungList, jongsungList;
    @FXML
    private TextArea normText;
    @FXML
    private HBox chosungChainBox, jungsungChainBox, jongsungChainBox;
    @FXML
    private Pane normPane, chainPane;
    @FXML
    private Label chosungChainLabel, jungsungChainLabel, jongsungChainLabel;

    private char letter;
    private SegmentGroup segmentGroup;

    private int chosungEndIndex = -1, jungsungEndIndex = -1;
    private String chosungChain = "";
    private String jungsungChain = "";
    private String jongsungChain = "";

    public RecognizeController(BaseController parent, List<Segment> segments, char letter) {
        super(parent, "recognize.fxml", false);
        this.letter = letter;
        this.segmentGroup = new SegmentGroup(segments);
    }

    @Override
    protected void initController(boolean initialize) {
        ObservableList<Character> chosungs = FXCollections.observableArrayList();
        ObservableList<Character> jungsungs = FXCollections.observableArrayList();
        ObservableList<Character> jongsungs = FXCollections.observableArrayList();
        for(char chosung : HanUtil.CHOSUNG_LIST) {
            chosungs.add(chosung);
        }
        for(char jungsung : HanUtil.JUNGSUNG_LIST) {
            jungsungs.add(jungsung);
        }
        for(char jongsung : HanUtil.JONGSUNG_LIST) {
            jongsungs.add(jongsung);
        }

        chosungList.setItems(chosungs);
        jungsungList.setItems(jungsungs);
        jongsungList.setItems(jongsungs);


        char chosung = HanUtil.getChosung(letter);
        char jungsung = HanUtil.getJungsung(letter);
        char jongsung = HanUtil.getJongsung(letter);
        boolean useJongsung = jongsung != ' ';

        chosungList.getSelectionModel().select(Arrays.binarySearch(HanUtil.CHOSUNG_LIST, chosung));
        jungsungList.getSelectionModel().select(Arrays.binarySearch(HanUtil.JUNGSUNG_LIST, jungsung));
        jongsungList.getSelectionModel().select(Arrays.binarySearch(HanUtil.JONGSUNG_LIST, jongsung));

        String chain = segmentGroup.toChainCode();
        if(useJongsung == false) {
            jongsungList.getSelectionModel().clearSelection();
            jongsungList.setDisable(true);
        } else {
            jongsungList.setDisable(false);
        }

        char[] chainArray = chain.toCharArray();
        for(char ch : chainArray) {
            chosungChainBox.getChildren().add(new Button(String.valueOf(ch)));
            jungsungChainBox.getChildren().add(new Button(String.valueOf(ch)));
            jongsungChainBox.getChildren().add(new Button(String.valueOf(ch)));
        }

        chainPane.lookupAll(".button").forEach((button)->{
            int index = button.getParent().getChildrenUnmodifiable().indexOf(button);
            button.setUserData(index);
            if(chosungChainBox.getChildren().indexOf(button) == -1) {
                button.setDisable(true);
            }
            button.getStyleClass().add("off");
            ((Button) button).setOnAction((e)->{
                final int clickIndex = (int) ((Button) e.getSource()).getUserData();
                boolean isChosung = chosungChainBox.getChildren().indexOf(e.getSource()) > -1;
                boolean isJungsung = jungsungChainBox.getChildren().indexOf(e.getSource()) > -1;

                if(isChosung) {
                    chosungChainBox.getChildren().forEach(node->{
                        node.getStyleClass().setAll("button");
                        if((int) node.getUserData() > clickIndex) {
                            node.getStyleClass().add("off");
                        }
                    });
                    jungsungChainBox.getChildren().forEach(node->{
                        if(!node.getStyleClass().contains("off") && (int) node.getUserData() <= clickIndex) {
                            node.getStyleClass().setAll("button", "off");
                            node.setDisable(true);
                        } else if(node.getStyleClass().contains("off")  && (int) node.getUserData() > clickIndex) {
                            node.getStyleClass().setAll("button");
                            node.setDisable(false);
                        }
                    });
                    jongsungChainBox.getChildren().forEach(node->{
                        node.getStyleClass().setAll("button", "off");
                    });
                    chosungEndIndex = clickIndex;
                } else if(isJungsung) {
                    jungsungChainBox.getChildren().forEach(node->{
                        if(!node.isDisabled() && (int) node.getUserData() <= clickIndex) {
                            node.getStyleClass().setAll("button");
                        } else if(!node.getStyleClass().contains("off") && (int) node.getUserData() > clickIndex) {
                            node.getStyleClass().setAll("button", "off");
                            node.setDisable(false);
                        }
                    });
                    jongsungChainBox.getChildren().forEach(node->{
                        if(!node.getStyleClass().contains("off") && (int) node.getUserData() <= clickIndex) {
                            node.getStyleClass().setAll("button", "off");
                        } else if(node.getStyleClass().contains("off") && (int) node.getUserData() > clickIndex) {
                            node.getStyleClass().setAll("button");
                        }
                    });
                    jungsungEndIndex = clickIndex;
                }

                refreshChainCode();
            });
        });
        normText.setText(segmentGroup.toChainCode());
    }

    private void refreshChainCode() {
        String chain = segmentGroup.toChainCode();
        if(chosungEndIndex != -1) {
            chosungChain = chain.substring(0, chosungEndIndex + 1);
        }
        if(jungsungEndIndex != -1) {
            jungsungChain = chain.substring(chosungEndIndex + 1, jungsungEndIndex + 1);
            jongsungChain = chain.substring(jungsungEndIndex + 1);
        } else {
            jungsungChain = chain.substring(chosungEndIndex + 1);
        }

        if(chosungChain.endsWith("&"))
            chosungChain = chosungChain.substring(0, chosungChain.length() - 1);
        if(jungsungChain.startsWith("&"))
            jungsungChain = jungsungChain.substring(1);
        if(jungsungChain.endsWith("&"))
            jungsungChain = jungsungChain.substring(0, jungsungChain.length() - 1);
        if(jongsungChain.startsWith("&"))
            jongsungChain = jongsungChain.substring(1);

        chosungChainLabel.setText(" (" + chosungChain + ")");
        jungsungChainLabel.setText(" (" + jungsungChain + ")");
        jongsungChainLabel.setText(" (" + jongsungChain + ")");
    }

    public void onClick(Event e) {
        Object source = e.getSource();

        if(source == trainButton) {
            try {
                char chosung = (char) chosungList.getSelectionModel().getSelectedItem();
                char jungsung = (char) jungsungList.getSelectionModel().getSelectedItem();
                char jongsung = (char) jungsungList.getSelectionModel().getSelectedItem();
                boolean useJongsung = HanUtil.getJongsung(letter) != ' ';

                if(chosungChain.isEmpty() || jungsungChain.isEmpty() || (jongsungChain.isEmpty() && useJongsung)) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setContentText("Please select valid range of letter's chaincode.");
                    alert.showAndWait();
                    return;
                }

                Trainer.train(chosung, chosungChain);
                Trainer.train(jungsung, jungsungChain);
                if(useJongsung) {
                    Trainer.train(jongsung, jongsungChain);
                }

                Trainer.saveToChainFile();
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setContentText("Finished !");
                alert.showAndWait();

            } catch (IOException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(ex.getClass().toString());
                alert.setContentText(ex.getLocalizedMessage());
                alert.showAndWait();
            }
        }
    }

    public Pane getNormPane() {
        return normPane;
    }
}
