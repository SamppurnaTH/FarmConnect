package com.agrichain.reporting.dto;

/**
 * A single row of data in a generated report CSV.
 */
public class ReportDataRow {

    private final String id;
    private final String date;
    private final String description;
    private final String amount;
    private final String status;

    public ReportDataRow(String id, String date, String description, String amount, String status) {
        this.id          = id;
        this.date        = date;
        this.description = description;
        this.amount      = amount;
        this.status      = status;
    }

    public String getId()          { return id; }
    public String getDate()        { return date; }
    public String getDescription() { return description; }
    public String getAmount()      { return amount; }
    public String getStatus()      { return status; }
}
