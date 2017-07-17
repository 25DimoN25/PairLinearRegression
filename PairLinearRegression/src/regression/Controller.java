package regression;

import com.sun.javafx.charts.Legend;
import javafx.beans.*;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.IntStream;

public class Controller implements Initializable {
	private PairLinearRegressionCalculator calc;
	private double xMax;
	private double xMin;
	private double xLength;
	private int n;


	@FXML private LineChart<Double, Double> plot;

	@FXML private Label equation;
	@FXML private Label S2rem;
	@FXML private Label ma;
	@FXML private Label mb;
	@FXML private Label S2x;
	@FXML private Label S2y;
	@FXML private Label rxy;
	@FXML private Label R2;
	@FXML private Label avgA;

	@FXML private TextField xPrognoses;
	@FXML private TextField prognosesAlpha;
	@FXML private Label prognoses;

	@FXML private TextField intervalCoefficientsAlpha;
	@FXML private Label intervalA;
	@FXML private Label intervalB;

	@FXML private TextField studAlpha;
	@FXML private Label studTaCalc;
	@FXML private Label studTbCalc;
	@FXML private Label studTrCalc;
	@FXML private Label studTTable;
	@FXML private Label studIsValidA;
	@FXML private Label studIsValidB;
	@FXML private Label studIsValidFull;


	@FXML private TextField fishAlpha;
	@FXML private Label fishFCalc;
	@FXML private Label fishFTable;
	@FXML private Label fishIsValidFull;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		Properties properties = readData();

		plot.getXAxis().setLabel(properties.getProperty("xTitle", "'xTitle' property key"));
		plot.getYAxis().setLabel(properties.getProperty("yTitle", "'yTitle' property key"));
		plot.setTitle(properties.getProperty("plotTitle", "'plotTitle' property key"));
		double[] x = convertStringToDoubleArray(properties.getProperty("xValues", "0, 1, 2"));
		double[] y = convertStringToDoubleArray(properties.getProperty("yValues", "1, 3, 5"));

		xMax = Arrays.stream(x).max().getAsDouble();
		xMin = Arrays.stream(x).min().getAsDouble();
		xLength = xMax - xMin;
		n = IntStream.of(x.length, y.length).min().getAsInt();

		calc = new PairLinearRegressionCalculator(x, y, n);
		equation.setText(String.format("%.3f + %.3fx ", calc.getA(), calc.getB()));
		S2rem.setText(String.format("%.3f", calc.getSRemSqr()));
		S2x.setText(String.format("%.3f", calc.getSXSqr()));
		S2y.setText(String.format("%.3f", calc.getSYSqr()));
		ma.setText(String.format("%.3f", calc.getMa()));
		mb.setText(String.format("%.3f", calc.getMb()));
		rxy.setText(String.format("%.3f", calc.getRxy()));
		R2.setText(String.format("%.3f", calc.getRSqr()));
		avgA.setText(String.format("%.3f%%", calc.getAvgA()));

		XYChart.Series<Double, Double> source = new XYChart.Series<>();
		source.setName("Исходные данные");
		for (int i = 0; i < n; i++) {
			source.getData().add(new XYChart.Data<>(x[i], y[i]));
		}

		XYChart.Series<Double, Double> model = new XYChart.Series<>();
		model.setName("Регрессионная модель");
		for (double X = xMin; X <= xMax; X += xLength) {
			model.getData().add(new XYChart.Data<>(X, calc.Y(X)));
		}

		XYChart.Series<Double, Double> prognoses = new XYChart.Series<>();
		prognoses.setName("Прогнозируемое значение");

		XYChart.Series<Double, Double> prognosesInterval = new XYChart.Series<>();
		prognosesInterval.setName("Дов. интервал прогноза");

		XYChart.Series<Double, Double> aInter1 = new XYChart.Series<>();
		XYChart.Series<Double, Double> aInter2 = new XYChart.Series<>();
		aInter1.setName("Дов. интервалы a");

		XYChart.Series<Double, Double> bInter1 = new XYChart.Series<>();
		XYChart.Series<Double, Double> bInter2 = new XYChart.Series<>();
		bInter1.setName("Дов. интервалы b");

		plot.getData().addAll(
				source, model,
				prognoses, prognosesInterval,
				aInter1, aInter2,
				bInter1, bInter2);

		Legend legend = (Legend) plot.lookup(".chart-legend");
		legend.getItems().removeIf(item -> item.getText() == null);


		source.getData().forEach(point ->
			setToolTip(point.getNode(), '(' + point.getXValue().toString() + "; " + point.getYValue().toString() + ')')
		);

		Arrays.asList(prognoses, prognosesInterval).forEach(series ->
			series.getData().addListener((ListChangeListener<XYChart.Data<Double, Double>>) c ->
				c.getList().forEach(point ->
					setToolTip(point.getNode(), '(' + point.getXValue().toString() + "; " + point.getYValue().toString() + ')')
				)
			)
		);
	}

	@FXML
	private void calcPrognoses(ActionEvent e) {
		double x;
		double alpha;

		try {
			x = Double.valueOf(xPrognoses.getText());
		} catch (Exception exception) {
			showErrorToolTip(xPrognoses, "Некорректные данные");
			return;
		}

		try {
			alpha = Double.valueOf(prognosesAlpha.getText());
		} catch (Exception exception) {
			showErrorToolTip(prognosesAlpha, "Некорректные данные");
			return;
		}

		if (alpha <= 0 || alpha >= 1) {
			showErrorToolTip(prognosesAlpha, "Значение должно находиться между 0 и 1");
			return;
		}

		double prognosesOffset = calc.calcStandardPrognosesError(x, alpha);


		ObservableList<XYChart.Data<Double, Double>> prognosesData = plot.getData().get(2).getData();
		ObservableList<XYChart.Data<Double, Double>> intervalData = plot.getData().get(3).getData();

		prognosesData.clear();
		prognosesData.add(new XYChart.Data<>(x, calc.Y(x)));
		intervalData.clear();
		intervalData.add(new XYChart.Data<>(x, calc.Y(x) - prognosesOffset));
		intervalData.add(new XYChart.Data<>(x, calc.Y(x) + prognosesOffset));

		prognoses.setText(String.format(
				"Yпр(Xпр = %.3f) = %.3f%n" +
				"Yпр ∈ (%.3f ; %.3f) c вер-тью %.1f%% %n",
				x, calc.Y(x),
				calc.Y(x) - prognosesOffset, calc.Y(x) + prognosesOffset, (1 - alpha) * 100));
	}


	@FXML
	private void calcIntervals(ActionEvent e) {
		double alpha;

		try {
			alpha = Double.valueOf(intervalCoefficientsAlpha.getText());
		} catch (Exception exception) {
			showErrorToolTip(intervalCoefficientsAlpha, "Некорректные данные");
			return;
		}

		if (alpha <= 0 || alpha >= 1) {
			showErrorToolTip(intervalCoefficientsAlpha, "Значение должно находиться между 0 и 1");
			return;
		}

		double aOffset = calc.calcCoefficientAOffset(alpha);
		double bOffset = calc.calcCoefficientBOffset(alpha);

		ObservableList<XYChart.Data<Double, Double>> dataA1 = plot.getData().get(4).getData();
		dataA1.clear();
		for (double X = xMin; X <= xMax; X += xLength) {
			dataA1.add(new XYChart.Data<>(X, (calc.getA() - aOffset) + calc.getB() * X));
		}

		ObservableList<XYChart.Data<Double, Double>> dataA2 = plot.getData().get(5).getData();
		dataA2.clear();
		for (double X = xMin; X <= xMax; X += xLength) {
			dataA2.add(new XYChart.Data<>(X, (calc.getA() + aOffset) + calc.getB() * X));
		}

		ObservableList<XYChart.Data<Double, Double>> dataB1 = plot.getData().get(6).getData();
		dataB1.clear();
		for (double X = xMin; X <= xMax; X += xLength) {
			dataB1.add(new XYChart.Data<>(X, calc.getA() + (calc.getB() - bOffset) * X));
		}

		ObservableList<XYChart.Data<Double, Double>> dataB2 = plot.getData().get(7).getData();
		dataB2.clear();
		for (double X = xMin; X <= xMax; X += xLength) {
			dataB2.add(new XYChart.Data<>(X, calc.getA() + (calc.getB() + bOffset) * X));
		}

		intervalA.setText(String.format("(%.3f ; %.3f)", calc.getA() - aOffset, calc.getA() + aOffset));
		intervalB.setText(String.format("(%.3f ; %.3f)", calc.getB() - bOffset, calc.getB() + bOffset));

	}

    @FXML
    private void tCriterion(ActionEvent e) {
		double alpha;

		try {
			alpha = Double.valueOf(studAlpha.getText());
		} catch (Exception exception) {
			showErrorToolTip(studAlpha, "Некорректные данные");
			return;
		}

		if (alpha <= 0 || alpha >= 1) {
			showErrorToolTip(studAlpha, "Значение должно находиться между 0 и 1");
			return;
		}

		double ta = calc.getTa();
		double tb = calc.getTb();
		double tr = calc.getTr();
		double ttable = calc.calcTTable(alpha);

		studTaCalc.setText(String.format("%.3f", ta));
		studTbCalc.setText(String.format("%.3f", tb));
		studTrCalc.setText(String.format("%.3f", tr));
		studTTable.setText(String.format("%.3f", ttable));
		studIsValidA.setText(Math.abs(ta) >= ttable ? "коэф-ент значим" : "коэф-ент незначим");
		studIsValidB.setText(Math.abs(tb) >= ttable ? "коэф-ент значим" : "коэф-ент незначим");
		studIsValidFull.setText(Math.abs(tr) >= ttable ? "уравнение значимо" : "уравнение незначимо");
    }

    @FXML
    private void fCriterion(ActionEvent e) {
		double alpha;

		try {
			alpha = Double.valueOf(fishAlpha.getText());
		} catch (Exception exception) {
			showErrorToolTip(fishAlpha, "Некорректные данные");
			return;
		}

		if (alpha <= 0 || alpha >= 1) {
			showErrorToolTip(fishAlpha, "Значение должно находиться между 0 и 1");
			return;
		}

		double f = calc.getF();
		double ftable = calc.calcFTable(alpha);

		fishFCalc.setText(String.format("%.3f", f));
		fishFTable.setText(String.format("%.3f", ftable));
		fishIsValidFull.setText(f >= ftable ? "уравнение значимо" : "уравнение незначимо");
    }

	/**
	 * Установка тултипа на ноуд, показывающегося при наведении мышью
	 * @param node
	 * @param text
	 */
	private void setToolTip(Node node, String text) {
		Tooltip tooltip = new Tooltip(text);
		node.setOnMouseEntered(e -> tooltip.show(node, e.getScreenX() + 10, e.getScreenY() + 10));
		node.setOnMouseExited(e -> tooltip.hide());
	}

	/**
	 * Показ тултипа возле ноуда
	 * @param node
	 * @param text
	 */
	private void showErrorToolTip(Node node, String text) {
		Tooltip tooltip = new Tooltip(text);
		Bounds bounds = node.localToScreen(node.getBoundsInLocal());
		tooltip.show(node, bounds.getMaxX(), bounds.getMinY());
		EventHandler<? super MouseEvent> action = node.getOnMousePressed();
		node.setOnMousePressed(event -> {
			tooltip.hide();
			if (action != null)
				action.handle(event);
		});
	}


	private Properties readData() {
		File file = new File("data.properties");

		Properties properties = new Properties();
		try (Reader reader = file.exists()
				? new FileReader(file)
				: new InputStreamReader(getClass().getResourceAsStream(file.getName())) ) {

			properties.load(reader);

		} catch (Exception e) {
			e.printStackTrace();
		}

		return properties;
	}

	private double[] convertStringToDoubleArray(String stringArray) {
		String[] stringData = stringArray.split(",\\s*");
		double[] data = new double[stringData.length];

		for (int i = 0; i < stringData.length; i++) {
			data[i] = Double.parseDouble(stringData[i]);
		}

		return data;
	}
}
