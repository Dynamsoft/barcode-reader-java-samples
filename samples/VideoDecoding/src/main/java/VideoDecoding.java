import com.dynamsoft.core.*;
import com.dynamsoft.core.basic_structures.FileImageTag;
import com.dynamsoft.core.basic_structures.ImageData;
import com.dynamsoft.core.basic_structures.ImageSourceAdapter;
import com.dynamsoft.core.basic_structures.ImageTag;
import com.dynamsoft.cvr.CaptureVisionException;
import com.dynamsoft.cvr.CaptureVisionRouter;
import com.dynamsoft.cvr.CapturedResultReceiver;
import com.dynamsoft.cvr.EnumPresetTemplate;
import com.dynamsoft.dbr.BarcodeResultItem;
import com.dynamsoft.dbr.DecodedBarcodesResult;
import com.dynamsoft.license.LicenseError;
import com.dynamsoft.license.LicenseException;
import com.dynamsoft.license.LicenseManager;
import com.dynamsoft.utility.MultiFrameResultCrossFilter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import static org.bytedeco.opencv.global.opencv_highgui.*;
import static org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FRAME_HEIGHT;
import static org.bytedeco.opencv.global.opencv_videoio.CAP_PROP_FRAME_WIDTH;

class MyCapturedResultReceiver extends CapturedResultReceiver {

    @Override
    public void onDecodedBarcodesReceived(DecodedBarcodesResult result) {
        if (result.getErrorCode() == EnumErrorCode.EC_UNSUPPORTED_JSON_KEY_WARNING) {
            System.out.println("Warning: " + result.getErrorCode() + ", " + result.getErrorString());
        } else if (result.getErrorCode() != EnumErrorCode.EC_OK) {
            System.out.println("Error: " + result.getErrorString());
        }

        ImageTag tag = result.getOriginalImageTag();
        if (tag != null) {
            System.out.println("ImageId: " + tag.getImageId());
        }
        BarcodeResultItem[] items = result.getItems();
        System.out.println("Decoded " + items.length + " barcodes.");
        for (int index = 0; index < items.length; index++) {
            BarcodeResultItem item = items[index];
            System.out.println("Result " + index + 1);
            System.out.println("Barcode Format: " + item.getFormatString());
            System.out.println("Barcode Text: " + item.getText());
        }
    }
}

class MyVideoFetcher extends ImageSourceAdapter {

    @Override
    public boolean hasNextImageToFetch() {
        return true;
    }
}

public class VideoDecoding {
    private static void decodeVideo(boolean useVideoFile, String videoFilePath) {
        CaptureVisionRouter cvRouter = new CaptureVisionRouter();
        MyVideoFetcher fetcher = new MyVideoFetcher();

        try {
            fetcher.setMaxImageCount(100);
            fetcher.setBufferOverflowProtectionMode(EnumBufferOverflowProtectionMode.BOPM_UPDATE);
            fetcher.setColourChannelUsageType(EnumColourChannelUsageType.CCUT_AUTO);
            cvRouter.setInput(fetcher);

            MultiFrameResultCrossFilter filter = new MultiFrameResultCrossFilter();
            filter.enableResultCrossVerification(EnumCapturedResultItemType.CRIT_BARCODE, true);
            filter.enableResultDeduplication(EnumCapturedResultItemType.CRIT_BARCODE, true);;
            filter.setDuplicateForgetTime(EnumCapturedResultItemType.CRIT_BARCODE, 5000);
            cvRouter.addResultFilter(filter);

            MyCapturedResultReceiver receiver = new MyCapturedResultReceiver();
            cvRouter.addResultReceiver(receiver);
        } catch (CaptureVisionException ignored) {
        }

        try {
            cvRouter.startCapturing(EnumPresetTemplate.PT_READ_BARCODES, false);
        } catch (CaptureVisionException e) {
            System.out.println("Error: " + e.getErrorString());
            return;
        }

        try (VideoCapture vc = !useVideoFile ? new VideoCapture(0) : new VideoCapture(videoFilePath)) {
            int videoWidth = (int) vc.get(CAP_PROP_FRAME_WIDTH);
            int videoHeight = (int) vc.get(CAP_PROP_FRAME_HEIGHT);
            vc.set(CAP_PROP_FRAME_WIDTH, videoWidth);
            vc.set(CAP_PROP_FRAME_HEIGHT, videoHeight);

            if (!vc.isOpened()) {
                cvRouter.stopCapturing();
                return;
            }

            String windowName = "Video Barcode Reader";

            int imageId = 0;
            Mat frame = new Mat();
            while (true) {
                imageId++;
                boolean rval = vc.read(frame);
                if (!rval) {
                    break;
                }

                FileImageTag tag = new FileImageTag("", 0, 0);
                tag.setImageId(imageId);
                byte[] byteArray = new byte[(int) (frame.total() * frame.channels())];
                frame.data().get(byteArray);
                ImageData image = new ImageData(byteArray, frame.cols(), frame.rows(), (int) frame.step(), EnumImagePixelFormat.IPF_RGB_888, 0, tag);
                fetcher.addImageToBuffer(image);

                imshow(windowName, frame);
                int key = waitKey(1);
                if (key == 27)
                    break;
            }

            cvRouter.stopCapturing();
            destroyWindow(windowName);
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("-------------------start------------------------");

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

            boolean useVideoFile = false;
            String videoFilePath = "";
            while (true) {
                try {
                    System.out.println(">> Choose a Mode Number:");
                    System.out.println("1. Decode video from camera.");
                    System.out.println("2. Decode video from file.");
                    System.out.println(">> 1 or 2:");
                    int mode = scanner.nextInt();
                    scanner.nextLine();

                    if (mode == 1 || mode == 2) {
                        if (mode == 1) {
                            useVideoFile = false;
                        } else {
                            useVideoFile = true;
                            while (true) {
                                System.out.println(">> Input your video full path:");
                                videoFilePath = scanner.nextLine();
                                videoFilePath = videoFilePath.replaceAll("^\"|\"$", "");
                                if (Files.exists(Paths.get(videoFilePath))) {
                                    break;
                                }
                                System.out.println("Error: File not found");
                            }
                        }
                        break;
                    }
                } catch (Exception ignored) {
                }

                System.out.println("Error: Wrong input.");
            }

            decodeVideo(useVideoFile, videoFilePath);

        } finally {
            System.out.println("-------------------over------------------------");
            System.out.print("Press Enter to quit...");
            scanner.nextLine();
            scanner.close();
        }
    }
}