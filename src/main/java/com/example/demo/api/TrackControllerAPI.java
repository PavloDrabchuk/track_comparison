package com.example.demo.api;

import com.example.demo.model.Result;
import com.example.demo.service.TrackService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TrackControllerAPI {
    private final TrackService trackService;

    @Autowired
    public TrackControllerAPI(TrackService trackService) {
        this.trackService = trackService;
    }

    @GetMapping("/api/compare-tracks-and-point/lon={lon},lat={lat}/accuracy={accuracy}")
    public List<Result> compareTracksWithPoint(@PathVariable("lon") Double lon,
                                               @PathVariable("lat") Double lat,
                                               @PathVariable("accuracy") Integer accuracy) {

        return trackService.getResultListForPoint(lon, lat, accuracy);
    }

    @GetMapping("/api/compare-tracks/trkId={id}/accuracy={accuracy}")
    public List<Result> compareTracks(@PathVariable("accuracy") Integer accuracy,
                                      @PathVariable("id") Integer idTrack) {

        return trackService.getResultListForTrack(accuracy, idTrack);
    }
}
