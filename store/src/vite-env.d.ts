/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_TEBEX_WEBSTORE_TOKEN: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
