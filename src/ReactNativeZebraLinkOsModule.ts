import { NativeModule, requireNativeModule } from "expo";
import { UsbDeviceInfo, PrintConfirmResult, BatchPrintResult } from ".";

export type ZebraError = {
  code: string; // one of the documented error codes
  message: string; // human-readable message
};

declare class ReactNativeZebraLinkOsModule extends NativeModule {
  /**
   * Discover and connect to a Zebra USB printer.
   * @throws {ZebraError}
   */
  usbFindAndConnect(): Promise<UsbDeviceInfo>;

  /**
   * Download a raw template to the printer over USB.
   * @param base64Data Base64-encoded template bytes
   * @throws {ZebraError}
   */
  downloadTemplate(base64Data: string): Promise<"OK">;

  /**
   * Print a stored template, substituting the given variables.
   * @param path Path to the stored format on the printer
   * @param data Map from variable-index to replacement string
   * @throws {ZebraError}
   */
  printStoredFormat(path: string, data: Record<number, string>): Promise<"OK">;

  /**
   * Download a TTF font file to the printer.
   * @param path Destination path on the printer
   * @param base64Data Base64-encoded font file
   * @throws {ZebraError}
   */
  downloadTtfFont(path: string, base64Data: string): Promise<"OK">;

  /**
   * Send a ZPL template to the printer and confirm it printed.
   * Checks printer readiness before sending, polls status after,
   * and compares label counters to determine the result.
   * @param base64Data Base64-encoded ZPL bytes
   * @throws {ZebraError} if printer is not ready or connection fails
   */
  printAndConfirm(base64Data: string): Promise<PrintConfirmResult>;

  /**
   * Send all ZPL tickets in one connection burst, poll once after all are sent,
   * and compare the label counter diff to the expected count.
   * @param base64Tickets Array of Base64-encoded ZPL strings, one per ticket
   * @throws {ZebraError} if printer is not reachable or write fails
   */
  printBatch(base64TicketsJson: string): Promise<BatchPrintResult>;

  /**
   * Reset printer USB permissions.
   * @throws {ZebraError}
   */
  resetUsbPermissions(): Promise<"OK">;
}

export default requireNativeModule<ReactNativeZebraLinkOsModule>(
  "ReactNativeZebraLinkOs",
);
