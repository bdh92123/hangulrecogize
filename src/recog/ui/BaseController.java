package recog.ui;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import recog.util.FXUtil;

import java.awt.*;

public abstract class BaseController {
	protected Stage ownerStage, stage;
	private boolean sizeToScene;
	private Parent root;
	private String fxml;
	protected Scene scene;
	private static int screenWidth;
	private static int screenHeight;
	private boolean useOwnerStage;
	private BaseController parentController;
	private boolean softClose;

	private EventHandler<WindowEvent> closeRequestHandler = new EventHandler<WindowEvent>() {
		@Override
		public void handle(WindowEvent event) {
			BaseController.this.onCloseRequest(event, softClose);
		}
	};
	

	static {
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		screenWidth = gd.getDisplayMode().getWidth();
		screenHeight = gd.getDisplayMode().getHeight();
	}

	public BaseController(BaseController parent, String fxml) {
		this(parent, fxml, true, true);
	}

	public BaseController(BaseController parent, String fxml, boolean useOwnerStage) {
		this(parent, fxml, useOwnerStage, true);
	}

	public BaseController(BaseController parent, String fxml, boolean useOwnerStage, boolean sizeToScene) {
		this.fxml = fxml;
		this.parentController = parent;
		if (parent != null) {
			this.ownerStage = parent.getStage();
		}
		this.useOwnerStage = useOwnerStage;
		this.sizeToScene = sizeToScene;
	}

	protected abstract void initController(boolean initialize);

	public Parent getRoot() {
		if (root == null) {
			makeRoot();
			assert root != null;
		}

		return root;
	}

	public Scene getScene() {
		assert Platform.isFxApplicationThread();

		if (scene == null) {
			scene = new Scene(getRoot());
			scene.setFill(Color.TRANSPARENT);
		}

		return scene;
	}

	private Stage prepareStage() {
		assert Platform.isFxApplicationThread();

		if (stage == null) {
			if (!useOwnerStage || ownerStage == null) {
				stage = new Stage();
			} else {
				stage = ownerStage;
			}
			if (stage != ownerStage) {
				// stage.initOwner(ownerStage);
			}

			stage.setScene(getScene());
			initController(true);
			stage.setOnCloseRequest(closeRequestHandler);
		} else {
			stage.setScene(getScene());
			initController(false);
			stage.setOnCloseRequest(closeRequestHandler);
		}

		if (sizeToScene) {
			stage.sizeToScene();
		}

		stage.getIcons().clear();
		stage.getIcons().add(new Image(BaseController.class.getResourceAsStream("/img/recog.png")));

		Platform.runLater(() -> {
			stage.setMinWidth(stage.getWidth());
			stage.setMinHeight(stage.getHeight());
		});
		return stage;
	}

	public Stage getStage() {
		return stage;
	}

	public void showWindow() {
		showWindow(false);
	}

	public void showWindow(boolean wait) {
		assert Platform.isFxApplicationThread();

		prepareStage();
		getStage().toFront();
		if (wait) {
			if (!getStage().getModality().equals(Modality.APPLICATION_MODAL)) {
				getStage().initModality(Modality.APPLICATION_MODAL);
			}
			getStage().showAndWait();
		} else {
			getStage().show();
		}
	}

	public void closeWindow() {
        assert Platform.isFxApplicationThread();
    	getStage().close();
    	softClose = true;
    	stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    	softClose = false;
    }

	public void prevWindow() {
		if (parentController != null) {
			parentController.showWindow();
		}
	}

	public void onCloseRequest(WindowEvent event, boolean softClose) {
	}

	protected final void setRoot(Parent root) {
		assert root != null;
		this.root = root;
	}


	protected Node makeRoot() {
		try {
			if (fxml != null) {
				Parent node = (Parent) FXUtil.loadFxml(fxml, this);
				this.root = node;
				this.root.setUserData(this);
				return node;
			} else {
				this.root = new Pane();
				return this.root;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static int getScreenWidth() {
		return screenWidth;
	}

	public static int getScreenHeight() {
		return screenHeight;
	}

	public BaseController getParentController() {
		return parentController;
	}
	
	public boolean isShowing() {
		return stage != null && stage.isShowing();
	}
}
