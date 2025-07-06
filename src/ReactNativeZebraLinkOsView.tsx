import { requireNativeView } from 'expo';
import * as React from 'react';

import { ReactNativeZebraLinkOsViewProps } from './ReactNativeZebraLinkOs.types';

const NativeView: React.ComponentType<ReactNativeZebraLinkOsViewProps> =
  requireNativeView('ReactNativeZebraLinkOs');

export default function ReactNativeZebraLinkOsView(props: ReactNativeZebraLinkOsViewProps) {
  return <NativeView {...props} />;
}
