package com.example.demo;

import com.example.demo.model.Track;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface TracksRepository extends CrudRepository<Track, Integer> {
    Optional<List<Track>> findByTrkId(Integer trkId);

    @Query(
            value = "SELECT count(*) FROM `basic_coordinates`",
            nativeQuery = true)
    int selectAllFromBasicTable();

    @Modifying
    @Query(
            value = "INSERT INTO `basic_coordinates` (`code_coordinate`,`trk_id`) SELECT `code_coordinate`,`trk_id` FROM `temporary_coordinates`",
            nativeQuery = true)
    @Transactional
    void addCoordinatesToBasicTable();

    @Query(
            value = "SELECT COUNT(DISTINCT basic_coordinates.code_coordinate) AS 'count' FROM basic_coordinates " +
                    "INNER JOIN temporary_coordinates " +
                    "ON SUBSTRING(basic_coordinates.code_coordinate,1,?1) = SUBSTRING(temporary_coordinates.code_coordinate,1,?1 ) " +
                    "WHERE basic_coordinates.trk_id=?2",
            nativeQuery = true)
    @Transactional
    int compareTable(int accuracy, int idTrackBasicTable);

    @Query(
            value = "SELECT COUNT(DISTINCT basic_coordinates.code_coordinate) AS 'count' FROM basic_coordinates " +
                    "WHERE SUBSTRING(basic_coordinates.code_coordinate,1,?1) = SUBSTRING(?3,1,?1 ) " +
                    "AND basic_coordinates.trk_id=?2",
            nativeQuery = true
    )
    @Transactional
    int comparePoint(int accuracy, int idTrackBasicTable, String pointCodeCoordinate);


    @Query(
            value = "select DISTINCT(c.trk_id) from basic_coordinates c order by c.trk_id",
            nativeQuery = true)
    List<String> getListTrackId();

    @Query(value = "SELECT DISTINCT (t.trk_id) from temporary_coordinates t",
            nativeQuery = true)
    int getTemporaryTrackId();

    @Query(
            value = "select COUNT(*) from basic_coordinates b WHERE b.trk_id=?1",
            nativeQuery = true)
    int getCountTrackById(int trkId);

    @Modifying
    @Query(
            value = "delete from `basic_coordinates` where (`trk_id`=?1)",
            nativeQuery = true
    )
    @Transactional
    void deleteByStringId(String trkId);

    @Query(
            value = "select DISTINCT(c.trk_id) from basic_coordinates c order by c.trk_id limit ?1, ?2",
            nativeQuery = true)
    List<String> getListTrackIdForPage(int offset, int row_count);

    Track getFirstById(Integer id);
}
