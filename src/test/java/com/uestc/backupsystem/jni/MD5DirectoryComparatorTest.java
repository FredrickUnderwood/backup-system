package com.uestc.backupsystem.jni;

import com.uestc.backupsystem.dto.ComparisonResultDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MD5DirectoryComparatorTest {
    @Autowired
    private MD5DirectoryComparator md5DirectoryComparator;

    @Test
    void compare() {
        ComparisonResultDTO comparisonResultDTO = md5DirectoryComparator.compare("D:/backup/workstation", "E:/workstation");
        System.out.println(comparisonResultDTO.toString());
    }

}