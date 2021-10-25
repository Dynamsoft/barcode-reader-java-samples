# Dynamsoft Barcode Reader samples for the Java edition

This repository contains multiple samples that demonstrates how to use the [Dynamsoft Barcode Reader](https://www.dynamsoft.com/barcode-reader/overview/) Java Edition.

## Requirements
- Operating systems:
  - Windows 7, 8, 10
  - Windows Server 2003, 2008, 2008 R2, 2012
  - Linux x64 (Ubuntu 14.04.4+ LTS, Debian 8+, etc.)
  - macOS x64 10.12+
- JDK 1.7 and above
- Environment: Eclipse 3.7 and above

## Samples

| Sample Name | Description |
| ----------- | ----------- |
| `HelloWorld` | This is a Java sample that illustrates the simplest way to recognize barcodes from images with Dynamsoft Barcode Reader SDK. |
| `GeneralSettings` | This is a Java sample that illustrates how to make general settings (including barcode format/barcode count/scan region) when using Dynamsoft Barcode Reader. | 
| `ImageDecoding` | This is a Java sample that illustrates how to decode images in various format (including Base64/BufferedImage/file bytes/image buffer) when using Dynamsoft Barcode Reader. | 
| `SpeedFirstSettings` | This is a Java sample that shows how to configure Dynamsoft Barcode Reader to read barcodes as fast as possible. The downside is that read-rate and accuracy might be affected. |
| `ReadRateFirstSettings` | This is a Java sample that shows how to configure Dynamsoft Barcode Reader to read as many barcodes as possible at one time. The downside is that speed and accuracy might be affected. It is recommended to apply these configurations when decoding multiple barcodes from a single image. |
| `AccuracyFirstSettings` | This is a Java sample that shows how to configure Dynamsoft Barcode Reader to read barcodes as accurately as possible. The downside is that speed and read-rate might be affected. It is recommended to apply these configurations when misreading is unbearable. |

## License

- If you want to use an offline license, please contact [Dynamsoft Support](https://www.dynamsoft.com/company/contact/)
- You can also request a 30-day trial license in the [customer portal](https://www.dynamsoft.com/customer/license/trialLicense?product=dbr&utm_source=samples&package=java)

## Contact Us

https://www.dynamsoft.com/company/contact/