package org.example.billing_software.views;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.example.billing_software.services.InvoiceRepository;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SalesChartView {

    public static Node create(Connection conn) {
        // --- Precompute the three category lists ---
        List<String> dayCats = IntStream.rangeClosed(1, 31)
                .mapToObj(String::valueOf)
                .collect(Collectors.toList());

        List<String> monthCats = IntStream.rangeClosed(1, 12)
                .mapToObj(m -> Month.of(m)
                        .getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                .collect(Collectors.toList());

        int currentYear = LocalDate.now().getYear();
        List<String> yearCats = IntStream.rangeClosed(currentYear - 9, currentYear)
                .mapToObj(String::valueOf)
                .collect(Collectors.toList());

        // --- Controls Pane ---
        ComboBox<String> granBox = new ComboBox<>(FXCollections.observableArrayList(
                "Daily", "Monthly", "Yearly"
        ));
        granBox.setValue("Daily");

        ComboBox<Integer> yearBox = new ComboBox<>(FXCollections.observableArrayList(
                IntStream.rangeClosed(currentYear - 10, currentYear)
                        .boxed()
                        .sorted((a, b) -> b - a)
                        .collect(Collectors.toList())
        ));
        yearBox.setValue(currentYear);

        ComboBox<Integer> monthBox = new ComboBox<>(FXCollections.observableArrayList(
                IntStream.rangeClosed(1, 12).boxed().collect(Collectors.toList())
        ));
        monthBox.setValue(LocalDate.now().getMonthValue());

        HBox controls = new HBox(10,
                new Label("Granularity:"), granBox,
                new Label("Year:"), yearBox,
                new Label("Month:"), monthBox
        );
        controls.setPadding(new Insets(10));

        // --- Chart Setup ---
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setAutoRanging(false);
        xAxis.setLabel("Time");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Sales (₹)");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Sales Overview");
        chart.setCategoryGap(2);
        chart.setBarGap(1);

        // --- Chart Refresh Logic ---
        Runnable updateChart = () -> {
            chart.getData().clear();

            String gran = granBox.getValue();
            int year = yearBox.getValue();

            switch (gran) {
                case "Daily" -> {
                    // Show days 1–31 for the chosen month+year
                    monthBox.setDisable(false);
                    yearBox .setDisable(false);
                    xAxis.getCategories().setAll(dayCats);

                    Map<java.time.LocalDate, Double> daily =
                            InvoiceRepository.fetchDailySales(conn, year, monthBox.getValue());

                    var series = new XYChart.Series<String, Number>();
                    series.setName(String.format("%d-%02d", year, monthBox.getValue()));

                    for (String d : dayCats) {
                        java.time.LocalDate dt = LocalDate.of(year, monthBox.getValue(), Integer.parseInt(d));
                        Double val = daily.get(dt);
                        if (val != null) {
                            series.getData().add(new XYChart.Data<>(d, val));
                        }
                    }
                    chart.getData().add(series);
                }
                case "Monthly" -> {
                    // Show Jan–Dec for the chosen year
                    monthBox.setDisable(true);
                    yearBox .setDisable(false);
                    xAxis.getCategories().setAll(monthCats);

                    Map<Integer, Double> monthly =
                            InvoiceRepository.fetchMonthlySales(conn, year);

                    var series = new XYChart.Series<String, Number>();
                    series.setName(String.valueOf(year));

                    for (int m = 1; m <= 12; m++) {
                        String lbl = monthCats.get(m - 1);
                        Double val = monthly.get(m);
                        if (val != null) {
                            series.getData().add(new XYChart.Data<>(lbl, val));
                        }
                    }
                    chart.getData().add(series);
                }
                case "Yearly" -> {
                    // Show last 10 years as categories
                    monthBox.setDisable(true);
                    yearBox .setDisable(true);
                    xAxis.getCategories().setAll(yearCats);

                    Map<Integer, Double> yearly =
                            InvoiceRepository.fetchYearlySales(conn, currentYear - 9, currentYear);

                    var series = new XYChart.Series<String, Number>();
                    series.setName((currentYear - 9) + "–" + currentYear);

                    for (String yStr : yearCats) {
                        int y = Integer.parseInt(yStr);
                        Double val = yearly.get(y);
                        if (val != null) {
                            series.getData().add(new XYChart.Data<>(yStr, val));
                        }
                    }
                    chart.getData().add(series);
                }
            }
        };

        // --- Wire up listeners so switching any control redraws immediately ---
        granBox .setOnAction(e -> updateChart.run());
        yearBox .setOnAction(e -> updateChart.run());
        monthBox.setOnAction(e -> updateChart.run());

        // initial draw
        updateChart.run();

        // --- Assemble and return ---
        VBox root = new VBox(10, controls, chart);
        root.setPadding(new Insets(20));
        return root;
    }
}
