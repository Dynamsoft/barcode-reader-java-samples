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

import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;

import java.awt.GraphicsEnvironment;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import org.bytedeco.javacv.CanvasFrame;
import static org.bytedeco.opencv.global.opencv_highgui.*;

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

        CanvasFrame canvas = null;
        VideoCapture vc = null;
        FFmpegFrameGrabber grabber = null;
        OpenCVFrameConverter.ToMat converter = null;
        String windowName = "Video Barcode Reader";
        try {
            if (useVideoFile) {
                avutil.av_log_set_level(avutil.AV_LOG_QUIET);
                grabber = new FFmpegFrameGrabber(videoFilePath);
                try {
                    grabber.start();
                } catch (Exception e) {
                    System.out.println("Error: " + e.getMessage());
                    return;
                }
            } else {
                vc = new VideoCapture(0);
                if (!vc.isOpened()) {
                    System.out.println("Error: Cannot open camera.");
                    return;
                }
            }

            boolean isHeadless = GraphicsEnvironment.isHeadless();
            if (!isHeadless) {
                canvas = new CanvasFrame(windowName, 1);
                canvas.setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);
                canvas.setAlwaysOnTop(true);
                canvas.setVisible(true);
                canvas.setAlwaysOnTop(false);
            }

            int imageId = 0;
            Frame frame;
            converter = new OpenCVFrameConverter.ToMat();
            while (canvas == null || canvas.isVisible()) {
                imageId++;
                if (grabber != null) {
                    frame = grabber.grab();
                } else if (vc != null) {
                    Mat mat = new Mat();
                    if (!vc.read(mat)) {
                        break;
                    }

                    if (mat.empty()) {
                        System.out.println("Error: Frame is empty.");
                        break;
                    }

                    frame = converter.convert(mat);
                } else {
                    System.out.println("Error: No video source available.");
                    break;
                }

                if (frame == null) {
                    break;
                } else if (frame.image == null) {
                    continue;
                }

                byte[] byteArray;
                Buffer buffer = frame.image[0];
                if (buffer instanceof ByteBuffer) {
                    ByteBuffer byteBuffer = (ByteBuffer)buffer;
                    byteBuffer.rewind();
                    byteArray = new byte[byteBuffer.remaining()];
                    byteBuffer.get(byteArray);
                } else {
                    System.out.println("Error: Frame is not a ByteBuffer.");
                    break;
                }

                FileImageTag tag = new FileImageTag("", 0, 0);
                tag.setImageId(imageId);
                ImageData image = new ImageData(byteArray, frame.imageWidth, frame.imageHeight, frame.imageStride, EnumImagePixelFormat.IPF_RGB_888, 0, tag);
                fetcher.addImageToBuffer(image);

                if (canvas != null) {
                    canvas.showImage(frame);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (canvas != null) {
                if (canvas.isVisible()) {
                    canvas.setVisible(false);
                }
                canvas.dispose();
            }

            if (converter != null) {
                converter.close();
            }

            try {
                if (useVideoFile && grabber != null) {
                    grabber.stop();
                    grabber.release();
                } else if (vc != null) {
                    vc.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            cvRouter.stopCapturing();
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
                    String strChoice = scanner.nextLine();
                    if (strChoice.trim().isEmpty()) {
                        continue;
                    }

                    boolean choosedMode = false;
                    try {
                        int mode = Integer.parseInt(strChoice.trim());
                        if (mode == 1 || mode == 2) {
                            useVideoFile = mode == 2;
                            choosedMode = true;
                        }
                    } catch (NumberFormatException e) {
                        videoFilePath = strChoice.replaceAll("^\"|\"$", "");
                        useVideoFile = true;
                    }

                    if (useVideoFile) {
                        do {
                            if (videoFilePath.isEmpty()) {
                                System.out.println(">> Input your video full path:");
                                videoFilePath = scanner.nextLine();
                                if (videoFilePath.trim().isEmpty()) {
                                    continue;
                                }
                                videoFilePath = videoFilePath.replaceAll("^\"|\"$", "");
                            }
                            if (Files.exists(Paths.get(videoFilePath))) {
                                break;
                            }
                            System.out.println("Error: File not found");
                            videoFilePath = "";
                        } while (choosedMode);
                    }

                    if ((!useVideoFile && choosedMode) || !videoFilePath.isEmpty()) {
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