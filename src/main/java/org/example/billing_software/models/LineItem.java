// src/main/java/org/example/billing_software/models/LineItem.java
package org.example.billing_software.models;

import javafx.beans.property.*;

public class LineItem {
    public final StringProperty  particulars = new SimpleStringProperty();
    public final IntegerProperty quantity    = new SimpleIntegerProperty(1);
    public final DoubleProperty  rate        = new SimpleDoubleProperty(0.0);
    public final DoubleProperty  amount      = new SimpleDoubleProperty(0.0);
}
