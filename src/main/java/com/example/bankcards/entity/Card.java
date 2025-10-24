package com.example.bankcards.entity;

import com.example.bankcards.util.PanAttributeConverter;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "card")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "owner_id", nullable = false)
    private Owner owner;

    @Column(name = "pan_encrypted", nullable = false, columnDefinition = "BYTEA")
    @Lob
    @Convert(converter = PanAttributeConverter.class)
    private String pan;

    @Column(name = "pan_last4", nullable = false, length = 4)
    private String panLast4;

    @Column(name = "bin", length = 8, nullable = false)
    private String bin;

    @Column(name = "expiry_month", nullable = false)
    private short expiryMonth;

    @Column(name = "expiry_year", nullable = false) // исправлено!
    private short expiryYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CardStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    public Card() {
    }

    public String getPan() {
        return pan;
    }

    public void setPan(String pan) {
        this.pan = pan;
        this.panLast4 = pan.substring(pan.length() - 4);
        this.bin = pan.substring(0, 6);
    }

    public String getPanLast4() {
        return panLast4;
    }

    public void setPanLast4(String panLast4) {
        this.panLast4 = panLast4;
    }

    public String getBin() {
        return bin;
    }

    public void setBin(String bin) {
        this.bin = bin;
    }

    public short getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(short expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public short getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(short expiryYear) {
        this.expiryYear = expiryYear;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (getClass() != o.getClass()) return false;

        Card other = (Card) o;

        if (this.id != null && other.id != null) {
            return Objects.equals(this.id, other.id);
        }
        // fallback: бизнес-ключ (у нас есть составной UNIQUE в БД)
        Long thisOwnerId = this.owner != null ? this.owner.getId() : null;
        Long otherOwnerId = other.owner != null ? other.owner.getId() : null;

        return Objects.equals(thisOwnerId, otherOwnerId)
                && Objects.equals(this.panLast4, other.panLast4)
                && this.expiryMonth == other.expiryMonth
                && this.expiryYear == other.expiryYear
                && Objects.equals(this.bin, other.bin);
    }

    @Override
    public int hashCode() {
        if (id != null) {
            return Objects.hash(id);
        }
        Long ownerId = owner != null ? owner.getId() : null;
        return Objects.hash(ownerId, panLast4, expiryMonth, expiryYear, bin);
    }

    @Override
    public String toString() {
        return "Card{" +
                "id=" + id +
                ", ownerId=" + (owner != null ? owner.getId() : null) +
                ", status=" + status +
                ", currency=" + currency +
                ", balance=" + balance +
                ", mask=**** **** **** " + panLast4 +
                '}';
    }
}
