// Reexport the native module. On web, it will be resolved to ReactNativeZebraLinkOsModule.web.ts
// and on native platforms to ReactNativeZebraLinkOsModule.ts
export { default } from "./ReactNativeZebraLinkOsModule";

// Generic shapes
export type ZebraErrorCode =
  | UsbFindAndConnectErrorCode
  | DownloadTemplateErrorCode
  | PrintStoredFormatErrorCode
  | DownloadTtfFontErrorCode;

export interface ZebraNativeError extends Error {
  code: ZebraErrorCode;
}

export type UsbFindAndConnectErrorCode =
  | "NO_CONTEXT"
  | "NO_USB_MANAGER"
  | "NO_PRINTER"
  | "NO_PERMISSION_INTENT"
  | "PERMISSION_IN_PROGRESS";
// | 'PERMISSION_DENIED'
// | 'PERMISSION_TIMEOUT'

export interface UsbDeviceInfo {
  success: true;
  deviceName: string;
  vendorId: number;
  productId: number;
}

export type DownloadTemplateErrorCode =
  | "INVALID_DATA"
  | "DECODE_FAILED"
  | "NO_PRINTER"
  | "CONNECTION_OPEN_FAILED"
  | "WRITE_FAILED";

export type PrintStoredFormatErrorCode =
  | "INVALID_PATH"
  | "NO_PRINTER"
  | "CONNECTION_OPEN_FAILED"
  | "PRINTER_INIT_FAILED"
  | "PRINT_FAILED";

export type DownloadTtfFontErrorCode =
  | "INVALID_PATH"
  | "INVALID_DATA"
  | "DECODE_FAILED"
  | "NO_PRINTER"
  | "CONNECTION_OPEN_FAILED"
  | "PRINTER_INIT_FAILED"
  | "INVALID_PRINTER"
  | "DOWNLOAD_FAILED";
