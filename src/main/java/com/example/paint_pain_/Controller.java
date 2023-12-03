package com.example.paint_pain_;

import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXComboBox;
import com.jfoenix.controls.JFXSlider;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.geometry.Point2D;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.TextAlignment;
import javafx.scene.transform.Scale;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

public class Controller {
    @FXML
    private SplitPane splitPane;
    @FXML
    private Canvas canvas;
    @FXML
    private JFXButton paintButton;
    @FXML
    private JFXButton eraseButton;
    @FXML
    private JFXButton textButton;
    @FXML
    private JFXButton fillButton;
    @FXML
    private JFXButton redoButton;
    @FXML
    private JFXButton undoButton;
    @FXML
    private JFXButton eyeButton;
    @FXML
    private ColorPicker colorPicker;
    private GraphicsContext graphicsContext;
    @FXML
    private JFXSlider SizeSlider;
    @FXML
    private JFXComboBox <String> brushType;
    @FXML
    private JFXSlider OpacitySlider;
    @FXML
    private Label opacity;
    @FXML
    private JFXButton lineButton;
    @FXML
    private JFXButton zoomButton;
    private double startX,startY ;
    private Stack<Image> undoStack = new Stack<>();
    private Stack<Image> redoStack = new Stack<>();
    private double lineStartX, lineStartY;
    @FXML
    private ScrollPane canvasScrollPane;
    private boolean zoomEnabled = false;


    private final double ZOOM_FACTOR = 1.1; // Adjust this value for the zoom speed
    private final Scale zoomTransform = new Scale(1, 1);










    public void initialize() {
        canvas.getTransforms().add(zoomTransform);
        initializeZoomAndPan();
        graphicsContext = canvas.getGraphicsContext2D();
        graphicsContext.setFill(Color.WHITE);
        graphicsContext.setStroke(Color.BLACK);
        graphicsContext.setLineCap(StrokeLineCap.ROUND);
        graphicsContext.setLineJoin(StrokeLineJoin.ROUND);
        graphicsContext.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphicsContext.setLineWidth(SizeSlider.getValue());
        initializeBrushTypeComboBox();

    }

    private void initializeZoomAndPan() {
        canvasScrollPane.setOnScroll(event -> {
            if (zoomEnabled) {
                double zoomFactor = 1.05;
                double deltaY = event.getDeltaY();

                if (deltaY < 0) {
                    zoomFactor = 2.0 - zoomFactor;
                }
                canvas.setScaleX(canvas.getScaleX() * zoomFactor);
                canvas.setScaleY(canvas.getScaleY() * zoomFactor);
                event.consume();
            }
        });
    }
    @FXML
    private void toggleZoom() {
        zoomEnabled = !zoomEnabled;
    }

    private void zoomIn() {
        zoomTransform.setX(zoomTransform.getX() * ZOOM_FACTOR);
        zoomTransform.setY(zoomTransform.getY() * ZOOM_FACTOR);
    }

    private void zoomOut() {
        zoomTransform.setX(zoomTransform.getX() / ZOOM_FACTOR);
        zoomTransform.setY(zoomTransform.getY() / ZOOM_FACTOR);
    }
    private void saveCanvasState() {
        WritableImage snapshot = canvas.snapshot(new SnapshotParameters(), null);
        undoStack.push(snapshot);
        redoStack.clear();
    }

    @FXML
    private void setColorPicker() {
        colorPicker.setOnAction(event -> {
            graphicsContext.setStroke(colorPicker.getValue());
        });
    }

    @FXML
    private void setStrokeSize(){
        SizeSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            graphicsContext.setLineWidth(newValue.doubleValue());
        });
    }
    @FXML
    private void setOpacitySlider(){
        OpacitySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            double opacity = newValue.doubleValue();
            Color currentColor = colorPicker.getValue();
            Color newColor = new Color(currentColor.getRed(), currentColor.getGreen(),
                    currentColor.getBlue(), opacity);

            graphicsContext.setStroke(newColor);
            graphicsContext.setGlobalBlendMode(BlendMode.SRC_OVER);
        });
        opacity.setText(String.format("%.2f", OpacitySlider.getValue()));

    }


    private void MousePressedPrinting(MouseEvent mouseEvent) {
        saveCanvasState();
        graphicsContext.beginPath();
        graphicsContext.moveTo(mouseEvent.getX(), mouseEvent.getY());

        double opacity = OpacitySlider.getValue();
        Color currentColor = colorPicker.getValue();
        Color newColor = new Color(currentColor.getRed(), currentColor.getGreen(),
                currentColor.getBlue(), opacity);

        graphicsContext.setStroke(newColor);
    }

    private void MouseDraggedPrinting(MouseEvent mouseEvent) {
        graphicsContext.lineTo(mouseEvent.getX(), mouseEvent.getY());
        graphicsContext.stroke();
    }

    private void MouseReleasedPrinting(MouseEvent mouseEvent) {
        graphicsContext.closePath();
        saveCanvasState();
    }
    private void MousePressedErasing(MouseEvent mouseEvent) {
        saveCanvasState();
        graphicsContext.beginPath();
        graphicsContext.moveTo(mouseEvent.getX(), mouseEvent.getY());
        graphicsContext.setStroke(Color.WHITE);
    }

    private void MouseDraggedErasing(MouseEvent mouseEvent) {
        graphicsContext.lineTo(mouseEvent.getX(), mouseEvent.getY());
        graphicsContext.stroke();
    }

    private void MouseReleasedErasing(MouseEvent mouseEvent) {
        graphicsContext.setGlobalBlendMode(BlendMode.SRC_OVER); // Reset blend mode
        graphicsContext.closePath();
        saveCanvasState();
    }


    @FXML
    private void togglePaintMode() {
        paintButton.setDisable(true);
        eraseButton.setDisable(false);
        fillButton.setDisable(false);
        textButton.setDisable(false);
        eyeButton.setDisable(false);

        double opacity = OpacitySlider.getValue();
        Color currentColor = colorPicker.getValue();
        Color newColor = new Color(currentColor.getRed(), currentColor.getGreen(),
                currentColor.getBlue(), opacity);

        graphicsContext.setStroke(newColor);

        canvas.setOnMousePressed(this::MousePressedPrinting);
        canvas.setOnMouseDragged(this::MouseDraggedPrinting);
        canvas.setOnMouseReleased(this::MouseReleasedPrinting);
    }

    @FXML
    private void toggleEraseMode(){
        eraseButton.setDisable(true);
        paintButton.setDisable(false);
        textButton.setDisable(false);
        fillButton.setDisable(false);
        eyeButton.setDisable(false);

        graphicsContext.setStroke(Color.WHITE);
        canvas.setOnMousePressed(mouseEvent -> MousePressedErasing(mouseEvent));
        canvas.setOnMouseDragged(mouseEvent -> MouseDraggedErasing(mouseEvent));
        canvas.setOnMouseReleased(mouseEvent -> MouseReleasedErasing(mouseEvent));
    }

    private void addText(MouseEvent mouseEvent){
        if(mouseEvent.getEventType() == MouseEvent.MOUSE_PRESSED){
            startX = mouseEvent.getX();
            startY = mouseEvent.getY();
        }else if (mouseEvent.getEventType() == MouseEvent.MOUSE_RELEASED){
            TextInputDialog dialog = new TextInputDialog("Enter text");
            dialog.setHeaderText(null);
            dialog.setContentText("Enter the text:");

            dialog.showAndWait().ifPresent(text -> {
                graphicsContext.setFont(javafx.scene.text.Font.font("Verdana", SizeSlider.getValue()));
                graphicsContext.setFill(colorPicker.getValue());
                graphicsContext.fillText(text, startX, startY);
            });
        }
    }


    @FXML
    private void toggleTextMode() {
        textButton.setDisable(true);
        eraseButton.setDisable(false);
        paintButton.setDisable(false);
        fillButton.setDisable(false);
        eyeButton.setDisable(false);

        graphicsContext.setFill(colorPicker.getValue());
        canvas.setOnMousePressed(mouseEvent -> addText(mouseEvent));
        canvas.setOnMouseDragged(mouseEvent -> addText(mouseEvent));
        canvas.setOnMouseReleased(mouseEvent -> addText(mouseEvent));
    }




    @FXML
    private void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(canvas.snapshot(new SnapshotParameters(), null));
            Image lastState = undoStack.pop();
            graphicsContext.drawImage(lastState, 0, 0);
        }
    }
    @FXML
    private void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(canvas.snapshot(new SnapshotParameters(), null));
            Image nextState = redoStack.pop();
            graphicsContext.drawImage(nextState, 0, 0);
        }
    }
    private void setBrushCircle(){
        graphicsContext.setLineCap(StrokeLineCap.ROUND);
        graphicsContext.setLineJoin(StrokeLineJoin.ROUND);
    }
    private void setBrushSquare(){
        graphicsContext.setLineCap(StrokeLineCap.SQUARE);
        graphicsContext.setLineJoin(StrokeLineJoin.BEVEL);
    }

    private void initializeBrushTypeComboBox() {
        ObservableList<String> brushTypes = FXCollections.observableArrayList("Circle", "Square","Triangle");
        brushType.setItems(brushTypes);

        brushType.getSelectionModel().selectFirst();

        brushType.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if ("Circle".equals(newValue)) {
                setBrushCircle();
            } else if ("Square".equals(newValue)) {
                setBrushSquare();
            }else if ("Triangle".equals(newValue)) {
                setBrushTriangle();
            }
        });
    }

    private void setBrushTriangle() {
        graphicsContext.setLineCap(StrokeLineCap.BUTT);
        graphicsContext.setLineJoin(StrokeLineJoin.MITER);
    }


    @FXML
    private void toggleEyeDropper() {
        AtomicBoolean eyeDropperMode = new AtomicBoolean(true);
        eyeButton.setDisable(true);
        textButton.setDisable(false);
        eraseButton.setDisable(false);
        paintButton.setDisable(false);
        fillButton.setDisable(false);

        canvas.setOnMousePressed(mouseEvent -> {
            eyeDropper(mouseEvent);
            eyeDropperMode.set(false);
            canvas.setOnMousePressed(null);
            togglePaintMode();
        });
    }
    private void eyeDropper(MouseEvent e) {
        double x = e.getX();
        double y = e.getY();
        WritableImage writableImage = new WritableImage((int) canvas.getWidth(), (int) canvas.getHeight());
        SnapshotParameters parameters = new SnapshotParameters();
        parameters.setFill(Color.TRANSPARENT);
        canvas.snapshot(parameters, writableImage);
        Color color = writableImage.getPixelReader().getColor((int) x, (int) y);
        colorPicker.setValue(color);
    }

    private void MousePressedDrawingLine(MouseEvent mouseEvent) {
        saveCanvasState();
        lineStartX = mouseEvent.getX();
        lineStartY = mouseEvent.getY();
        graphicsContext.beginPath();
        graphicsContext.moveTo(lineStartX, lineStartY);

        double opacity = OpacitySlider.getValue();
        Color currentColor = colorPicker.getValue();
        Color newColor = new Color(currentColor.getRed(), currentColor.getGreen(),
                currentColor.getBlue(), opacity);

        graphicsContext.setStroke(newColor);
    }

    private void MouseDraggedDrawingLine(MouseEvent mouseEvent) {
        double mouseX = mouseEvent.getX();
        double mouseY = mouseEvent.getY();

        graphicsContext.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        graphicsContext.drawImage(undoStack.peek(), 0, 0);

        graphicsContext.strokeLine(lineStartX, lineStartY, mouseX, mouseY);
    }

    private void MouseReleasedDrawingLine(MouseEvent mouseEvent) {
        double lineEndX = mouseEvent.getX();
        double lineEndY = mouseEvent.getY();

        graphicsContext.strokeLine(lineStartX, lineStartY, lineEndX, lineEndY);
        graphicsContext.closePath();
        saveCanvasState();
    }

    @FXML
    private void toggleLineMode() {
        eraseButton.setDisable(false);
        paintButton.setDisable(false);
        textButton.setDisable(false);
        fillButton.setDisable(false);
        eyeButton.setDisable(false);

        graphicsContext.setStroke(colorPicker.getValue());
        graphicsContext.setLineWidth(SizeSlider.getValue());

        canvas.setOnMousePressed(this::MousePressedDrawingLine);
        canvas.setOnMouseDragged(this::MouseDraggedDrawingLine);
        canvas.setOnMouseReleased(this::MouseReleasedDrawingLine);
    }

}
