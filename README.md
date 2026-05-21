# SD Prompt Viewer

An Android app for reading and browsing metadata (prompts, LoRA, InstantID, settings) embedded in Stable Diffusion generated images.

## Features

- **Gallery view** — browse SD images in any folder, including hidden folders (e.g. `.mi`, `.thumbnails`)
- **Metadata extraction** — reads PNG tEXt/iTXt chunks and JPEG EXIF UserComment directly on-device, no network required
- **Prompt display** — positive / negative prompts with one-tap copy
- **LoRA info** — name and weight (model/clip) extracted from prompt text and ComfyUI nodes
- **InstantID detection** — detects InstantID usage from ControlNet settings or ComfyUI node graph
- **Settings tab** — seed, steps, CFG scale, sampler, model, etc.

## Supported formats

| Format | PNG | JPEG |
|---|---|---|
| Automatic1111 / AUTOMATIC1111 | ✅ | ✅ |
| ComfyUI | ✅ | — |
| Fooocus | ✅ | — |
| InvokeAI | ✅ | — |
| StableSwarmUI | ✅ | — |

## Requirements

- Android 8.0 (API 26) or higher
- Storage permission (All files access) — required to read hidden folders

## Build

1. Open the project in **Android Studio** (Meerkat 2024.3 or later recommended)
2. **Build → Generate App Bundles or APKs → Build APK(s)**
3. Install `app/build/outputs/apk/debug/app-debug.apk` on your device

## How it works

The app uses a `WebView` + `JavascriptInterface` bridge to combine a native Android file system layer with a pure JS metadata parser. PNG chunks and JPEG EXIF data are read natively and passed as Base64 to the JS layer, which parses them without any server or external dependency. Images are served to the WebView via a custom `localfile://` URL scheme intercepted by `shouldInterceptRequest`.

## Privacy

All processing is done entirely on-device. No images or metadata are ever sent to any server.

## License

MIT
