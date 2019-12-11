package net.tospay.auth.model.transfer;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.UUID;

public class OrderInfo implements Parcelable{

    @SerializedName("amount")
    @Expose
    private Amount amount;

    @SerializedName("reference")
    @Expose
    private String reference;

    protected OrderInfo(Parcel in) {
        amount = in.readParcelable(Amount.class.getClassLoader());
        reference = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(amount, flags);
        dest.writeString(reference);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<OrderInfo> CREATOR = new Creator<OrderInfo>() {
        @Override
        public OrderInfo createFromParcel(Parcel in) {
            return new OrderInfo(in);
        }

        @Override
        public OrderInfo[] newArray(int size) {
            return new OrderInfo[size];
        }
    };

    public Amount getAmount() {
        return amount;
    }

    public void setAmount(Amount amount) {
        this.amount = amount;
    }

    public String getReference() {
        return UUID.randomUUID().toString();
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public String toString() {
        return "OrderInfo{" +
                "amount=" + amount +
                ", reference='" + reference + '\'' +
                '}';
    }
}
