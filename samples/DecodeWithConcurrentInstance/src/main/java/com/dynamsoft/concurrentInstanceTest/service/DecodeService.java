package com.dynamsoft.concurrentInstanceTest.service;

import com.dynamsoft.concurrentInstanceTest.zz3rdPart.DynamsoftBarcodeWrapper;
import com.dynamsoft.dbr.TextResult;
import org.springframework.stereotype.Service;

@Service("decodeService")
public class DecodeService {
    public TextResult[] decode(byte[] bytes) throws Exception {
        return DynamsoftBarcodeWrapper.decode(bytes);
    }
}
