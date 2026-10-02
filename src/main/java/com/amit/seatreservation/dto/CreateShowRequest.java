package com.amit.seatreservation.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class CreateShowRequest {
	@NotBlank(message = "Show name is required")
    private String name;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than zero")
    private Long pricePaise;

    @NotNull(message = "Per-user limit is required")
    @Min(value = 1, message = "Minimum limit is 1")
    @Max(value = 4, message = "Maximum limit is 4")
    private Integer perUserLimit = 4;

    public CreateShowRequest() {
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getPricePaise() {
		return pricePaise;
	}

	public void setPricePaise(Long pricePaise) {
		this.pricePaise = pricePaise;
	}

	public Integer getPerUserLimit() {
		return perUserLimit;
	}

	public void setPerUserLimit(Integer perUserLimit) {
		this.perUserLimit = perUserLimit;
	}
    
    
}