package com.assignment.pdfgen.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Objects;


public class InvoiceRequest {

    @NotBlank(message = "seller must not be blank")
    private String seller;

    @NotBlank(message = "sellerGstin must not be blank")
    private String sellerGstin;

    @NotBlank(message = "sellerAddress must not be blank")
    private String sellerAddress;

    @NotBlank(message = "buyer must not be blank")
    private String buyer;

    @NotBlank(message = "buyerGstin must not be blank")
    private String buyerGstin;

    @NotBlank(message = "buyerAddress must not be blank")
    private String buyerAddress;

    @NotEmpty(message = "items must contain at least one entry")
    @Valid
    private List<InvoiceItem> items;

    public InvoiceRequest() {
    }

    public String getSeller() {
        return seller;
    }

    public void setSeller(String seller) {
        this.seller = seller;
    }

    public String getSellerGstin() {
        return sellerGstin;
    }

    public void setSellerGstin(String sellerGstin) {
        this.sellerGstin = sellerGstin;
    }

    public String getSellerAddress() {
        return sellerAddress;
    }

    public void setSellerAddress(String sellerAddress) {
        this.sellerAddress = sellerAddress;
    }

    public String getBuyer() {
        return buyer;
    }

    public void setBuyer(String buyer) {
        this.buyer = buyer;
    }

    public String getBuyerGstin() {
        return buyerGstin;
    }

    public void setBuyerGstin(String buyerGstin) {
        this.buyerGstin = buyerGstin;
    }

    public String getBuyerAddress() {
        return buyerAddress;
    }

    public void setBuyerAddress(String buyerAddress) {
        this.buyerAddress = buyerAddress;
    }

    public List<InvoiceItem> getItems() {
        return items;
    }

    public void setItems(List<InvoiceItem> items) {
        this.items = items;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceRequest)) return false;
        InvoiceRequest that = (InvoiceRequest) o;
        return Objects.equals(seller, that.seller)
                && Objects.equals(sellerGstin, that.sellerGstin)
                && Objects.equals(sellerAddress, that.sellerAddress)
                && Objects.equals(buyer, that.buyer)
                && Objects.equals(buyerGstin, that.buyerGstin)
                && Objects.equals(buyerAddress, that.buyerAddress)
                && Objects.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seller, sellerGstin, sellerAddress, buyer, buyerGstin, buyerAddress, items);
    }
}
