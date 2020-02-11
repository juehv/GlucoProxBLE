package de.heoegbr.bgproxy.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "bgreadings")
public class BgReading {

    @PrimaryKey
    @ColumnInfo(name = "date")
    public long date;

    public boolean isValid = true;

    @ColumnInfo(name = "value")
    public double value;
    @ColumnInfo(name = "direction")
    public String direction;

    public BgReading() {
    }

    public static boolean isSlopeNameInvalid(String direction) {
        if (direction.compareTo("NOT_COMPUTABLE") == 0 ||
                direction.compareTo("NOT COMPUTABLE") == 0 ||
                direction.compareTo("OUT_OF_RANGE") == 0 ||
                direction.compareTo("OUT OF RANGE") == 0 ||
                direction.compareTo("NONE") == 0 ||
                direction.compareTo("NotComputable") == 0
        ) {
            return true;
        } else {
            return false;
        }
    }

    public String directionToSymbol() {
        String symbol = "";
        if (direction == null) {
            symbol = "??";
        } else if (direction.compareTo("DoubleDown") == 0) {
            symbol = "\u21ca";
        } else if (direction.compareTo("SingleDown") == 0) {
            symbol = "\u2193";
        } else if (direction.compareTo("FortyFiveDown") == 0) {
            symbol = "\u2198";
        } else if (direction.compareTo("Flat") == 0) {
            symbol = "\u2192";
        } else if (direction.compareTo("FortyFiveUp") == 0) {
            symbol = "\u2197";
        } else if (direction.compareTo("SingleUp") == 0) {
            symbol = "\u2191";
        } else if (direction.compareTo("DoubleUp") == 0) {
            symbol = "\u21c8";
        } else if (isSlopeNameInvalid(direction)) {
            symbol = "??";
        }
        return symbol;
    }

    @Override
    public String toString() {
        return "BgReading{" +
                "date=" + date +
                ", date=" + new Date(date).toLocaleString() +
                ", value=" + value +
                ", direction=" + direction +
                '}';
    }

}
