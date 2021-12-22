package com.example.demo.api;

import com.example.demo.ProcessingXML;
import com.example.demo.TracksRepository;
import com.example.demo.model.Track;
import com.example.demo.service.TrackService;
import com.example.demo.storage.StorageFileNotFoundException;
import com.example.demo.storage.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class TracksController {
    private final StorageService storageService;

    private final TracksRepository tracksRepository;
    private final TrackService trackService;

    @Autowired
    public TracksController(StorageService storageService, TracksRepository tracksRepository, TrackService trackService) {
        this.storageService = storageService;
        this.tracksRepository = tracksRepository;
        this.trackService = trackService;
    }

    @PostMapping("/compare-point")
    public String compareTracksWithPoint(@RequestParam("lon") Double lon,
                                         @RequestParam("lat") Double lat,
                                         @RequestParam("accuracy") Integer accuracy,
                                         Model model) {
        // System.out.println("lon: " + lon + ", lat: " + lat + ", accuracy: " + accuracy);

        model.addAttribute("resultListComparePoint", trackService.getResultListForPoint(lon, lat, accuracy));
        model.addAttribute("lon", lon);
        model.addAttribute("lat", lat);
        model.addAttribute("accuracy", accuracy);

        return "compare-point";
    }

    @GetMapping("/compare-point")
    public String getComparePointView() {
        return "compare-point";
    }

    @PostMapping("/compareTracks")
    public String compareTracks(@RequestParam("accuracy") String accuracy, RedirectAttributes redirectAttributes) {
        System.out.println("accuracy: " + accuracy);
        int idTrack = tracksRepository.getTemporaryTrackId();

        redirectAttributes.addFlashAttribute("idTrack", idTrack);
        redirectAttributes.addFlashAttribute("resultList", trackService.getResultListForTrack(Integer.parseInt(accuracy), idTrack));

        return "redirect:/uploadForm";
    }

    @GetMapping("/uploadForm")
    public String listUploadedFiles(Model model) {

        model.addAttribute("files", storageService.loadAll().map(
                        path -> MvcUriComponentsBuilder.fromMethodName(TracksController.class,
                                "serveFile", path.getFileName().toString()).build().toUri().toString())
                .collect(Collectors.toList()));

        model.addAttribute("message_upload", "Виберіть kml/gpx файл");

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String name = (auth.getPrincipal() instanceof OidcUser)
                ? ((OidcUser) auth.getPrincipal()).getFullName()
                : auth.getName();

        model.addAttribute("name", name);

        return "uploadForm";
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {

        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/uploadForm")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) throws IOException, SAXException, ParserConfigurationException {

        storageService.store(file);
        redirectAttributes.addFlashAttribute("message",
                "Ви успішно завантажили " + file.getOriginalFilename() + "!");

        String trackName = file.getOriginalFilename();

        ProcessingXML processingXML = new ProcessingXML("upload-dir/" + trackName, trackService);

        Track track;

        int trkId = Integer.parseInt(processingXML.getPartFileName(1));

        if (processingXML.xmlValidation()) {
            System.out.println("ok");
            tracksRepository.deleteAll();
            Set<String> codeOfCoordinates = processingXML.xmlParsing();
            for (String code : codeOfCoordinates) {
                track = new Track(trkId, code);
                tracksRepository.save(track);
            }
        }
        //перевірка чи не порожня основна таблиця

        int countElementBasicTable = tracksRepository.selectAllFromBasicTable();
        System.out.println("countBasicTable: " + countElementBasicTable);

        if (countElementBasicTable == 0) {
            tracksRepository.addCoordinatesToBasicTable();
            tracksRepository.deleteAll();
        } else {
            redirectAttributes.addFlashAttribute("fileName", "Порівняти файл " + file.getOriginalFilename() + " з наявними");
        }
        return "redirect:/uploadForm";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound() {
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/admin")
    public String listUploadedTracks(Model model, @RequestParam(value = "page", defaultValue = "1") String page) throws IOException {
        if (page != null) System.out.println("Get----admin --> " + page);
        double limitTracksId = 2;

        assert page != null;
        List<String> listTrackId = tracksRepository.getListTrackIdForPage((Integer.parseInt(page) - 1) * (int) limitTracksId, (int) limitTracksId);
        List<String> pageNumList = new ArrayList<>();
        for (int i = 1; i <= ((int) Math.ceil(tracksRepository.getListTrackId().size() / limitTracksId)); i++) {
            pageNumList.add(Integer.toString(i));
        }
        model.addAttribute("track", listTrackId);
        model.addAttribute("numPages", pageNumList);

        return "admin";
    }

    @PostMapping("/deleteTracks")
    public String deleteTracks(@RequestParam Map<String, String> allQueryParams, RedirectAttributes redirectAttributes) {

        StringBuilder deleteTrackMessage = new StringBuilder("Треки: ");
        for (String key : allQueryParams.keySet()) {
            tracksRepository.deleteByStringId(key);
            storageService.deleteFileByName(key + ".kml");
            storageService.deleteFileByName(key + ".gpx");
            deleteTrackMessage.append(key).append(", ");
        }
        deleteTrackMessage = new StringBuilder(deleteTrackMessage.substring(0, deleteTrackMessage.length() - 2));
        deleteTrackMessage.append(" видалено успішно.");

        if (allQueryParams.keySet().size() != 0)
            redirectAttributes.addFlashAttribute("deleteTrack", deleteTrackMessage.toString());
        else redirectAttributes.addFlashAttribute("deleteTrack", "Не обрано жодного треку.");

        return "redirect:/admin";
    }

    @PostMapping("/admin/uploadFiles")
    public String uploadFiles(@RequestParam("files") MultipartFile[] files, RedirectAttributes redirectAttributes) throws IOException, SAXException, ParserConfigurationException {

        String trackName;
        ProcessingXML processingXML;
        List<String> trackNames = new ArrayList<>();


        Track track;
        int trkId;

        for (MultipartFile file : files) {
            trackName = file.getOriginalFilename();
            System.out.println(" >> trackName: " + trackName);
            processingXML = new ProcessingXML("upload-dir/" + trackName, trackService);
            System.out.println("2 group: " + processingXML.getPartFileName(2));

            if (processingXML.getPartFileName(2) != "-1") {
                storageService.store(file);
                trackNames.add(trackName);
                trkId = Integer.parseInt(processingXML.getPartFileName(1));
                if (processingXML.xmlValidation()) {
                    System.out.println("ok");
                    tracksRepository.deleteAll();
                    Set<String> codeOfCoordinates = processingXML.xmlParsing();
                    for (String code : codeOfCoordinates) {
                        track = new Track(trkId, code);
                        tracksRepository.save(track);
                    }
                }
                tracksRepository.addCoordinatesToBasicTable();
                tracksRepository.deleteAll();
            }
        }

        StringBuilder message = new StringBuilder("Ви успішно завантажили файли: ");
        for (String tName : trackNames) {
            message.append(tName).append(", ");
        }
        message = new StringBuilder(message.substring(0, message.length() - 2));
        message.append(" на сервер.");

        redirectAttributes.addFlashAttribute("uploadFilesMessage", message.toString());

        return "redirect:/admin";
    }
}
