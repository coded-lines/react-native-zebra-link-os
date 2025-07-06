import { registerWebModule, NativeModule } from 'expo';

import { ReactNativeZebraLinkOsModuleEvents } from './ReactNativeZebraLinkOs.types';

class ReactNativeZebraLinkOsModule extends NativeModule<ReactNativeZebraLinkOsModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
}

export default registerWebModule(ReactNativeZebraLinkOsModule, 'ReactNativeZebraLinkOsModule');
