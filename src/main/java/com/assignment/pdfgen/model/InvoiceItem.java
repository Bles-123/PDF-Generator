package com.assignment.pdfgen.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Objects;


public class InvoiceItem {

    @NotBlank(message = "Item name must not be blank")
    private String name;

    @NotBlank(message = "Item quantity must not be blank")
    private String quantity;

    @NotNull(message = "Item rate is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Rate must not be negative")
    private BigDecimal rate;

    @NotNull(message = "Item amount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Amount must not be negative")
    private BigDecimal amount;

    public InvoiceItem() {
    }

    public InvoiceItem(String name, String quantity, BigDecimal rate, BigDecimal amount) {
        this.name = name;
        this.quantity = quantity;
        this.rate = rate;
        this.amount = amount;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceItem)) return false;
        InvoiceItem that = (InvoiceItem) o;
        return Objects.equals(name, that.name)
                && Objects.equals(quantity, that.quantity)
                && Objects.equals(rate, that.rate)
                && Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, quantity, rate, amount);
    }

    @Override
    public String toString() {
        return "InvoiceItem{" +
                "name='" + name + '\'' +
                ", quantity='" + quantity + '\'' +
                ", rate=" + rate +
                ", amount=" + amount +
                '}';
    }
}
