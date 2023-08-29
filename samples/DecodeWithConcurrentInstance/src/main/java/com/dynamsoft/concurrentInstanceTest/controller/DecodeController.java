package com.dynamsoft.concurrentInstanceTest.controller;

import com.dynamsoft.concurrentInstanceTest.zzUtil;
import com.dynamsoft.concurrentInstanceTest.service.DecodeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;

@RestController
@RequestMapping("/decode")
public class DecodeController {

    @Resource
    DecodeService decodeService;

    @PostMapping(consumes = {"multipart/form-data"})
    String decode(@RequestParam("image")MultipartFile mpImage) throws Exception {
        var bytesImage = mpImage.getBytes();
        var results = decodeService.decode(bytesImage);
        return zzUtil.jsonMapper.writeValueAsString(results);
    }
}
