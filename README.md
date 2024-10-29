# Android-VBMeta-Fixer
A Magisk/KernelSU/Apatch module to fix VBMeta detections on Android. <br>
It works by performing **key attestation** and setting the system property `ro.boot.vbmeta.digest` to the verified boot hash result.

## Features
- Utilizes key attestation to generate a verified boot hash
- Sets the `ro.boot.vbmeta.digest` system property to the verified boot hash

## Limitations
Please note that this tool currently **does not verify** if `vbmeta` should be set, so there may be issues depending on your device configuration.

## Key Attestation Credit
This tool uses key attestation code from [Key Attestation by vvb2060](https://github.com/vvb2060/KeyAttestation).

## Contributing
I welcome contributions to improve this tool! If you'd like to contribute, please feel free to submit a pull request or reach out with ideas.

## Issues
If you encounter any problems, please **open an issue** here on GitHub. Providing detailed information about your device and OS version can help speed up the troubleshooting process.

## Contact
For any questions, collaboration requests, or updates, feel free to reach out via:

Telegram Channel: [Join Channel](https://t.me/reveny1) <br>
Telegram Contact: [Contact Me](https://t.me/revenyy) <br>
Website: [My Website](https://reveny.me) <br>
Email: [contact@reveny.me](mailto:contact@reveny.me) <br>

## License
This project is licensed under the GPLv3 License. See the [LICENSE](LICENSE) file for details.
