package weighttracker.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import weighttracker.Body;
import weighttracker.Exercise;

public class MainController {
    private static class LevelDescriptor {
        final int level;
        final String colorName;
        final String styleClass;
        final String message;

        LevelDescriptor(int level, String colorName, String styleClass, String message) {
            this.level = level;
            this.colorName = colorName;
            this.styleClass = styleClass;
            this.message = message;
        }
    }

    private static class ResultRow {
        final String exercise;
        final int level;
        final String colorName;
        final String message;
        final String detail;
        final String styleClass;

        ResultRow(String exercise, int level, String colorName, String message, String detail, String styleClass) {
            this.exercise = exercise;
            this.level = level;
            this.colorName = colorName;
            this.message = message;
            this.detail = detail;
            this.styleClass = styleClass;
        }
    }

    private static class LevelRow {
        final String exercise;
        final java.util.Map<Integer, String> levelValues;

        LevelRow(String exercise, java.util.Map<Integer, String> levelValues) {
            this.exercise = exercise;
            this.levelValues = levelValues;
        }
    }

    @FXML
    private TextField bodyWeightField;
    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;
    @FXML
    private Label tableLabel;
    @FXML
    private Label languageLabel;
    @FXML
    private Label bodyWeightLabel;
    @FXML
    private TextField squatField;
    @FXML
    private Label squatLabel;
    @FXML
    private TextField benchField;
    @FXML
    private Label benchLabel;
    @FXML
    private TextField cleanField;
    @FXML
    private Label cleanLabel;
    @FXML
    private TextField chinsField;
    @FXML
    private Label chinsLabel;
    @FXML
    private TextField brutalBenchField;
    @FXML
    private Label brutalLabel;
    @FXML
    private TextField dipsField;
    @FXML
    private Label dipsLabel;
    @FXML
    private ComboBox<String> cooperMinutesBox;
    @FXML
    private ComboBox<String> cooperSecondsBox;
    @FXML
    private Label cooperLabel;
    @FXML
    private Button calculateButton;
    @FXML
    private Button clearButton;
    @FXML
    private Button undoClearButton;
    @FXML
    private Label statusLabel;
    @FXML
    private ListView<ResultRow> resultsList;
    @FXML
    private ComboBox<String> genderBox;
    @FXML
    private ComboBox<String> languageBox;
    @FXML
    private Label resultsTitle;
    @FXML
    private Button resultsButton;
    @FXML
    private Button tableButton;
    @FXML
    private Button radarButton;
    @FXML
    private Button weightTableButton;
    @FXML
    private TableView<LevelRow> levelTable;
    @FXML
    private TableView<LevelRow> weightTable;
    @FXML
    private VBox radarPane;
    @FXML
    private Canvas radarCanvas;

    private List<Exercise> exercises;
    private ViewMode viewMode = ViewMode.RESULTS;
    private List<Integer> lastLevels = new ArrayList<>();
    @FXML
    private ResourceBundle resources;
    private ResourceBundle bundle;
    private final Preferences prefs = Preferences.userNodeForPackage(MainController.class);
    private InputsSnapshot lastCleared;
    private boolean updatingLanguage;

    private static class InputsSnapshot {
        final String bodyWeight;
        final String squat;
        final String bench;
        final String clean;
        final String chins;
        final String brutal;
        final String dips;
        final String cooperMinutes;
        final String cooperSeconds;

        InputsSnapshot(String bodyWeight, String squat, String bench, String clean, String chins,
                       String brutal, String dips, String cooperMinutes, String cooperSeconds) {
            this.bodyWeight = bodyWeight;
            this.squat = squat;
            this.bench = bench;
            this.clean = clean;
            this.chins = chins;
            this.brutal = brutal;
            this.dips = dips;
            this.cooperMinutes = cooperMinutes;
            this.cooperSeconds = cooperSeconds;
        }
    }

    private enum ViewMode {
        RESULTS,
        TABLE,
        RADAR,
        WEIGHT_TABLE
    }

    @FXML
    private void initialize() {
        String savedLang = prefs.get("lang", "sv");
        Locale locale = "en".equals(savedLang) ? Locale.ENGLISH : new Locale("sv", "SE");
        bundle = ResourceBundle.getBundle("ui.messages", locale);
        setupLanguageBox();
        applyLanguage();
        int savedGender = prefs.getInt("genderIndex", 0);
        genderBox.getSelectionModel().select(savedGender);
        genderBox.setOnAction(event -> {
            prefs.putInt("genderIndex", genderBox.getSelectionModel().getSelectedIndex());
            refreshExercises();
        });
        refreshExercises();
        setupCooperPickers();
        resultsList.setPlaceholder(new Label(t("placeholder.results")));

        String savedBodyWeight = prefs.get("bodyWeight", "");
        if (!savedBodyWeight.isBlank()) {
            bodyWeightField.setText(savedBodyWeight);
        }
        restoreInputs();
        updateUndoButton();
        resultsList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ResultRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    getStyleClass().removeAll("level-red", "level-yellow", "level-green", "level-blue");
                    return;
                }

                Label title = new Label(item.exercise + " - " + t("label.level") + " " + item.level);
                title.getStyleClass().add("result-title");
                Label color = new Label(item.colorName);
                color.getStyleClass().add("result-color");
                Label message = new Label(item.message);
                message.getStyleClass().add("result-message");
                Label detail = new Label(item.detail);
                detail.getStyleClass().add("result-detail");

                VBox left = new VBox(4, title, message, detail);
                HBox.setHgrow(left, Priority.ALWAYS);

                VBox right = new VBox(4, color);
                right.getStyleClass().add("result-badge");

                HBox wrapper = new HBox(12, left, right);
                wrapper.getStyleClass().add("result-row");

                setGraphic(wrapper);
                getStyleClass().removeAll("level-red", "level-yellow", "level-green", "level-blue");
                getStyleClass().add(item.styleClass);
            }
        });

        levelTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        weightTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        setView(ViewMode.RESULTS);

        radarCanvas.widthProperty().bind(radarPane.widthProperty());
        radarCanvas.heightProperty().bind(radarPane.heightProperty());
        radarCanvas.widthProperty().addListener((obs, oldVal, newVal) -> drawRadar());
        radarCanvas.heightProperty().addListener((obs, oldVal, newVal) -> drawRadar());
    }

    @FXML
    private void onCalculate() {
        statusLabel.setText("");

        refreshExercises();
        boolean anyRatioInput = hasAnyRatioInput();
        Body body = null;
        if (anyRatioInput) {
            Double bodyWeight = parseRequiredDouble(bodyWeightField, t("label.bodyweight"));
            if (bodyWeight == null) {
                return;
            }
            if (bodyWeight <= 0) {
                showStatus(t("status.bodyweight.invalid"));
                return;
            }
            prefs.put("bodyWeight", formatNumber(bodyWeight));
            body = new Body(bodyWeight);
        }
        saveInputs();

        List<ResultRow> results = new ArrayList<>();
        List<Integer> levelsByExercise = new ArrayList<>();
        for (Exercise exercise : exercises) {
            Double value = readExerciseValue(exercise);
            if (value == null) {
                levelsByExercise.add(0);
                continue;
            }
            if (value.isNaN()) {
                return;
            }

            if (exercise.getType() == Exercise.Type.BODYWEIGHT_RATIO && body == null) {
                showStatus(t("status.bodyweight.required"));
                return;
            }
            int level = computeLevel(body, exercise, value);
            levelsByExercise.add(level);
            LevelDescriptor descriptor = levelDescriptor(level);
            String detail = buildDetail(exercise, body, value, level);

            results.add(new ResultRow(
                    exercise.getName(),
                    level,
                    descriptor.colorName,
                    descriptor.message,
                    detail,
                    descriptor.styleClass
            ));
        }

        if (results.isEmpty()) {
            showStatus(t("status.fill.one"));
            resultsList.setItems(FXCollections.observableArrayList());
            lastLevels = new ArrayList<>();
            drawRadar();
            return;
        }

        ObservableList<ResultRow> items = FXCollections.observableArrayList(results);
        resultsList.setItems(items);

        lastLevels = levelsByExercise;
        refreshWeightTable(body != null ? body.getWeightKg() : null);
        drawRadar();
    }

    @FXML
    private void onShowResults() {
        setView(ViewMode.RESULTS);
    }

    @FXML
    private void onShowTable() {
        setView(ViewMode.TABLE);
    }

    @FXML
    private void onShowRadar() {
        setView(ViewMode.RADAR);
    }

    @FXML
    private void onShowWeightTable() {
        Double bodyWeight = parseOptionalDouble(bodyWeightField, t("label.bodyweight"));
        if (bodyWeight != null && bodyWeight.isNaN()) {
            return;
        }
        if (bodyWeight == null) {
            showStatus(t("status.weighttable.bodyweight"));
        }
        refreshWeightTable(bodyWeight);
        setView(ViewMode.WEIGHT_TABLE);
    }

    @FXML
    private void onClearFields() {
        lastCleared = captureInputs();
        bodyWeightField.clear();
        squatField.clear();
        benchField.clear();
        cleanField.clear();
        chinsField.clear();
        brutalBenchField.clear();
        dipsField.clear();
        cooperMinutesBox.getSelectionModel().select("—");
        cooperSecondsBox.getSelectionModel().select("—");
        statusLabel.setText("");
        resultsList.setItems(FXCollections.observableArrayList());
        lastLevels = new ArrayList<>();
        drawRadar();
        saveInputs();
        updateUndoButton();
    }

    @FXML
    private void onUndoClear() {
        if (lastCleared == null) {
            return;
        }
        bodyWeightField.setText(lastCleared.bodyWeight);
        squatField.setText(lastCleared.squat);
        benchField.setText(lastCleared.bench);
        cleanField.setText(lastCleared.clean);
        chinsField.setText(lastCleared.chins);
        brutalBenchField.setText(lastCleared.brutal);
        dipsField.setText(lastCleared.dips);
        restoreCooperSelection(lastCleared.cooperMinutes, lastCleared.cooperSeconds);
        statusLabel.setText("");
        saveInputs();
        updateUndoButton();
    }

    private List<Exercise> buildExercises(boolean isFemale) {
        List<Exercise> list = new ArrayList<>();
        if (isFemale) {
            list.add(Exercise.ratioExercise("squat", t("exercise.squat"), new double[]{
                    0.75, 0.84, 0.94, 1.03, 1.13, 1.22, 1.32, 1.41, 1.51, 1.60
            }));
            list.add(Exercise.ratioExercise("bench", t("exercise.bench"), new double[]{
                    0.50, 0.58, 0.67, 0.75, 0.83, 0.92, 1.00, 1.08, 1.17, 1.25
            }));
            list.add(Exercise.ratioExercise("clean", t("exercise.clean"), new double[]{
                    0.50, 0.58, 0.67, 0.75, 0.83, 0.92, 1.00, 1.08, 1.17, 1.25
            }));
            list.add(Exercise.repExercise("chins", t("exercise.chins"), new int[]{
                    1, 2, 3, 4, 5, 6, 7, 8, 9, 10
            }));
            list.add(Exercise.repExercise("brutal", t("exercise.brutal"), new int[]{
                    3, 6, 10, 10, 13, 17, 20, 23, 27, 30
            }));
            list.add(Exercise.repExercise("dips", t("exercise.dips"), new int[]{
                    2, 4, 6, 8, 10, 12, 14, 16, 18, 20
            }));
            list.add(Exercise.timeExercise("cooper", t("exercise.cooper"), new double[]{
                    14 * 60 + 10, 13 * 60 + 50, 13 * 60 + 30, 13 * 60 + 10, 12 * 60 + 50,
                    12 * 60 + 30, 12 * 60 + 10, 11 * 60 + 50, 11 * 60 + 30, 11 * 60 + 10
            }));
        } else {
            list.add(Exercise.ratioExercise("squat", t("exercise.squat"), new double[]{
                    1.10, 1.22, 1.33, 1.44, 1.55, 1.66, 1.77, 1.88, 1.99, 2.10
            }));
            list.add(Exercise.ratioExercise("bench", t("exercise.bench"), new double[]{
                    0.75, 0.82, 0.89, 0.96, 1.04, 1.11, 1.18, 1.25, 1.33, 1.40
            }));
            list.add(Exercise.ratioExercise("clean", t("exercise.clean"), new double[]{
                    0.75, 0.82, 0.89, 0.96, 1.04, 1.11, 1.18, 1.25, 1.33, 1.40
            }));
            list.add(Exercise.repExercise("chins", t("exercise.chins"), new int[]{
                    2, 4, 6, 8, 10, 12, 14, 16, 18, 20
            }));
            list.add(Exercise.repExercise("brutal", t("exercise.brutal"), new int[]{
                    3, 6, 9, 10, 13, 16, 20, 23, 26, 30
            }));
            list.add(Exercise.repExercise("dips", t("exercise.dips"), new int[]{
                    3, 6, 9, 10, 13, 16, 20, 23, 26, 30
            }));
            list.add(Exercise.timeExercise("cooper", t("exercise.cooper"), new double[]{
                    13 * 60 + 15, 12 * 60 + 58, 12 * 60 + 41, 12 * 60 + 24, 12 * 60 + 7,
                    11 * 60 + 50, 11 * 60 + 33, 11 * 60 + 17, 11 * 60 + 1, 10 * 60 + 45
            }));
        }

        return list;
    }

    private void refreshExercises() {
        boolean isFemale = genderBox.getSelectionModel().getSelectedIndex() == 1;
        exercises = buildExercises(isFemale);
        buildLevelTableColumns();
        levelTable.setItems(buildLevelTableRows());
        weightTable.getColumns().clear();
        weightTable.setItems(FXCollections.observableArrayList());
        lastLevels = new ArrayList<>();
        drawRadar();
    }

    private void buildLevelTableColumns() {
        levelTable.getColumns().clear();

        javafx.scene.control.TableColumn<LevelRow, String> exerciseColumn =
                new javafx.scene.control.TableColumn<>(t("label.exercise"));
        exerciseColumn.setPrefWidth(170);
        exerciseColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().exercise));
        levelTable.getColumns().add(exerciseColumn);

        int maxLevels = exercises.stream()
                .mapToInt(Exercise::getLevelCount)
                .max()
                .orElse(0);
        for (int level = 1; level <= maxLevels; level++) {
            int currentLevel = level;
            javafx.scene.control.TableColumn<LevelRow, String> column =
                    new javafx.scene.control.TableColumn<>(String.valueOf(level));
            column.setPrefWidth(70);
            column.setCellValueFactory(data -> {
                String value = data.getValue().levelValues.getOrDefault(currentLevel, "-");
                return new ReadOnlyStringWrapper(value);
            });
            column.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    getStyleClass().removeAll("level-red", "level-yellow", "level-green", "level-blue");
                    if (empty || item == null) {
                        setText(null);
                        return;
                    }
                    setText(item);
                    LevelDescriptor descriptor = levelDescriptor(currentLevel);
                    getStyleClass().add(descriptor.styleClass);
                }
            });
            levelTable.getColumns().add(column);
        }
    }

    private ObservableList<LevelRow> buildLevelTableRows() {
        List<LevelRow> rows = new ArrayList<>();
        for (Exercise exercise : exercises) {
            java.util.Map<Integer, String> values = new java.util.LinkedHashMap<>();
            int count = exercise.getLevelCount();
            for (int level = 1; level <= count; level++) {
                double threshold = exercise.thresholdForLevel(level);
                if (exercise.getType() == Exercise.Type.BODYWEIGHT_RATIO) {
                    values.put(level, String.format(Locale.US, "%.2f", threshold));
                } else if (exercise.getType() == Exercise.Type.TIME_LOWER_BETTER) {
                    values.put(level, formatTimeSeconds(threshold));
                } else {
                    values.put(level, String.format(Locale.US, "%.0f", threshold));
                }
            }
            String label = exercise.getName();
            if (exercise.getType() == Exercise.Type.BODYWEIGHT_RATIO) {
                label = label + " " + t("suffix.bw");
            } else if (exercise.getType() == Exercise.Type.TIME_LOWER_BETTER) {
                label = label + " " + t("suffix.min");
            } else {
                label = label + " " + t("suffix.reps");
            }
            rows.add(new LevelRow(label, values));
        }
        return FXCollections.observableArrayList(rows);
    }

    private void buildWeightTableColumns() {
        weightTable.getColumns().clear();

        javafx.scene.control.TableColumn<LevelRow, String> exerciseColumn =
                new javafx.scene.control.TableColumn<>(t("label.exercise"));
        exerciseColumn.setPrefWidth(170);
        exerciseColumn.setCellValueFactory(data -> new ReadOnlyStringWrapper(data.getValue().exercise));
        weightTable.getColumns().add(exerciseColumn);

        int maxLevels = exercises.stream()
                .mapToInt(Exercise::getLevelCount)
                .max()
                .orElse(0);
        for (int level = 1; level <= maxLevels; level++) {
            int currentLevel = level;
            javafx.scene.control.TableColumn<LevelRow, String> column =
                    new javafx.scene.control.TableColumn<>(String.valueOf(level));
            column.setPrefWidth(70);
            column.setCellValueFactory(data -> {
                String value = data.getValue().levelValues.getOrDefault(currentLevel, "-");
                return new ReadOnlyStringWrapper(value);
            });
            weightTable.getColumns().add(column);
        }
    }

    private void refreshWeightTable(Double bodyWeight) {
        buildWeightTableColumns();
        weightTable.setItems(buildWeightTableRows(bodyWeight));
    }

    private ObservableList<LevelRow> buildWeightTableRows(Double bodyWeight) {
        List<LevelRow> rows = new ArrayList<>();
        for (Exercise exercise : exercises) {
            java.util.Map<Integer, String> values = new java.util.LinkedHashMap<>();
            int count = exercise.getLevelCount();
            for (int level = 1; level <= count; level++) {
                double threshold = exercise.thresholdForLevel(level);
                if (exercise.getType() == Exercise.Type.BODYWEIGHT_RATIO) {
                    if (bodyWeight == null || bodyWeight.isNaN()) {
                        values.put(level, "-");
                    } else {
                        double kg = threshold * bodyWeight;
                        values.put(level, String.format(Locale.US, "%.1f", kg));
                    }
                } else if (exercise.getType() == Exercise.Type.TIME_LOWER_BETTER) {
                    values.put(level, formatTimeSeconds(threshold));
                } else {
                    values.put(level, String.format(Locale.US, "%.0f", threshold));
                }
            }
            String label = exercise.getName();
            if (exercise.getType() == Exercise.Type.BODYWEIGHT_RATIO) {
                label = label + " " + t("suffix.kg");
            } else if (exercise.getType() == Exercise.Type.TIME_LOWER_BETTER) {
                label = label + " " + t("suffix.min");
            } else {
                label = label + " " + t("suffix.reps");
            }
            rows.add(new LevelRow(label, values));
        }
        return FXCollections.observableArrayList(rows);
    }

    private void setView(ViewMode mode) {
        viewMode = mode;
        levelTable.setVisible(mode == ViewMode.TABLE);
        levelTable.setManaged(mode == ViewMode.TABLE);
        weightTable.setVisible(mode == ViewMode.WEIGHT_TABLE);
        weightTable.setManaged(mode == ViewMode.WEIGHT_TABLE);
        resultsList.setVisible(mode == ViewMode.RESULTS);
        resultsList.setManaged(mode == ViewMode.RESULTS);
        radarPane.setVisible(mode == ViewMode.RADAR);
        radarPane.setManaged(mode == ViewMode.RADAR);
        resultsTitle.setText(mode == ViewMode.RESULTS
                ? t("results.title")
                : mode == ViewMode.TABLE
                ? t("results.table.title")
                : mode == ViewMode.WEIGHT_TABLE
                ? t("results.alllevels.title")
                : t("results.radar.title"));
        resultsButton.setDisable(mode == ViewMode.RESULTS);
        tableButton.setDisable(mode == ViewMode.TABLE);
        radarButton.setDisable(mode == ViewMode.RADAR);
        weightTableButton.setDisable(mode == ViewMode.WEIGHT_TABLE);
        drawRadar();
    }

    private Double readExerciseValue(Exercise exercise) {
        if (exercise.getId().equals("squat")) {
            return parseOptionalDouble(squatField, t("label.squat"));
        }
        if (exercise.getId().equals("bench")) {
            return parseOptionalDouble(benchField, t("label.bench"));
        }
        if (exercise.getId().equals("clean")) {
            return parseOptionalDouble(cleanField, t("label.clean"));
        }
        if (exercise.getId().equals("chins")) {
            return parseOptionalDouble(chinsField, t("label.chins"));
        }
        if (exercise.getId().equals("brutal")) {
            return parseOptionalDouble(brutalBenchField, t("label.brutal"));
        }
        if (exercise.getId().equals("dips")) {
            return parseOptionalDouble(dipsField, t("label.dips"));
        }
        if (exercise.getId().equals("cooper")) {
            return readCooperSeconds();
        }
        return null;
    }

    private int computeLevel(Body body, Exercise exercise, double value) {
        if (exercise.getType() == Exercise.Type.BODYWEIGHT_RATIO) {
            if (body == null) {
                return 1;
            }
            double ratio = value / body.getWeightKg();
            return exercise.levelFor(ratio);
        }
        return exercise.levelFor(value);
    }

    private String buildDetail(Exercise exercise, Body body, double value, int level) {
        if (exercise.getType() == Exercise.Type.BODYWEIGHT_RATIO) {
            double ratio = value / body.getWeightKg();
            String valueStr = String.format(Locale.US, "%.1f", value);
            String ratioStr = String.format(Locale.US, "%.2f", ratio);
            String thresholdStr = String.format(Locale.US, "%.2f", exercise.thresholdForLevel(level));
            return fmt("format.detail.ratio", valueStr, ratioStr, thresholdStr);
        }
        if (exercise.getType() == Exercise.Type.TIME_LOWER_BETTER) {
            return fmt("format.detail.time", formatTimeSeconds(value), formatTimeSeconds(exercise.thresholdForLevel(level)));
        }
        String repsStr = String.format(Locale.US, "%.0f", value);
        String thresholdRepsStr = String.format(Locale.US, "%.0f", exercise.thresholdForLevel(level));
        return fmt("format.detail.reps", repsStr, thresholdRepsStr);
    }

    private void drawRadar() {
        if (radarCanvas == null || exercises == null || viewMode != ViewMode.RADAR) {
            return;
        }

        double width = radarCanvas.getWidth();
        double height = radarCanvas.getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        GraphicsContext gc = radarCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, width, height);
        gc.setFont(javafx.scene.text.Font.font("Avenir Next", 12));

        double padding = 40;
        double radius = Math.min(width, height) / 2.0 - padding;
        double centerX = width / 2.0;
        double centerY = height / 2.0;

        gc.setStroke(Color.rgb(60, 70, 90, 0.25));
        gc.setLineWidth(1.0);
        for (int ring = 1; ring <= 10; ring++) {
            double r = radius * ring / 10.0;
            gc.strokeOval(centerX - r, centerY - r, r * 2, r * 2);
        }

        int count = exercises.size();
        if (count == 0) {
            return;
        }
        double angleStep = 2 * Math.PI / count;
        gc.setFill(Color.rgb(22, 27, 34, 0.92));
        for (int i = 0; i < count; i++) {
            double angle = -Math.PI / 2 + angleStep * i;
            double x = centerX + Math.cos(angle) * radius;
            double y = centerY + Math.sin(angle) * radius;
            gc.strokeLine(centerX, centerY, x, y);

            String label = exercises.get(i).getName();
            double labelX = x + (x > centerX ? 8 : -8);
            double labelY = y + (y > centerY ? 14 : -6);
            gc.fillText(label, labelX - (label.length() * 3.0), labelY);
        }

        if (lastLevels == null || lastLevels.isEmpty()) {
            return;
        }

        gc.setStroke(Color.rgb(31, 58, 147, 0.75));
        gc.setLineWidth(2.0);
        gc.setFill(Color.rgb(31, 58, 147, 0.25));

        double[] xs = new double[count];
        double[] ys = new double[count];
        for (int i = 0; i < count; i++) {
            int level = i < lastLevels.size() ? lastLevels.get(i) : 0;
            double value = Math.max(0, Math.min(10, level));
            double angle = -Math.PI / 2 + angleStep * i;
            double r = radius * value / 10.0;
            xs[i] = centerX + Math.cos(angle) * r;
            ys[i] = centerY + Math.sin(angle) * r;
        }
        gc.strokePolygon(xs, ys, count);
        gc.fillPolygon(xs, ys, count);

        gc.setFill(Color.rgb(31, 58, 147, 0.95));
        for (int i = 0; i < count; i++) {
            gc.fillOval(xs[i] - 4, ys[i] - 4, 8, 8);
        }

        gc.setFill(Color.rgb(22, 27, 34, 0.95));
        for (int i = 0; i < count; i++) {
            int level = i < lastLevels.size() ? lastLevels.get(i) : 0;
            String label = String.valueOf(level);
            double labelX = xs[i] + (xs[i] > centerX ? 8 : -8);
            double labelY = ys[i] + (ys[i] > centerY ? 14 : -8);
            gc.fillText(label, labelX, labelY);
        }
    }

    private LevelDescriptor levelDescriptor(int level) {
        String message = t("level.msg." + level);
        if (level <= 4) {
            return new LevelDescriptor(level, t("color.red"), "level-red", message);
        }
        if (level <= 6) {
            return new LevelDescriptor(level, t("color.yellow"), "level-yellow", message);
        }
        if (level <= 8) {
            return new LevelDescriptor(level, t("color.green"), "level-green", message);
        }
        return new LevelDescriptor(level, t("color.blue"), "level-blue", message);
    }

    private Double parseRequiredDouble(TextField field, String label) {
        String raw = field.getText().trim().replace(',', '.');
        if (raw.isEmpty()) {
            showStatus(fmt("status.fill.field", label));
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            showStatus(fmt("status.invalid.number", label));
            return null;
        }
    }

    private Double parseOptionalDouble(TextField field, String label) {
        String raw = field.getText().trim().replace(',', '.');
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException ex) {
            showStatus(fmt("status.invalid.number", label));
            return Double.NaN;
        }
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    private String formatTimeSeconds(double seconds) {
        int totalSeconds = (int) Math.round(seconds);
        int minutes = totalSeconds / 60;
        int secs = totalSeconds % 60;
        return String.format(Locale.US, "%d:%02d", minutes, secs);
    }

    private String formatNumber(double value) {
        return String.format(Locale.US, "%.2f", value).replace('.', ',');
    }

    private void setupCooperPickers() {
        ObservableList<String> minutes = FXCollections.observableArrayList();
        minutes.add("—");
        for (int i = 1; i <= 59; i++) {
            minutes.add(String.valueOf(i));
        }
        cooperMinutesBox.setItems(minutes);
        cooperMinutesBox.getSelectionModel().select("—");

        ObservableList<String> seconds = FXCollections.observableArrayList();
        seconds.add("—");
        for (int i = 1; i <= 59; i++) {
            seconds.add(String.format(Locale.US, "%02d", i));
        }
        cooperSecondsBox.setItems(seconds);
        cooperSecondsBox.getSelectionModel().select("—");
    }

    private void setupLanguageBox() {
        languageBox.setItems(FXCollections.observableArrayList(t("language.sv"), t("language.en")));
        int initialIndex = "en".equals(bundle.getLocale().getLanguage()) ? 1 : 0;
        languageBox.getSelectionModel().select(initialIndex);
        languageBox.setOnAction(event -> {
            if (updatingLanguage) {
                return;
            }
            int index = languageBox.getSelectionModel().getSelectedIndex();
            Locale locale = index == 1 ? Locale.ENGLISH : new Locale("sv", "SE");
            if (!locale.getLanguage().equals(bundle.getLocale().getLanguage())) {
                bundle = ResourceBundle.getBundle("ui.messages", locale);
                prefs.put("lang", locale.getLanguage());
                applyLanguage();
                refreshExercises();
                resultsList.setPlaceholder(new Label(t("placeholder.results")));
            }
        });
    }

    private void applyLanguage() {
        updatingLanguage = true;
        int genderIndex = genderBox.getSelectionModel().getSelectedIndex();
        genderBox.getItems().setAll(t("gender.male"), t("gender.female"));
        genderBox.getSelectionModel().select(Math.max(genderIndex, 0));

        int languageIndex = "en".equals(bundle.getLocale().getLanguage()) ? 1 : 0;
        languageBox.getItems().setAll(t("language.sv"), t("language.en"));
        languageBox.getSelectionModel().select(languageIndex);

        titleLabel.setText(t("app.title"));
        subtitleLabel.setText(t("app.subtitle"));
        tableLabel.setText(t("label.table"));
        languageLabel.setText(t("label.language"));
        bodyWeightLabel.setText(t("label.bodyweight"));
        squatLabel.setText(t("label.squat"));
        benchLabel.setText(t("label.bench"));
        cleanLabel.setText(t("label.clean"));
        chinsLabel.setText(t("label.chins"));
        brutalLabel.setText(t("label.brutal"));
        dipsLabel.setText(t("label.dips"));
        cooperLabel.setText(t("label.cooper"));

        bodyWeightField.setPromptText(t("prompt.bodyweight"));
        squatField.setPromptText(t("prompt.squat"));
        benchField.setPromptText(t("prompt.bench"));
        cleanField.setPromptText(t("prompt.clean"));
        chinsField.setPromptText(t("prompt.chins"));
        brutalBenchField.setPromptText(t("prompt.brutal"));
        dipsField.setPromptText(t("prompt.dips"));

        calculateButton.setText(t("button.calculate"));
        clearButton.setText(t("button.clear"));
        undoClearButton.setText(t("button.undo"));
        resultsButton.setText(t("button.results"));
        tableButton.setText(t("button.table"));
        radarButton.setText(t("button.radar"));
        weightTableButton.setText(t("button.alllevels"));

        resultsTitle.setText(viewMode == ViewMode.RESULTS
                ? t("results.title")
                : viewMode == ViewMode.TABLE
                ? t("results.table.title")
                : viewMode == ViewMode.WEIGHT_TABLE
                ? t("results.alllevels.title")
                : t("results.radar.title"));

        resultsList.setPlaceholder(new Label(t("placeholder.results")));
        updatingLanguage = false;
    }

    private String t(String key) {
        return bundle.getString(key);
    }

    private String fmt(String key, Object... args) {
        return MessageFormat.format(t(key), args);
    }

    private Double readCooperSeconds() {
        String minutesValue = cooperMinutesBox.getSelectionModel().getSelectedItem();
        String secondsValue = cooperSecondsBox.getSelectionModel().getSelectedItem();
        boolean minutesEmpty = minutesValue == null || "—".equals(minutesValue);
        boolean secondsEmpty = secondsValue == null || "—".equals(secondsValue);
        if (minutesEmpty && secondsEmpty) {
            return null;
        }
        if (minutesEmpty || secondsEmpty) {
            showStatus(t("status.cooper.pick"));
            return Double.NaN;
        }
        int minutes = Integer.parseInt(minutesValue);
        int seconds = Integer.parseInt(secondsValue);
        return minutes * 60.0 + seconds;
    }

    private void saveInputs() {
        saveField("squat", squatField);
        saveField("bench", benchField);
        saveField("clean", cleanField);
        saveField("chins", chinsField);
        saveField("brutal", brutalBenchField);
        saveField("dips", dipsField);
        prefs.put("cooperMinutes", valueOrEmpty(cooperMinutesBox.getSelectionModel().getSelectedItem()));
        prefs.put("cooperSeconds", valueOrEmpty(cooperSecondsBox.getSelectionModel().getSelectedItem()));
        prefs.put("bodyWeight", bodyWeightField.getText().trim());
    }

    private void restoreInputs() {
        restoreField("squat", squatField);
        restoreField("bench", benchField);
        restoreField("clean", cleanField);
        restoreField("chins", chinsField);
        restoreField("brutal", brutalBenchField);
        restoreField("dips", dipsField);
        String savedMinutes = prefs.get("cooperMinutes", "—");
        String savedSeconds = prefs.get("cooperSeconds", "—");
        restoreCooperSelection(savedMinutes, savedSeconds);
    }

    private void saveField(String key, TextField field) {
        prefs.put(key, field.getText().trim());
    }

    private void restoreField(String key, TextField field) {
        String value = prefs.get(key, "");
        if (!value.isBlank()) {
            field.setText(value);
        }
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    private void restoreCooperSelection(String minutes, String seconds) {
        if (cooperMinutesBox.getItems().contains(minutes)) {
            cooperMinutesBox.getSelectionModel().select(minutes);
        }
        if (cooperSecondsBox.getItems().contains(seconds)) {
            cooperSecondsBox.getSelectionModel().select(seconds);
        }
    }

    private InputsSnapshot captureInputs() {
        return new InputsSnapshot(
                bodyWeightField.getText(),
                squatField.getText(),
                benchField.getText(),
                cleanField.getText(),
                chinsField.getText(),
                brutalBenchField.getText(),
                dipsField.getText(),
                valueOrEmpty(cooperMinutesBox.getSelectionModel().getSelectedItem()),
                valueOrEmpty(cooperSecondsBox.getSelectionModel().getSelectedItem())
        );
    }

    private void updateUndoButton() {
        boolean hasUndo = lastCleared != null;
        undoClearButton.setVisible(hasUndo);
        undoClearButton.setManaged(hasUndo);
    }

    private boolean hasAnyRatioInput() {
        return hasValue(squatField) || hasValue(benchField) || hasValue(cleanField);
    }

    private boolean hasValue(TextField field) {
        return !field.getText().trim().isEmpty();
    }
}
