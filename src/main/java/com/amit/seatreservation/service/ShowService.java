package com.amit.seatreservation.service;

import java.util.List;

import com.amit.seatreservation.dto.CreateShowRequest;
import com.amit.seatreservation.dto.ShowResponse;

public interface ShowService {
	ShowResponse createShow(CreateShowRequest request);
    ShowResponse getShowById(Long showId);
    List<ShowResponse> getAllShows();

}
