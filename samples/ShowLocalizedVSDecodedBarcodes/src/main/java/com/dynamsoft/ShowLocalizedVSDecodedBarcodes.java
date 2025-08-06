package com.dynamsoft;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import com.dynamsoft.core.EnumErrorCode;
import com.dynamsoft.core.IntermediateResultExtraInfo;
import com.dynamsoft.core.basic_structures.ImageData;
import com.dynamsoft.core.basic_structures.Point;
import com.dynamsoft.core.basic_structures.Quadrilateral;
import com.dynamsoft.cvr.CaptureVisionException;
import com.dynamsoft.cvr.CaptureVisionRouter;
import com.dynamsoft.cvr.CapturedResult;
import com.dynamsoft.cvr.EnumPresetTemplate;
import com.dynamsoft.cvr.IntermediateResultManager;
import com.dynamsoft.cvr.IntermediateResultReceiver;
import com.dynamsoft.dbr.BarcodeResultItem;
import com.dynamsoft.dbr.DecodedBarcodesResult;
import com.dynamsoft.dbr.intermediate_results.LocalizedBarcodeElement;
import com.dynamsoft.dbr.intermediate_results.LocalizedBarcodesUnit;
import com.dynamsoft.license.LicenseError;
import com.dynamsoft.license.LicenseException;
import com.dynamsoft.license.LicenseManager;
import com.dynamsoft.utility.ImageDrawer;
import com.dynamsoft.utility.ImageIO;
import com.dynamsoft.utility.UtilityException;

class MyIntermediateResultReceiver extends IntermediateResultReceiver {

    public List<Quadrilateral> locations = new ArrayList<>();

    @Override
    public void onLocalizedBarcodesReceived(LocalizedBarcodesUnit result, IntermediateResultExtraInfo info) {
        if (info.isSectionLevelResult) {
            LocalizedBarcodeElement[] elements = result.getLocalizedBarcodes();
            for (LocalizedBarcodeElement element : elements) {
                locations.add(element.getLocation());
            }
        }
    }
}

public class ShowLocalizedVSDecodedBarcodes {

    private static boolean isWithin10Percent(int area1, int area2) {
        if (area1 == 0 || area2 == 0) {
            return false;
        }
        double ratio = (double)Math.abs(area1 - area2) / Math.max(area1, area2);
        return ratio <= 0.10;
    }

    private static boolean seemAsSameLocation(Quadrilateral location, Quadrilateral resultLoc) {
        int x = 0, y = 0;
        Point[] locationPoints = location.points;
        for (Point point : locationPoints) {
            x += point.getX();
            y += point.getY();
        }

        x = (x + locationPoints.length / 2) / locationPoints.length;
        y = (y + locationPoints.length / 2) / locationPoints.length;
        if (!resultLoc.contains(new Point(x, y))) {
            return false;
        }
        if (isWithin10Percent(location.getArea(), resultLoc.getArea())) {
            return true;
        }
        return false;
    }

    private static List<Quadrilateral> removeIfLocExistsInResultLocs(List<Quadrilateral> locations, List<Quadrilateral> resultLocs) {
        if (locations == null || resultLocs == null)
            return null;

        List<Quadrilateral> retLoc = new ArrayList<Quadrilateral>();
        Set<Integer> excludedLoc = new HashSet<Integer>();
        for (int i = 0; i < locations.size(); i++) {
            for (int j = 0; j < resultLocs.size(); j++) {
                if (seemAsSameLocation(locations.get(i), resultLocs.get(j))) {
                    excludedLoc.add(i);
                    break;
                }
            }
        }
        for (int i = 0; i < locations.size(); i++) {
            if (!excludedLoc.contains(i)) {
                retLoc.add(locations.get(i));
            }
        }

        return retLoc;
    }

    private static ImageData drawOnImage(ImageData image, List<Quadrilateral> locations, List<Quadrilateral> resultLocs) {
        ImageDrawer drawer = new ImageDrawer();
        locations = removeIfLocExistsInResultLocs(locations, resultLocs);
        if (locations != null) {
            Quadrilateral[] quads = locations.toArray(new Quadrilateral[0]);
            image = drawer.drawOnImage(image, quads, 0xFFFF0000, 2);
        }
        if (resultLocs != null) {
            Quadrilateral[] quads = resultLocs.toArray(new Quadrilateral[0]);
            image = drawer.drawOnImage(image, quads, 0xFF00FF00, 2);
        }
        return image;
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            int errorCode = 0;
            String errorMsg = "";

            // Initialize license.
            // You can request and extend a trial license from https://www.dynamsoft.com/customer/license/trialLicense?product=dbr&utm_source=samples&package=java
            // The string 'DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9' here is a free public trial license. Note that network connection is required for this license to work.
            try {
                LicenseError licenseError = LicenseManager.initLicense("DLS2eyJvcmdhbml6YXRpb25JRCI6IjIwMDAwMSJ9");
                if (licenseError.getErrorCode() != EnumErrorCode.EC_OK) {
                    errorCode = licenseError.getErrorCode();
                    errorMsg = licenseError.getErrorString();
                }
            } catch (LicenseException e) {
                errorCode = e.getErrorCode();
                errorMsg = e.getErrorString();
            }

            if (errorCode != EnumErrorCode.EC_OK) {
                System.out.println("License initialization failed: ErrorCode: " + errorCode + ", ErrorString: " + errorMsg);
                return;
            }

            CaptureVisionRouter cvRouter = new CaptureVisionRouter();
            IntermediateResultManager irm = cvRouter.getIntermediateResultManager();
            MyIntermediateResultReceiver irr = new MyIntermediateResultReceiver();
            try {
                irm.AddResultReceiver(irr);
            } catch (CaptureVisionException e) {
            }

            String imagePath = "../../images/GeneralBarcodes.png";
            ImageIO imageIO = new ImageIO();

            ImageData image;
            try {
                image = imageIO.readFromFile(imagePath);
            } catch (UtilityException e) {
                System.out.println("Failed to read image.");
                return;
            }

            CapturedResult result = cvRouter.capture(image, EnumPresetTemplate.PT_READ_BARCODES);

            if (result.getErrorCode() == (int)EnumErrorCode.EC_UNSUPPORTED_JSON_KEY_WARNING) {
                System.out.println("Warning: " + result.getErrorCode() + ", " + result.getErrorString());
            } else if (result.getErrorCode() != EnumErrorCode.EC_OK) {
                System.out.println("Error: " + result.getErrorCode() + ", " + result.getErrorString());
            } else {
                List<Quadrilateral> resultLocs = new ArrayList<>();
                DecodedBarcodesResult decodedBarcodes = result.getDecodedBarcodesResult();
                BarcodeResultItem[] decodedItems = decodedBarcodes != null ? decodedBarcodes.getItems() : null;
                if (decodedItems != null && decodedItems.length > 0)
                {
                    for (BarcodeResultItem item : decodedItems) {
                        resultLocs.add(item.getLocation());
                    }
                }

                if (!irr.locations.isEmpty() || !resultLocs.isEmpty()) {
                    ImageData imageComplete = drawOnImage(image, irr.locations, resultLocs);

                    String resultPath = "result.png";
                    try {
                        imageIO.saveToFile(imageComplete, resultPath);
                        System.out.println("Result saved to: " + (new File(resultPath)).getAbsolutePath());
                    } catch (UtilityException e) {
                        System.out.println("Failed to save result image: " + e.getMessage());
                    }
                } else {
                    System.out.println("No barcodes detected.");
                }
            }
        } finally {
            System.out.print("Press Enter to quit...");
            scanner.nextLine();
            scanner.close();
        }
    }
}