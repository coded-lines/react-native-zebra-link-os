// Reexport the native module. On web, it will be resolved to ReactNativeZebraLinkOsModule.web.ts
// and on native platforms to ReactNativeZebraLinkOsModule.ts
export { default } from './ReactNativeZebraLinkOsModule';
export { default as ReactNativeZebraLinkOsView } from './ReactNativeZebraLinkOsView';
export * from  './ReactNativeZebraLinkOs.types';
