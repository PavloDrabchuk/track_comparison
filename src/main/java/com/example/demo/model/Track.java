package com.example.demo.model;

import javax.persistence.*;

@Entity(name="TemporaryCoordinates")
public class Track {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Integer id;
    private Integer trkId;
    private String codeCoordinate;

    public Track(Integer trkId) {
        this.trkId = trkId;
    }

    public Track(Integer trkId, String codeCoordinate){
        this.trkId=trkId;
        this.codeCoordinate=codeCoordinate;
    }

    public Track(){}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTrkId() {
        return trkId;
    }

    public void setTrkId(Integer trkId) {
        this.trkId = trkId;
    }

    public String getCodeCoordinate() {
        return codeCoordinate;
    }

    public void setCodeCoordinate(String codeCoordinate) {
        this.codeCoordinate = codeCoordinate;
    }


}
