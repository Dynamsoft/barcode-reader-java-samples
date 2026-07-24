import com.dynamsoft.core.EnumErrorCode;
import com.dynamsoft.core.basic_structures.Point;
import com.dynamsoft.core.basic_structures.Quadrilateral;
import com.dynamsoft.cvr.CaptureVisionException;
import com.dynamsoft.cvr.CaptureVisionRouter;
import com.dynamsoft.cvr.CapturedResult;
import com.dynamsoft.cvr.SimplifiedCaptureVisionSettings;
import com.dynamsoft.dbr.BarcodeResultItem;
import com.dynamsoft.dbr.DecodedBarcodesResult;
import com.dynamsoft.utility.EnumLayoutElementSource;
import com.dynamsoft.utility.EnumLayoutPattern;
import com.dynamsoft.utility.LayoutAnalysisParameter;
import com.dynamsoft.utility.LayoutAnalysisResult;
import com.dynamsoft.utility.LayoutAnalyzer;
import com.dynamsoft.utility.LayoutElement;
import com.dynamsoft.license.LicenseError;
import com.dynamsoft.license.LicenseException;
import com.dynamsoft.license.LicenseManager;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Scalar;
import org.bytedeco.opencv.global.opencv_imgcodecs;
import org.bytedeco.opencv.global.opencv_imgproc;
import org.bytedeco.opencv.global.opencv_highgui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GridBarcodeScanner {

    static final String SAMPLE_IMAGE_PATH = "../../Images/sample_grid.png";
    static final String LIGHTWEIGHT_TEMPLATE_PATH = "templates/GridFastScan.json";
    static final String LIGHTWEIGHT_TEMPLATE_NAME = "GridFastScan";
    static final String DEEP_DECODE_TEMPLATE_PATH = "templates/GridDeepDecode.json";
    static final String DEEP_DECODE_TEMPLATE_NAME = "GridDeepDecode";
    static final String RESULT_DIR = "./Result/";
    static final float SCALE_FACTOR = 2.0f;

    // ===========================
    // Data Structures
    // ===========================

    static class InputParams {
        boolean exit = false;
        String imagePath = "";
    }

    enum DecodeResultType {
        DECODE_FAILED,
        DECODED,
        INFERRED
    }

    static class CommonDecodeResult {
        List<Quadrilateral> locations = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        int error = 0;

        CommonDecodeResult() {}
        CommonDecodeResult(int err) { this.error = err; }

        void prepareForLayoutAnalysis() {
            for (int i = 0; i < locations.size(); i++) {
                locations.get(i).id = i;
            }
        }

        String getText(int id) {
            if (id < 0 || id >= texts.size())
                throw new IndexOutOfBoundsException("Invalid id: " + id);
            return texts.get(id);
        }
    }

    static class GridItem {
        Quadrilateral location;
        String text = "";
        DecodeResultType type = DecodeResultType.INFERRED;

        // Constructed from a decoded barcode (LES_INPUT)
        GridItem(Quadrilateral quad, String text) {
            this.location = new Quadrilateral(quad);
            this.text = text;
            this.type = DecodeResultType.DECODED;
        }

        // Constructed from an inferred layout element (LES_INFERRED)
        GridItem(Quadrilateral quad) {
            this.location = new Quadrilateral(quad);
            this.type = DecodeResultType.INFERRED;
        }
    }

    static class GridResult {
        // sparse 2D map: row -> col -> item
        final Map<Integer, Map<Integer, GridItem>> items = new HashMap<>();
        private final Object lock = new Object();

        GridResult(CommonDecodeResult commonResult, LayoutAnalysisResult layoutResult) {
            int rowCount = layoutResult.elements.length;
            for (int row = 0; row < rowCount; row++) {
                if (layoutResult.elements[row] == null) continue;
                int colCount = layoutResult.elements[row].length;
                for (int col = 0; col < colCount; col++) {
                    LayoutElement element = layoutResult.elements[row][col];
                    if (element == null) continue;
                    if (element.source == EnumLayoutElementSource.LES_INPUT) {
                        try {
                            String text = commonResult.getText(element.quad.id);
                            items.computeIfAbsent(row, k -> new HashMap<>())
                                 .put(col, new GridItem(element.quad, text));
                        } catch (Exception e) {
                            System.err.println("Error retrieving text for quad id "
                                    + element.quad.id + ": " + e.getMessage());
                        }
                    } else if (element.source == EnumLayoutElementSource.LES_INFERRED) {
                        items.computeIfAbsent(row, k -> new HashMap<>())
                             .put(col, new GridItem(element.quad));
                    }
                }
            }
        }

        void updateItem(int row, int col, String text, Quadrilateral quad) {
            synchronized (lock) {
                Map<Integer, GridItem> rowMap = items.get(row);
                if (rowMap == null)
                    throw new IllegalStateException("Row " + row + " not found.");
                GridItem item = rowMap.get(col);
                if (item == null)
                    throw new IllegalStateException("Column " + col + " not found in row " + row + ".");
                item.text = text;
                item.location = new Quadrilateral(quad);
                item.type = DecodeResultType.DECODED;
            }
        }

        void updateItemDecodeFailed(int row, int col) {
            synchronized (lock) {
                Map<Integer, GridItem> rowMap = items.get(row);
                if (rowMap == null)
                    throw new IllegalStateException("Row " + row + " not found.");
                GridItem item = rowMap.get(col);
                if (item == null)
                    throw new IllegalStateException("Column " + col + " not found in row " + row + ".");
                item.text = "";
                item.type = DecodeResultType.DECODE_FAILED;
            }
        }

        String toJson() {
            int totalDecoded = 0;
            int totalInferred = 0;
            StringBuilder gridJson = new StringBuilder();

            List<Integer> rowKeys = new ArrayList<>(items.keySet());
            Collections.sort(rowKeys);
            for (int rowIdx : rowKeys) {
                List<Integer> colKeys = new ArrayList<>(items.get(rowIdx).keySet());
                Collections.sort(colKeys);
                for (int colIdx : colKeys) {
                    GridItem item = items.get(rowIdx).get(colIdx);
                    String status;
                    if (item.type == DecodeResultType.DECODED || item.type == DecodeResultType.INFERRED) {
                        status = "Decoded";
                        totalDecoded++;
                    } else if (item.type == DecodeResultType.DECODE_FAILED) {
                        status = "Inferred";
                        totalInferred++;
                    } else {
                        status = "Failed";
                    }
                    if (gridJson.length() > 0) gridJson.append(",");
                    gridJson.append("\n\t\t{ \"row\": ").append(rowIdx + 1)
                            .append(", \"col\": ").append(colIdx + 1)
                            .append(", \"status\": \"").append(status)
                            .append("\", \"text\": \"").append(escapeJson(item.text)).append("\" }");
                }
            }

            return "{\n"
                    + "\t\"totalDecoded\": " + totalDecoded + ",\n"
                    + "\t\"totalInferred\": " + totalInferred + ",\n"
                    + "\t\"grid\": [" + gridJson + "\n\t]\n"
                    + "}";
        }

        private static String escapeJson(String input) {
            if (input == null) return "";
            StringBuilder sb = new StringBuilder();
            for (char c : input.toCharArray()) {
                switch (c) {
                    case '"':  sb.append("\\\""); break;
                    case '\\': sb.append("\\\\"); break;
                    case '\b': sb.append("\\b");  break;
                    case '\f': sb.append("\\f");  break;
                    case '\n': sb.append("\\n");  break;
                    case '\r': sb.append("\\r");  break;
                    case '\t': sb.append("\\t");  break;
                    default:
                        if (c < 32) sb.append(String.format("\\u%04x", (int) c));
                        else sb.append(c);
                }
            }
            return sb.toString();
        }
    }

    static class DeepDecodeTask {
        final int row;
        final int col;
        final GridItem item;

        DeepDecodeTask(int row, int col, GridItem item) {
            this.row = row;
            this.col = col;
            this.item = item;
        }
    }

    // ===========================
    // ImageShower
    // ===========================

    static class ImageShower {
        private static volatile ImageShower INSTANCE;

        private static boolean isHeadless() {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("mac") || os.contains("darwin")) {
                return true;
            }
            if (os.contains("linux") || os.contains("nix") || os.contains("nux")) {
                String display = System.getenv("DISPLAY");
                return display == null || display.isEmpty();
            }
            return false;
        }

        static ImageShower instance() {
            if (INSTANCE == null) {
                synchronized (ImageShower.class) {
                    if (INSTANCE == null) {
                        INSTANCE = new ImageShower("Grid Barcode Scanner [1/4] Fast Scan", 30);
                    }
                }
            }
            return INSTANCE;
        }

        static void stopIfRunning() {
            if (INSTANCE != null) INSTANCE.stop();
        }

        void update(Mat img, String title) {
            if (headless) return;
            Mat cloned = img.clone();
            synchronized (mutex) {
                if (pendingImg != null) pendingImg.release();
                pendingImg = cloned;
                windowTitle = title;
            }
        }

        void stop() {
            if (headless) return;
            running = false;
            if (thread != null && thread.isAlive()) {
                try { thread.join(3000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
        }

        void setDelay(int ms) { this.delayMs = ms; }

        private ImageShower(String windowName, int delayMs) {
            this.winName = windowName;
            this.delayMs = delayMs;
            this.windowTitle = windowName;
            this.headless = isHeadless();
            this.pendingImg = null;
            if (headless) {
                this.running = false;
                this.thread = null;
            } else {
                this.running = true;
                this.thread = new Thread(this::loop);
                this.thread.setDaemon(true);
                this.thread.start();
            }
        }

        private void loop() {
            opencv_highgui.namedWindow(winName, opencv_highgui.WINDOW_NORMAL);
            opencv_highgui.resizeWindow(winName, 800, 600);
            String lastTitle = windowTitle;
            while (running) {
                Mat frame = null;
                String title;
                synchronized (mutex) {
                    if (pendingImg != null && !pendingImg.empty()) {
                        frame = pendingImg;
                        pendingImg = null;
                    }
                    title = windowTitle;
                }
                if (frame != null) {
                    if (!title.equals(lastTitle)) {
                        opencv_highgui.setWindowTitle(winName, title);
                        lastTitle = title;
                    }
                    org.bytedeco.opencv.opencv_core.Rect rect =
                            opencv_highgui.getWindowImageRect(winName);
                    int winW = rect.width();
                    int winH = rect.height();
                    if (winW > 0 && winH > 0) {
                        double scale = Math.min(
                                (double) winW / frame.cols(),
                                (double) winH / frame.rows());
                        int newW = (int) (frame.cols() * scale);
                        int newH = (int) (frame.rows() * scale);
                        opencv_highgui.resizeWindow(winName, newW, newH);
                    }
                    opencv_highgui.imshow(winName, frame);
                    frame.release();
                }
                int key = opencv_highgui.waitKey(delayMs);
                if (key == 27) running = false;
                try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            }
            opencv_highgui.destroyAllWindows();
        }

        private final String winName;
        private volatile String windowTitle;
        private volatile int delayMs;
        private final Thread thread;
        private final Object mutex = new Object();
        private Mat pendingImg;
        private volatile boolean running;
        private final boolean headless;
    }

    // ===========================
    // ImageHelper
    // ===========================

    static class ImageHelper {
        private final Mat img;
        private final File imagePath;

        ImageHelper(String path) {
            this.img = opencv_imgcodecs.imread(path, opencv_imgcodecs.IMREAD_COLOR);
            if (this.img == null || this.img.empty())
                throw new RuntimeException("Failed to read image from file: " + path);
            this.imagePath = new File(path);
        }

        private ImageHelper(Mat mat, File imagePath) {
            this.img = mat;
            this.imagePath = imagePath;
        }

        int width() { return img.cols(); }
        int height() { return img.rows(); }
        String getImagePath() { return imagePath.getPath(); }

        boolean saveResultImage(CommonDecodeResult result) {
            ImageHelper resultImage = drawSolidQuads(result.locations, new Scalar(0, 255, 0, 255), 2);
            ImageShower.instance().update(resultImage.img, "Grid Barcode Scanner [1/4] Fast Scan");
            return resultImage.saveToResultFile("_phase1");
        }

        boolean saveResultImage(LayoutAnalysisResult layoutResult) {
            List<Quadrilateral> decodeQuads = new ArrayList<>();
            List<Quadrilateral> inferredQuads = new ArrayList<>();
            for (LayoutElement[] row : layoutResult.elements) {
                if (row == null) continue;
                for (LayoutElement element : row) {
                    if (element == null) continue;
                    if (element.source == EnumLayoutElementSource.LES_INPUT)
                        decodeQuads.add(element.quad);
                    else if (element.source == EnumLayoutElementSource.LES_INFERRED)
                        inferredQuads.add(element.quad);
                }
            }
            ImageHelper decodeImage = drawSolidQuads(decodeQuads, new Scalar(0, 255, 0, 255), 2);
            ImageHelper inferredImage = decodeImage.drawDashedQuads(inferredQuads, new Scalar(0, 0, 255, 255), 10, 4);
            ImageShower.instance().update(inferredImage.img, "Grid Barcode Scanner [2/4] Layout Analysis");
            return inferredImage.saveToResultFile("_phase2");
        }

        boolean saveResultImage(GridResult result) {
            List<Quadrilateral> decodeQuads = new ArrayList<>();
            List<Quadrilateral> inferredQuads = new ArrayList<>();
            List<Quadrilateral> undecodedQuads = new ArrayList<>();
            for (Map.Entry<Integer, Map<Integer, GridItem>> rowEntry : result.items.entrySet()) {
                for (Map.Entry<Integer, GridItem> colEntry : rowEntry.getValue().entrySet()) {
                    GridItem item = colEntry.getValue();
                    if (item.type == DecodeResultType.DECODED)
                        decodeQuads.add(item.location);
                    else if (item.type == DecodeResultType.INFERRED)
                        inferredQuads.add(item.location);
                    else if (item.type == DecodeResultType.DECODE_FAILED)
                        undecodedQuads.add(expandQuad(item.location, SCALE_FACTOR));
                }
            }
            ImageHelper decodeImage = drawSolidQuads(decodeQuads, new Scalar(0, 255, 0, 255), 2);
            ImageHelper inferredImage = decodeImage.drawSolidQuads(inferredQuads, new Scalar(0, 255, 0, 255), 2);
            ImageHelper undecodedImage = inferredImage.drawDashedQuads(undecodedQuads, new Scalar(0, 0, 255, 255), 10, 4);
            ImageShower.instance().update(undecodedImage.img, "Grid Barcode Scanner [3/4] Deep Decode");
            return undecodedImage.saveToResultFile("_phase3");
        }

        boolean saveFinalResultImage(GridResult result) {
            List<Quadrilateral> decodeQuads = new ArrayList<>();
            List<Quadrilateral> inferredQuads = new ArrayList<>();
            List<Quadrilateral> undecodedQuads = new ArrayList<>();
            for (Map.Entry<Integer, Map<Integer, GridItem>> rowEntry : result.items.entrySet()) {
                for (Map.Entry<Integer, GridItem> colEntry : rowEntry.getValue().entrySet()) {
                    GridItem item = colEntry.getValue();
                    if (item.type == DecodeResultType.DECODED)
                        decodeQuads.add(item.location);
                    else if (item.type == DecodeResultType.INFERRED)
                        inferredQuads.add(item.location);
                    else if (item.type == DecodeResultType.DECODE_FAILED)
                        undecodedQuads.add(expandQuad(item.location, SCALE_FACTOR));
                }
            }
            ImageHelper decodeImage = drawSolidQuads(decodeQuads, new Scalar(0, 255, 0, 255), 2);
            ImageHelper inferredImage = decodeImage.drawSolidQuads(inferredQuads, new Scalar(0, 255, 0, 255), 2);
            ImageHelper undecodedImage = inferredImage.drawDashedQuads(undecodedQuads, new Scalar(0, 0, 255, 255), 10, 4);
            for (Map.Entry<Integer, Map<Integer, GridItem>> rowEntry : result.items.entrySet()) {
                for (Map.Entry<Integer, GridItem> colEntry : rowEntry.getValue().entrySet()) {
                    GridItem item = colEntry.getValue();
                    if (item.type == DecodeResultType.DECODE_FAILED) continue;
                    org.bytedeco.opencv.opencv_core.Point topLeft = getTopLeft(item.location);
                    undecodedImage.drawText(item.text, topLeft, new Scalar(0, 255, 0, 255), 0.6);
                }
            }
            ImageShower.instance().update(undecodedImage.img, "Grid Barcode Scanner [4/4] Final Result");
            return undecodedImage.saveToResultFile("_final");
        }

        String getResultDir() { return RESULT_DIR + getFileStem(); }

        String getResultJsonPath() { return getResultDir() + "/result.json"; }

        static void createResultDir(String imgPath) {
            String baseName = new File(imgPath).getName();
            int dot = baseName.lastIndexOf('.');
            if (dot > 0) baseName = baseName.substring(0, dot);
            new File(RESULT_DIR + baseName).mkdirs();
        }

        // ----- Private helpers -----

        private String getFileStem() {
            String name = imagePath.getName();
            int dot = name.lastIndexOf('.');
            return (dot > 0) ? name.substring(0, dot) : name;
        }

        private String getFileExtension() {
            String name = imagePath.getName();
            int dot = name.lastIndexOf('.');
            return (dot > 0) ? name.substring(dot) : ".png";
        }

        private boolean saveToResultFile(String suffix) {
            String stem = getFileStem();
            String ext = getFileExtension();
            String resultPath = RESULT_DIR + stem + "/" + stem + suffix + ext;
            return opencv_imgcodecs.imwrite(resultPath, img);
        }

        private ImageHelper drawSolidQuads(List<Quadrilateral> quads, Scalar color, int thickness) {
            Mat dst = img.clone();
            for (Quadrilateral quad : quads) {
                for (int i = 0; i < 4; i++) {
                    org.bytedeco.opencv.opencv_core.Point p1 = new org.bytedeco.opencv.opencv_core.Point(
                            quad.points[i].getX(), quad.points[i].getY());
                    org.bytedeco.opencv.opencv_core.Point p2 = new org.bytedeco.opencv.opencv_core.Point(
                            quad.points[(i + 1) % 4].getX(), quad.points[(i + 1) % 4].getY());
                    opencv_imgproc.line(dst, p1, p2, color, thickness, opencv_imgproc.LINE_8, 0);
                }
            }
            return new ImageHelper(dst, imagePath);
        }

        private ImageHelper drawDashedQuads(List<Quadrilateral> quads, Scalar color, int dashLength, int thickness) {
            Mat dst = img.clone();
            for (Quadrilateral quad : quads) {
                for (int i = 0; i < 4; i++) {
                    org.bytedeco.opencv.opencv_core.Point p1 = new org.bytedeco.opencv.opencv_core.Point(
                            quad.points[i].getX(), quad.points[i].getY());
                    org.bytedeco.opencv.opencv_core.Point p2 = new org.bytedeco.opencv.opencv_core.Point(
                            quad.points[(i + 1) % 4].getX(), quad.points[(i + 1) % 4].getY());
                    drawDashedLine(dst, p1, p2, color, dashLength, thickness);
                }
            }
            return new ImageHelper(dst, imagePath);
        }

        private static void drawDashedLine(
                Mat dst,
                org.bytedeco.opencv.opencv_core.Point p1,
                org.bytedeco.opencv.opencv_core.Point p2,
                Scalar color, int dashLength, int thickness) {
            double dx = p2.x() - p1.x();
            double dy = p2.y() - p1.y();
            int steps = (int) Math.sqrt(dx * dx + dy * dy);
            if (steps == 0) return;
            boolean draw = true;
            int count = 0;
            for (int i = 0; i <= steps; i++) {
                double t = (double) i / steps;
                int x = (int) (p1.x() + t * dx);
                int y = (int) (p1.y() + t * dy);
                if (draw) {
                    opencv_imgproc.circle(dst,
                            new org.bytedeco.opencv.opencv_core.Point(x, y),
                            Math.max(1, thickness / 2), color, -1, opencv_imgproc.LINE_AA, 0);
                }
                if (++count == dashLength) { count = 0; draw = !draw; }
            }
        }

        private static org.bytedeco.opencv.opencv_core.Point getTopLeft(Quadrilateral quad) {
            int minX = quad.points[0].getX();
            int minY = quad.points[0].getY();
            for (int i = 1; i < 4; i++) {
                if (quad.points[i].getX() < minX) minX = quad.points[i].getX();
                if (quad.points[i].getY() < minY) minY = quad.points[i].getY();
            }
            return new org.bytedeco.opencv.opencv_core.Point(minX, minY);
        }

        private void drawText(String text, org.bytedeco.opencv.opencv_core.Point org,
                              Scalar color, double scale) {
            opencv_imgproc.putText(img, text, org,
                    opencv_imgproc.FONT_HERSHEY_SIMPLEX, scale, color, 2, opencv_imgproc.LINE_AA, false);
        }
    }

    // ===========================
    // Helper Functions
    // ===========================

    static Quadrilateral expandQuad(Quadrilateral quad, float scale) {
        float cx = 0, cy = 0;
        for (Point p : quad.points) {
            cx += p.getX();
            cy += p.getY();
        }
        cx /= 4;
        cy /= 4;
        Point[] newPoints = new Point[4];
        for (int i = 0; i < 4; i++) {
            int nx = (int) (cx + (quad.points[i].getX() - cx) * scale);
            int ny = (int) (cy + (quad.points[i].getY() - cy) * scale);
            newPoints[i] = new Point(nx, ny);
        }
        return new Quadrilateral(newPoints[0], newPoints[1], newPoints[2], newPoints[3]);
    }

    static String getResourceContent(String resourcePath) throws IOException {
    InputStream is = Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(resourcePath);
    if (is == null) {
        throw new IOException("Resource not found in classpath: " + resourcePath);
    }

    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }

        return sb.toString();
    }
    }

    // ===========================
    // Input / Welcome
    // ===========================

    static InputParams welcome(Scanner scanner) {
        InputParams params = new InputParams();
        System.out.println("Grid Barcode Scanner!");
        System.out.println("===========================");
        System.out.println();
        System.out.println("Image path : [press Enter to use sample image (sample_grid.png)]");
        System.out.println("'Q'/'q' to quit");
        String input = scanner.nextLine().trim();
        if (input.equals("Q") || input.equals("q")) {
            params.exit = true;
            return params;
        }
        if (input.isEmpty()) {
            params.imagePath = SAMPLE_IMAGE_PATH;
        } else {
            if (input.length() >= 2 && input.charAt(0) == '"' && input.charAt(input.length() - 1) == '"')
                input = input.substring(1, input.length() - 1);
            params.imagePath = input;
        }

        return params;
    }

    // ===========================
    // Phase 1: Fast Scan
    // ===========================

    static CommonDecodeResult commonDecode(ImageHelper imageHelper, String templatePath, String templateName) {
        CaptureVisionRouter cvRouter = new CaptureVisionRouter();
        try {
            String settings = getResourceContent(templatePath);
            cvRouter.initSettings(settings);
        }
        catch (IOException e) {
            System.out.println("Failed to read template file: " + e.getMessage());
            return new CommonDecodeResult(-1);
        }
        catch (CaptureVisionException e) {
            System.out.println("Failed to initialize settings from file: ErrorCode: "
                    + e.getErrorCode() + ", ErrorString: " + e.getErrorString());
            return new CommonDecodeResult(e.getErrorCode());
        }

        long start = System.currentTimeMillis();
        CapturedResult result = cvRouter.capture(imageHelper.getImagePath(), templateName);
        long elapsed = System.currentTimeMillis() - start;

        if (result.getErrorCode() == EnumErrorCode.EC_UNSUPPORTED_JSON_KEY_WARNING) {
            System.out.println("Common decode warning: " + result.getErrorCode() + ", " + result.getErrorString());
        } else if (result.getErrorCode() != EnumErrorCode.EC_OK && result.getErrorCode() != EnumErrorCode.EC_TIMEOUT) {
            System.out.println("Common decode error: " + result.getErrorCode() + ", " + result.getErrorString());
            return new CommonDecodeResult(result.getErrorCode());
        }

        CommonDecodeResult decodeResult = new CommonDecodeResult();
        DecodedBarcodesResult barcodeResult = result.getDecodedBarcodesResult();
        BarcodeResultItem[] items = barcodeResult != null ? barcodeResult.getItems() : null;
        if (items != null) {
            for (BarcodeResultItem item : items) {
                decodeResult.locations.add(item.getLocation());
                String text = item.getText();
                decodeResult.texts.add(text != null ? text : "");
            }
        }

        System.out.println("[Phase 1] Fast scan: " + decodeResult.texts.size()
                + " barcodes decoded in " + elapsed + "ms.");
        return decodeResult;
    }

    // ===========================
    // Phase 2: Layout Analysis
    // ===========================

    static LayoutAnalysisResult analyze(CommonDecodeResult decodeResult, ImageHelper imageHelper) {
        Quadrilateral[] quads = decodeResult.locations.toArray(new Quadrilateral[0]);

        LayoutAnalysisParameter layoutParam = new LayoutAnalysisParameter();
        layoutParam.pattern = EnumLayoutPattern.LP_MATRIX;
        layoutParam.inputImageWidth = imageHelper.width();
        layoutParam.inputImageHeight = imageHelper.height();

        LayoutAnalysisResult layoutResult = LayoutAnalyzer.analyze(quads, layoutParam);
        if (layoutResult == null || layoutResult.errorCode != EnumErrorCode.EC_OK) {
            System.out.println("Layout analysis failed: ErrorCode: "
                    + (layoutResult != null ? layoutResult.errorCode : -1));
            return null;
        }

        int rowCount = layoutResult.elements.length;
        int colCount = (rowCount > 0 && layoutResult.elements[0] != null)
                ? layoutResult.elements[0].length : 0;
        int inferredCount = layoutResult.inferredQuads != null ? layoutResult.inferredQuads.length : 0;

        System.out.println();
        System.out.println("[Phase 2] Layout analysis: " + (rowCount * colCount)
                + " grid positions (" + rowCount + "x" + colCount + "). "
                + inferredCount + " inferred regions.");
        return layoutResult;
    }

    // ===========================
    // Phase 3: Deep Decode
    // ===========================

    static void deepDecodeInner(ImageHelper imageHelper, String templatePath,
                                GridResult gridResult, DeepDecodeTask task, String templateName) {
        CaptureVisionRouter cvRouter = new CaptureVisionRouter();
        try {
            String settings = getResourceContent(templatePath);
            cvRouter.initSettings(settings);
        }
        catch (IOException e) {
            System.out.println("Failed to read template file: " + e.getMessage());
            gridResult.updateItemDecodeFailed(task.row, task.col);
            return;
        }
        catch (CaptureVisionException e) {
            gridResult.updateItemDecodeFailed(task.row, task.col);
            return;
        }

        try {
            SimplifiedCaptureVisionSettings settings = cvRouter.getSimplifiedSettings(templateName);
            settings.roi = expandQuad(task.item.location, SCALE_FACTOR);
            settings.roiMeasuredInPercentage = 0;
            cvRouter.updateSettings(templateName, settings);
        } catch (CaptureVisionException e) {
            gridResult.updateItemDecodeFailed(task.row, task.col);
            return;
        }

        CapturedResult result = cvRouter.capture(imageHelper.getImagePath(), templateName);
        if (result.getErrorCode() != EnumErrorCode.EC_OK) {
            gridResult.updateItemDecodeFailed(task.row, task.col);
            return;
        }

        DecodedBarcodesResult barcodeResult = result.getDecodedBarcodesResult();
        BarcodeResultItem[] items = barcodeResult != null ? barcodeResult.getItems() : null;
        if (items != null && items.length > 0) {
            String text = items[0].getText();
            if (text != null) {
                gridResult.updateItem(task.row, task.col, text, items[0].getLocation());
            } else {
                gridResult.updateItemDecodeFailed(task.row, task.col);
            }
            return;
        }
        gridResult.updateItemDecodeFailed(task.row, task.col);
    }

    static void deepDecode(ImageHelper imageHelper, String templatePath,
                           GridResult gridResult, String templateName) {
        List<DeepDecodeTask> tasks = new ArrayList<>();
        for (Map.Entry<Integer, Map<Integer, GridItem>> rowEntry : gridResult.items.entrySet()) {
            for (Map.Entry<Integer, GridItem> colEntry : rowEntry.getValue().entrySet()) {
                if (colEntry.getValue().type == DecodeResultType.INFERRED) {
                    tasks.add(new DeepDecodeTask(rowEntry.getKey(), colEntry.getKey(), colEntry.getValue()));
                }
            }
        }

        if (tasks.isEmpty()) return;

        int numThreads = Math.max(1, Runtime.getRuntime().availableProcessors());
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        long start = System.currentTimeMillis();

        final ImageHelper finalImageHelper = imageHelper;
        final String finalTemplatePath = templatePath;
        final String finalTemplateName = templateName;
        for (final DeepDecodeTask task : tasks) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        deepDecodeInner(finalImageHelper, finalTemplatePath, gridResult, task, finalTemplateName);
                    } catch (Exception e) {
                        System.err.println("Error in deep decode for row " + task.row
                                + ", col " + task.col + ": " + e.getMessage());
                    }
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long elapsed = System.currentTimeMillis() - start;
        int decodeCount = tasks.size();
        for (DeepDecodeTask task : tasks) {
            Map<Integer, GridItem> rowMap = gridResult.items.get(task.row);
            if (rowMap != null) {
                GridItem item = rowMap.get(task.col);
                if (item != null && item.type == DecodeResultType.DECODE_FAILED) {
                    decodeCount--;
                }
            }
        }

        System.out.println();
        System.out.println("[Phase 3] Deep decode: " + decodeCount + " / " + tasks.size()
                + " inferred regions decoded in " + elapsed + "ms.");
    }

    // ===========================
    // Result Output
    // ===========================

    static void printResult(GridResult gridResult, String jsonPath) {
        System.out.println("============================================================");
        System.out.printf(" %-4s | %-5s | %-13s | %s%n", "Row", "Col", "Status", "Text");
        System.out.println("============================================================");
        int total = 0, decoded = 0;

        List<Integer> rowKeys = new ArrayList<>(gridResult.items.keySet());
        Collections.sort(rowKeys);
        for (int rowIdx : rowKeys) {
            List<Integer> colKeys = new ArrayList<>(gridResult.items.get(rowIdx).keySet());
            Collections.sort(colKeys);
            for (int colIdx : colKeys) {
                total++;
                GridItem item = gridResult.items.get(rowIdx).get(colIdx);
                String status;
                switch (item.type) {
                    case DECODE_FAILED:
                        status = "Inferred";
                        break;
                    case DECODED:
                    case INFERRED:
                        status = "Decoded";
                        decoded++;
                        break;
                    default:
                        status = "Unknown";
                }
                System.out.printf("  %-3d | %-5d | %-13s | %s%n",
                        rowIdx + 1, colIdx + 1, status, item.text);
            }
        }

        System.out.println("============================================================");
        System.out.println("[Done] Total decoded: " + decoded + " / " + total + ".");

        try (PrintWriter pw = new PrintWriter(new FileWriter(jsonPath))) {
            pw.print(gridResult.toJson());
            System.out.println("Detailed results saved to result.json.");
        } catch (IOException e) {
            System.err.println("Failed to save detailed results to result.json: " + e.getMessage());
        }
    }

    // ===========================
    // Main
    // ===========================

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        int errorCode = 0;
        String errorMsg = "";

        // Initialize license.
        // You can request and extend a trial license from https://www.dynamsoft.com/customer/license/trialLicense?product=dcv&utm_source=samples&package=java
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
            System.out.println("License initialization failed: ErrorCode: " + errorCode
                    + ", ErrorString: " + errorMsg);
            return;
        }

        try {
            while (true) {
                try {
                    InputParams params = welcome(scanner);
                    if (params.exit) break;

                    ImageHelper imageHelper = new ImageHelper(params.imagePath);
                    ImageHelper.createResultDir(params.imagePath);

                    // Phase 1: Fast scan with lightweight template
                    CommonDecodeResult commonDecodeResult = commonDecode(
                            imageHelper, LIGHTWEIGHT_TEMPLATE_PATH, LIGHTWEIGHT_TEMPLATE_NAME);
                    if (commonDecodeResult.error != EnumErrorCode.EC_OK) continue;

                    commonDecodeResult.prepareForLayoutAnalysis();
                    imageHelper.saveResultImage(commonDecodeResult);

                    System.out.print(" Press Enter for Layout Analysis...");
                    scanner.nextLine();

                    // Phase 2: Layout analysis
                    LayoutAnalysisResult layoutResult = analyze(commonDecodeResult, imageHelper);
                    if (layoutResult == null) continue;

                    GridResult gridResult = new GridResult(commonDecodeResult, layoutResult);
                    imageHelper.saveResultImage(layoutResult);

                    System.out.print(" Press Enter for Deep Decode...");
                    scanner.nextLine();

                    // Phase 3: Deep decode inferred regions with deep decode template
                    deepDecode(imageHelper, DEEP_DECODE_TEMPLATE_PATH, gridResult, DEEP_DECODE_TEMPLATE_NAME);
                    imageHelper.saveResultImage(gridResult);

                    System.out.print(" Press Enter to view final result...");
                    scanner.nextLine();

                    // Phase 4: Print and save final result
                    imageHelper.saveFinalResultImage(gridResult);
                    String jsonPath = imageHelper.getResultJsonPath();
                    printResult(gridResult, jsonPath);

                    System.out.print("Press Enter for next image (or 'Q'/'q' to quit)...");
                    String input = scanner.nextLine().trim();
                    if (input.equals("Q") || input.equals("q")) break;

                } catch (Exception ex) {
                    System.out.println("An error occurred: " + ex.getMessage());
                    break;
                }
            }
        } finally {
            ImageShower.stopIfRunning();
            scanner.close();
        }
    }
}