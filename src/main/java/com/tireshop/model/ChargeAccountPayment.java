package com.tireshop.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * A payment made by a customer against their store charge account balance.
 * These are payoffs recorded from the Customers tab, not tied to a specific sale.
 */
@Entity
@Table(name = "charge_account_payments")
public class ChargeAccountPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "payment_timestamp", nullable = false)
    private LocalDateTime paymentTimestamp;

    @Column(name = "balance_after")
    private BigDecimal balanceAfter;

    private String notes;

    public ChargeAccountPayment() {
        this.paymentTimestamp = LocalDateTime.now();
    }

    public ChargeAccountPayment(Customer customer, BigDecimal amount) {
        this();
        this.customer = customer;
        this.amount = amount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getPaymentTimestamp() {
        return paymentTimestamp;
    }

    public void setPaymentTimestamp(LocalDateTime paymentTimestamp) {
        this.paymentTimestamp = paymentTimestamp;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
