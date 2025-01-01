package com.uestc.backupsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class ComparisonResultDTO {
    private boolean isIdentical;
    private List<String> differences;
    public ComparisonResultDTO() {
        differences = new ArrayList<>();
    }

    @Override
    public String toString() {
        return "ComparisonResultDTO{" +
                "isIdentical=" + isIdentical +
                ", differences=" + differences +
                '}';
    }
}
