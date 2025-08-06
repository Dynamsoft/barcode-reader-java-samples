# Dynamsoft Barcode Reader samples for the Java edition

This repository contains multiple samples that demonstrates how to use the [Dynamsoft Barcode Reader](https://www.dynamsoft.com/barcode-reader/overview/) Java Edition.

## Requirements
- Operating systems:
  - Windows Windows: Windows 8 and higher, or Windows Server 2012 and higher
  - Linux x64 (Ubuntu 14.04.4+ LTS, Debian 8+, CentOS 7+, etc.)
  - Linux arm 64bit
  - macOS universal 10.15+
- JDK 1.8 and above

## Samples

| Sample Name | Description |
| ----------- | ----------- |
| [`HelloWorld`](samples/HelloWorld) | This is a Java sample that illustrates the simplest way to recognize barcodes from images with Dynamsoft Barcode Reader SDK. |
| [`VideoDecoding`](samples/VideoDecoding) | This sample demonstrates how to read barcodes from video frames. |
| [`GeneralSettings`](samples/GeneralSettings) | This sample demonstrates how to configure general used settings and read barcodes from an image file. |
| [`ReadDPMBarcode`](samples/ReadDPMBarcode) | This sample demonstrates how to read DPM (Direct Part Marking) barcodes and get barcode results. |
| [`ParameterTuner`](samples/ParameterTuner) | This sample demonstrates how to adjust and test different parameter settings to optimize barcode recognition performance. |
| [`ShowLocalizedVSDecodedBarcodes`](samples/ShowLocalizedVSDecodedBarcodes) | This sample demonstrates how to highlight successfully decoded and only-localized barcodes with different styles of rectangles. |

### Additional Samples using Capture Vision SDK

In addition to the classic barcode decoding samples listed above, the following samples go a step further by parsing the decoded results and showcasing more structured workflows.

> [!IMPORTANT]
> These samples use the `Dynamsoft Capture Vision` package instead of `Dynamsoft Barcode Reader`. If you're switching to these samples, make sure to [download](https://www.dynamsoft.com/capture-vision/confirmation/#desktop) and use the correct package.

| Sample | Description |
| --- | --- |
| [`DriverLicenseScanner`](https://github.com/Dynamsoft/capture-vision-java-samples/blob/main/Samples/DriverLicenseScanner) | Shows how to capture and extract user's information from driver license/ID. |
| [`VINScanner`](https://github.com/Dynamsoft/capture-vision-java-samples/blob/main/Samples/VINScanner) | Shows how to capture and extract vehicle's information from Vehicle Identification Number (VIN). |
| [`GS1AIScanner`](https://github.com/Dynamsoft/capture-vision-java-samples/blob/main/Samples/GS1AIScanner) | Shows how to extract and interpret GS1 Application Identifiers (AIs) from GS1 barcodes. |

## License

The library requires a license to work, you use the API initLicense to initialize license key and activate the SDK.

These samples use a free public trial license which require network connection to function. You can request a 30-day free trial license key from <a href="https://www.dynamsoft.com/customer/license/trialLicense?architecture=dcv&product=dbr&utm_source=samples&package=java" target="_blank">Customer Portal</a> which works offline.


## Contact Us

https://www.dynamsoft.com/company/contact/
