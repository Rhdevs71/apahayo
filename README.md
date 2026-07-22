# Rhpatch

Rhpatch is an LSPosed module designed to enhance the experience of various Android applications, including WhatsApp, Instagram, and more.

## Features
- **WhatsApp Enhancements**: Custom themes, privacy controls, media cleaners, and voice changers.
- **Instagram Patches**: Hide ads (sponsored posts/stories), download media (long click share button), Ghost Mode (hide read receipts for DMs), and more. 
*(Ported using DexKit runtime scanning for maximum compatibility across versions).*
- **YouTube Enhancements**: Supports external downloaders (like YTDLnis) directly from the player.

## Acknowledgements & Open Source Licenses
This project incorporates ideas, architectures, and snippets from the following amazing open-source projects:

- **[Piko](https://github.com/crimera/piko)**: 
  Special thanks to the Piko project for the innovative DexKit-based method fingerprints ("Is ad pod", "__fbt_null__", etc.) used to hook Instagram. We have adapted their static-patching approach into our dynamic Xposed module architecture.

- **[Morphe](https://github.com/MorpheApp/morphe-patches)**:
  Thanks to the Morphe project for inspiring our module architectures and hooking mechanisms.

## Build Instructions
This project utilizes GitHub Actions for automated builds. If you fork this project:
- Push to the `main` branch to trigger builds automatically or manually run the workflows.
- Webhook notifications will automatically be sent to Discord upon build success or failure.

## Support
Ensure you are using the correct versions of the apps as defined in our `arrays.xml` (or dynamically scanned via DexKit). Old unsupported versions may be dropped over time.
