package com.amit.seatreservation.dto;

public class ShowResponse {

	private Long id;
	private String name;
	private Long pricePaise;
	private Integer perUserLimit;

	public ShowResponse() {
	}

	public ShowResponse(Long id, String name, Long pricePaise, Integer perUserLimit) {
		this.id = id;
		this.name = name;
		this.pricePaise = pricePaise;
		this.perUserLimit = perUserLimit;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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