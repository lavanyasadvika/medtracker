package com.medtracker.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportExpiryDTO {

    private List<ReportExpiryItemDTO> expired;
    private List<ReportExpiryItemDTO> expiring;
    private List<ReportExpiryItemDTO> safe;
}
