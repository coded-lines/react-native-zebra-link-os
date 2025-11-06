import { NativeModule, requireNativeModule } from "expo";
import { UsbDeviceInfo } from ".";

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
   * Reset printer USB permissions.
   * @throws {ZebraError}
   */
  resetUsbPermissions(): Promise<"OK">;
}

export default requireNativeModule<ReactNativeZebraLinkOsModule>(
  "ReactNativeZebraLinkOs",
);
