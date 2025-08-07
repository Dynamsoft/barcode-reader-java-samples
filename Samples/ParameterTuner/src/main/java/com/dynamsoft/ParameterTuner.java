package com.dynamsoft;

import com.dynamsoft.core.EnumErrorCode;
import com.dynamsoft.cvr.CaptureVisionError;
import com.dynamsoft.cvr.CaptureVisionException;
import com.dynamsoft.cvr.CaptureVisionRouter;
import com.dynamsoft.cvr.CapturedResult;
import com.dynamsoft.dbr.BarcodeResultItem;
import com.dynamsoft.dbr.DecodedBarcodesResult;
import com.dynamsoft.license.LicenseError;
import com.dynamsoft.license.LicenseException;
import com.dynamsoft.license.LicenseManager;

import java.io.File;
import java.util.Scanner;

public class ParameterTuner {

    private static int inputImage(String[] strRef, Scanner scanner) {
        int imageChoice = 0;
        String imagePath = null;
        while (true) {
            System.err.println(
                "\nAvailable Sample Scenarios:\n" +
                "[1] Blurred barcode\n" +
                "[2] Multiple barcodes\n" +
                "[3] Colour Inverted Barcode\n" +
                "[4] Direct Part Marking\n" +
                "[5] Custom image\n"
            );

            System.out.println("Enter the number of the image to test, or provide a full path to your own image:");
            String strChoice = scanner.nextLine();

            try {
                imageChoice = Integer.parseInt(strChoice);
                if (imageChoice == 1) {
                    imagePath = "../../Images/blurry.png";
                } else if (imageChoice == 2) {
                    imagePath = "../../Images/GeneralBarcodes.png";
                } else if (imageChoice == 3) {
                    imagePath = "../../Images/inverted-barcode.png";
                } else if (imageChoice == 4) {
                    imagePath = "../../Images/DPM.png";
                } else if (imageChoice == 5) {
                    System.out.println("Please enter the full path to your custom image:");
                    imagePath = scanner.nextLine();
                } else {
                    System.out.println("Invalid choice. Please try again.");
                    continue;
                }
            } catch (NumberFormatException e) {
                imagePath = strChoice;
            }

            imagePath = imagePath.replaceAll("^\"|\"$", "");
            File file = new File(imagePath);
            if (!file.exists()) {
                System.out.println("The specified image file does not exist. Please try again.");
                continue;
            }

            strRef[0] = imagePath;
            return imageChoice;
        }
    }

    private static void inputTemplate(CaptureVisionRouter cvRouter, int imageChoice, String[] strRef, Scanner scanner) {
        String firstTemplateDescription = null;
        switch (imageChoice) {
            case 1: firstTemplateDescription = "[1] ReadBlurryBarcode.json  (Suitable for blurred barcode)"; break;
            case 2: firstTemplateDescription = "[1] ReadMultipleBarcode.json  (Suitable for multiple barcodes)"; break;
            case 3: firstTemplateDescription = "[1] ReadInvertedBarcode.json  (Suitable for colour inverted barcode)"; break;
            case 4: firstTemplateDescription = "[1] ReadDPM.json  (Suitable for Direct Part Marking barcode)"; break;
            default: firstTemplateDescription = null; break;
        }

        String templatePath = null;
        while (true) {
            System.out.println(
                "\nSelect template for this image:\n" +
                (firstTemplateDescription != null ? firstTemplateDescription + "\n" : "") +
                "[2] ReadBarcodes_Default.json (General purpose settings)\n" +
                "[3] Custom template (Use your own template)\n"
            );

            System.out.println("Enter the number of the template to test, or provide a full path to your own template:");
            String strChoice = scanner.nextLine();

            try {
                int number = Integer.parseInt(strChoice);
                if (number == 1 && firstTemplateDescription != null) {
                    switch (imageChoice) {
                        case 1: templatePath = "../../CustomTemplates/ReadBlurryBarcode.json"; break;
                        case 2: templatePath = "../../CustomTemplates/ReadMultipleBarcode.json"; break;
                        case 3: templatePath = "../../CustomTemplates/ReadInvertedBarcode.json"; break;
                        case 4: templatePath = "../../CustomTemplates/ReadDPM.json"; break;
                        default: templatePath = ""; break;
                    }
                } else if (number == 2) {
                    templatePath = "../../CustomTemplates/ReadBarcodes_Default.json";
                } else if (number == 3) {
                    System.out.println("Please enter the full path to your custom template:");
                    templatePath = scanner.nextLine();
                } else {
                    System.out.println("Invalid choice. Please try again.");
                    continue;
                }
            } catch (NumberFormatException e) {
                templatePath = strChoice;
            }

            templatePath = templatePath.replaceAll("^\"|\"$", "");
            File file = new File(templatePath);
            if (!file.exists()) {
                System.out.println("The specified template file does not exist. Please try again.");
                continue;
            }

            try {
                CaptureVisionError error = cvRouter.initSettingsFromFile(templatePath);
                if (error.getErrorCode() == EnumErrorCode.EC_UNSUPPORTED_JSON_KEY_WARNING) {
                    System.out.println("Warning: " + error.getErrorCode() + ", " + error.getErrorString());
                }
            } catch (CaptureVisionException e) {
                System.out.println("\nFailed to initialize settings, Error: " + e.getErrorCode() + ", " + e.getErrorString() + ". Please try again.");
                continue;
            }

            strRef[0] = templatePath;
            return;
        }
    }

    private static int inputNextState(Scanner scanner) {
        while (true) {
            System.out.println(
                "\nWhat would you like to do next?\n" +
                "[1] Try a different template\n" +
                "[2] Load another image\n" +
                "[3] Exit\n"
            );

            System.out.println("Enter your choice:");
            String strChoice = scanner.nextLine();

            try {
                int number = Integer.parseInt(strChoice);
                if (number >= 1 && number <= 3) {
                    return number;
                } else {
                    System.out.println("Invalid choice. Please try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number between 1 and 3.");
            }
        }
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
                System.out.print("Press Enter to quit...");
                scanner.nextLine();
                return;
            }

            System.out.println("Welcome to ParameterTuner!");

            int state = 2;
            int imageChoice = 0;
            String imagePath = null;
            String templatePath = null;
            String[] strRef = new String[1];
            while (state != 3)
            {
                if (state == 2) {
                    imageChoice = inputImage(strRef, scanner);
                    imagePath = strRef[0];
                }

                CaptureVisionRouter cvRouter = new CaptureVisionRouter();

                inputTemplate(cvRouter, imageChoice, strRef, scanner);
                templatePath = strRef[0];

                long beginTime = System.currentTimeMillis();
                CapturedResult[] results = cvRouter.captureMultiPages(imagePath);
                long endTime = System.currentTimeMillis();
                long timeElapsed = endTime - beginTime;

                System.out.println(
                    "\nRunning with\n" +
                    "Image: " + imagePath + "\n" +
                    "Template: " + templatePath
                );

                if (results == null || results.length == 0) {
                    System.out.println("No captured result.");
                } else {
                    for (int index = 0; index < results.length; index++) {
                        CapturedResult result = results[index];

                        if (result.getErrorCode() == (int)EnumErrorCode.EC_UNSUPPORTED_JSON_KEY_WARNING) {
                            System.out.println("Warning: " + result.getErrorCode() + ", " + result.getErrorString());
                        } else if (result.getErrorCode() != EnumErrorCode.EC_OK) {
                            System.out.println("Error: " + result.getErrorCode() + ", " + result.getErrorString());
                        }

                        DecodedBarcodesResult barcodeResult = result.getDecodedBarcodesResult();
                        BarcodeResultItem[] items = barcodeResult != null ? barcodeResult.getItems() : null;
                        if (items == null || items.length == 0) {
                            System.out.println("Page-" + (index + 1) + " No barcode found.");
                        } else {
                            System.out.println("Page-" + (index + 1) + " Decoded " + items.length + " barcodes:");
                            for (int i = 0; i < items.length; i++) {
                                System.out.println("Result " + (i + 1));
                                System.out.println("Barcode Format: " + items[i].getFormatString());
                                System.out.println("Barcode Text: " + items[i].getText());
                            }
                        }
                    }

                    System.out.println("Time elapsed: " + timeElapsed + " ms...");
                }

                state = inputNextState(scanner);
            }

        } finally {
            scanner.close();
        }
    }
}