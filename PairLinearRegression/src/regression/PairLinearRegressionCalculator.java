package regression;

import static java.lang.Math.*;

public class PairLinearRegressionCalculator {

	//исходные данные
	private final double[] x;
	private final double[] y;
	private final int n;

	//среднее X
	private double avgX;

	//коэффициенты системы
	private double a;
	private double b;

	//выборочная остаточная дисперсия (S^2)_ост
	private double sRemSqr;

	//выборочные дисперсии по x, y	(S^2)_x, (S^2)_y
	private double sXSqr;
	private double sYSqr;

	//стандартные ошибки коэффициентов a, b		m_a, m_b
	private double ma;
	private double mb;

	//выборочный коэффициент парной корреляции r_xy
	private double rxy;

	//Коэффициент детерминации R^2
	private double rSqr;

	//средняя ошибка аппроксимации регрессии
	private double avgA;

	//статистики по a, b, r_xy (Стьюдент)
	private double ta;
	private double tb;
	private double tr;

	//статистика по R (Фишер)
	private double F;

	//функция линейной парной регрессии
	public double Y(double X) {
		return a + b * X;
	}


	public PairLinearRegressionCalculator(double[] x, double[] y, int n) {
		this.x = x;
		this.y = y;
		this.n = n;

		/*
		 * Расчет коэффициентов уравнения теоретической регрессии a, b
		 */
		avgX = 0;
		double avgY = 0;
		double avgXY = 0;
		double avgXX = 0;
		for (int i = 0; i < n; i++) {
			avgX += x[i] / n;
			avgY += y[i] / n;
			avgXY += (x[i] * y[i]) / n;
			avgXX += (x[i] * x[i]) / n;
		}
		b = (avgXY - avgX * avgY) / (avgXX - avgX * avgX);
		a = avgY - b * avgX;


		/*
		 * Расчет остаточной дисперсии (S^2)_ост
		 */
		double sumYMinusRegYSqr = 0;
		for (int i = 0; i < n; i++) {
			sumYMinusRegYSqr += (y[i] - Y(x[i])) * (y[i] - Y(x[i]));
		}
		sRemSqr = sumYMinusRegYSqr / (n - 2);


		/*
		 * Расчет выборочной дисперсии (S^2)_x, (S^2)_y
		 */
		double sumXMinusAvgXSqr = 0;
		double sumYMinusAvgYSqr = 0;
		for (int i = 0; i < n; i++) {
			sumXMinusAvgXSqr += (x[i] - avgX) * (x[i] - avgX);
			sumYMinusAvgYSqr += (y[i] - avgY) * (y[i] - avgY);
		}
		sXSqr = sumXMinusAvgXSqr / n;
		sYSqr = sumYMinusAvgYSqr / n;


		/*
		 * Расчет стандартных ошибок коэффициентов m_a, m_b
		 */
		double sumXSqr = 0;
		for (int i = 0; i < n; i++) {
			sumXSqr += x[i] * x[i];
		}
		ma = sqrt(sRemSqr) * sqrt(sumXSqr) / (sqrt(sXSqr) * n);
		mb = sqrt(sRemSqr) / (sqrt(sXSqr) * sqrt(n));


		/*
		 * Расчет выборочного коэффициента парной корреляции
		 */
		double sumYMinusAvgYMultiplyXMinusAvgX = 0;
		for (int i = 0; i < n; i++) {
			sumYMinusAvgYMultiplyXMinusAvgX += (y[i] - avgY) * (x[i] - avgX);
		}
		rxy = (sumYMinusAvgYMultiplyXMinusAvgX / n) / (sqrt(sXSqr) * sqrt(sYSqr));


		/*
		 * Расчет коэффициента детерминации rSqr^2
		 */
		rSqr = 1 - (sumYMinusRegYSqr / sumYMinusAvgYSqr);


		/*
		 * расчет средней ошибки аппроксимации A (%)
		 */
		double sumAbsYMinusRegYDetAbsY = 0;
		for (int i = 0; i < n; i++) {
			sumAbsYMinusRegYDetAbsY += abs(y[i] - Y(x[i])) / abs(y[i]);
		}
		avgA = (sumAbsYMinusRegYDetAbsY / n) * 100;


		/*
		 * расчет статистик по a, b, r_xy (Стьюдент)
		 */
		ta = a / ma;
		tb = b / mb;
		tr = sqrt(n - 2) * rxy / sqrt(1 - rxy * rxy);


		/*
		 * Расчет статистики R^2 (Фишер)
		 */
		F = (n - 2) * rSqr / (1 - rSqr);
	}


	/**
	 * Расчет доверительных интервалов коэффициента a
	 */
	public double calcCoefficientAOffset(double alpha) {
		double ttabl = calcTTable(alpha);
		return ma * ttabl;
	}

	/**
	 * Расчет доверительных интервалов коэффициента b
	 */
	public double calcCoefficientBOffset(double alpha) {
		double ttabl = calcTTable(alpha);
		return mb * ttabl;
	}

	/**
	 * Расчет доверительного интервала точечного прогноза
	 */
	public double calcStandardPrognosesError(double X, double alpha) {
		double ttabl = calcTTable(alpha);

		double sum = 0;
		for (int i = 0; i < n; i++) {
			sum += x[i] * avgX;
		}

		double my = sqrt(sRemSqr) * sqrt(1 + (1 / n) + (X - avgX) * (X - avgX) / sum);

		return my * ttabl;
	}

	public double calcTTable(double alpha) {
		return new org.apache.commons.math3.distribution.TDistribution(n - 2).inverseCumulativeProbability(1 - alpha);
	}

	public double calcFTable(double alpha) {
		return new org.apache.commons.math3.distribution.FDistribution(1, n - 2).inverseCumulativeProbability(1 - alpha);
	}

	public double getA() {
		return a;
	}

	public double getB() {
		return b;
	}

	public double getSRemSqr() {
		return sRemSqr;
	}

	public double getSXSqr() {
		return sXSqr;
	}

	public double getSYSqr() {
		return sYSqr;
	}

	public double getMa() {
		return ma;
	}

	public double getMb() {
		return mb;
	}

	public double getRxy() {
		return rxy;
	}

	public double getRSqr() {
		return rSqr;
	}

	public double getAvgA() {
		return avgA;
	}

	public double getTa() {
		return ta;
	}

	public double getTb() {
		return tb;
	}

	public double getTr() {
		return tr;
	}

	public double getF() {
		return F;
	}
}
