package com.dinesync.dinesync.model;

public class OrderMessage {

    private int table;
    private String item;

    public OrderMessage() {
    }

    public OrderMessage(int table, String item) {
        this.table = table;
        this.item = item;
    }

    public int getTable() {
        return table;
    }

    public void setTable(int table) {
        this.table = table;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }
}
