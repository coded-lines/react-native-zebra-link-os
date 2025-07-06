import { NativeModule, requireNativeModule } from 'expo';

import { ReactNativeZebraLinkOsModuleEvents } from './ReactNativeZebraLinkOs.types';

declare class ReactNativeZebraLinkOsModule extends NativeModule<ReactNativeZebraLinkOsModuleEvents> {
  PI: number;
  hello(): string;
  setValueAsync(value: string): Promise<void>;
}

// This call loads the native module object from the JSI.
export default requireNativeModule<ReactNativeZebraLinkOsModule>('ReactNativeZebraLinkOs');
