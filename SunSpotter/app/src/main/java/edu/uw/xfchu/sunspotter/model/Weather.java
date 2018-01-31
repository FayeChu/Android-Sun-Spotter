package edu.uw.xfchu.sunspotter.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

/**
 * Created by xfchu on 10/13/17.
 */

public class Weather {

    public String icon, info, date;
    public Double temp;

    public Weather(){

    }

    public Weather(String icon, String info, String date, Double temp) {
        this.icon = icon;
        this.info = info;
        this.date = date;
        this.temp = temp;
    }

}

