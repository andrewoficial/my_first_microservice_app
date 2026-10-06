package org.example.services;

/**
 * Точка графика для веб-страницы. Те же данные, что рисует окно ChartWindow.
 */
public record GraphSample(long epochMilli, double[] values) {
}
