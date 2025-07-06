import * as React from 'react';

import { ReactNativeZebraLinkOsViewProps } from './ReactNativeZebraLinkOs.types';

export default function ReactNativeZebraLinkOsView(props: ReactNativeZebraLinkOsViewProps) {
  return (
    <div>
      <iframe
        style={{ flex: 1 }}
        src={props.url}
        onLoad={() => props.onLoad({ nativeEvent: { url: props.url } })}
      />
    </div>
  );
}
