package com.chqqc.zhync.chaodalatitudeandl.entity;

import io.realm.RealmList;
import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by Time on 17/3/27.
 */

public class LocaInfo extends RealmObject{
    private String id;
    private String longitude;
    private String latitude;
    private String altitude;
    private String speed;
    private RealmList<LocaInfo> realmList;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getAltitude() {
        return altitude;
    }

    public void setAltitude(String altitude) {
        this.altitude = altitude;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public RealmList<LocaInfo> getRealmList() {
        return realmList;
    }

    public void setRealmList(RealmList<LocaInfo> realmList) {
        this.realmList = realmList;
    }
}
