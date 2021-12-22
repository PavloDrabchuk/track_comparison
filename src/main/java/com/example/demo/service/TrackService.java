package com.example.demo.service;

import com.example.demo.TracksRepository;
import com.example.demo.model.Result;
import com.example.demo.model.Track;
import org.apache.commons.math3.util.Precision;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TrackService {

    public final TracksRepository tracksRepository;

    public TrackService(TracksRepository tracksRepository) {
        this.tracksRepository = tracksRepository;
    }

    public String transferCoordinates(double longitude, double latitude, int accuracy) {
        double xLeft = -180, xRight = 180, yTop = 90, yBottom = -90;

        StringBuilder codeCoordinate = new StringBuilder();
        for (int a = 0; a < accuracy; a++) {
            if (longitude >= (xLeft + xRight) / 2 && latitude >= (yTop + yBottom) / 2) {
                codeCoordinate.append('S');
                xLeft = (xLeft + xRight) / 2;
                yBottom = (yTop + yBottom) / 2;
            } else if (longitude < (xLeft + xRight) / 2 && latitude > (yTop + yBottom) / 2) {
                codeCoordinate.append('Q');
                xRight = (xLeft + xRight) / 2;
                yBottom = (yTop + yBottom) / 2;
            } else if (longitude <= (xLeft + xRight) / 2 && latitude <= (yTop + yBottom) / 2) {
                codeCoordinate.append('T');
                xRight = (xLeft + xRight) / 2;
                yTop = (yTop + yBottom) / 2;
            } else if (longitude > (xLeft + xRight) / 2 && latitude < (yTop + yBottom) / 2) {
                codeCoordinate.append('P');
                xLeft = (xLeft + xRight) / 2;
                yTop = (yTop + yBottom) / 2;
            }
        }
        return codeCoordinate.toString();
    }

    public Track getFirstById(Integer id) {
        return tracksRepository.getFirstById(id);
    }

    public List<Result> getResultListForPoint(Double lon, Double lat, Integer accuracy) {
        List<String> listTrackId = tracksRepository.getListTrackId();
        String codeCoordinate = transferCoordinates(lon, lat, 25);

        return createResultList(listTrackId, accuracy, codeCoordinate);
    }

    public List<Result> getResultListForTrack(Integer accuracy, Integer idTrack) {
        List<Result> resultList;

        List<String> listTrackId = tracksRepository.getListTrackId();

        System.out.println("tId: " + idTrack);

        if (!listTrackId.contains(idTrack.toString())) {
            //після порівняння, збереження до основної таблиці
            tracksRepository.addCoordinatesToBasicTable();
            System.out.println("--- added new track ---");
        }

        listTrackId.remove(idTrack.toString());
        resultList = createResultList(listTrackId, accuracy, "tracks");

        tracksRepository.deleteAll(); //очищення тимчасової таблиці
        return resultList;
    }

    public List<Result> createResultList(List<String> listTrackId, Integer accuracy, String codeCoordinate) {
        List<Result> resultList = new ArrayList<>();

        int countJointTracks, countTrack;
        double percentageOfSimilarity;

        for (String id : listTrackId) {

            countJointTracks = (codeCoordinate.equals("tracks"))
                    ? tracksRepository.compareTable(accuracy, Integer.parseInt(id))
                    : tracksRepository.comparePoint(accuracy, Integer.parseInt(id), codeCoordinate);


            countTrack = tracksRepository.getCountTrackById(Integer.parseInt(id));

            percentageOfSimilarity = ((double) countJointTracks / (double) countTrack * 1.0) * 100;
            System.out.println("trkId: " + id + "\nКількість треків, що співпали: " + countJointTracks + "\nКількість треків у основній таблиці: " + countTrack);
            System.out.println("Відсоток схожості: " + percentageOfSimilarity + "\n");


            if (percentageOfSimilarity != 0)
                resultList.add(new Result(id, Precision.round(percentageOfSimilarity, 2)));

        }

        return resultList;
    }
}
