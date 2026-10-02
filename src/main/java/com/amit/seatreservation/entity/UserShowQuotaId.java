package com.amit.seatreservation.entity;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UserShowQuotaId {
	
	  private Long userId;

	    private Long showId;

	    public UserShowQuotaId() {
	    }

	    public UserShowQuotaId(Long userId, Long showId) {
	        this.userId = userId;
	        this.showId = showId;
	      
	        
	    }

		public Long getUserId() {
			return userId;
		}

		public void setUserId(Long userId) {
			this.userId = userId;
		}

		public Long getShowId() {
			return showId;
		}

		public void setShowId(Long showId) {
			this.showId = showId;
		}
	    

}
