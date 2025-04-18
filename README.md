# Practiso
    
[![Build and Release](https://github.com/zhufucdev/Practiso/actions/workflows/compose-app.yml/badge.svg)](https://github.com/zhufucdev/Practiso/actions/workflows/compose-app.yml)
[![GitHub Tag](https://img.shields.io/github/v/tag/zhufucdev/Practiso)](https://github.com/zhufucdev/Practiso/tags)
[![GitHub Release](https://img.shields.io/github/v/release/zhufucdev/Practiso)](https://github.com/zhufucdev/Practiso/releases)

Personal, offline and intelligent study & practice utility. Learn smarter, not more.

<p align="center"><img src="desktopApp/icons/icon.png" alt="Practiso logo" width="200px" /></p>



##  Screenshots


| Name                |                      Screenshot                      |
|:--------------------|:----------------------------------------------------:|
| Home                |        ![home-screen](assets/home-screen.png)        |
| Quiz editor 1       |      ![quiz-editor-1](assets/quiz-editor-1.png)      |
| Quiz editor 2       |      ![quiz-editor-2](assets/quiz-editor-2.png)      |
| Answer screen       |      ![answer-screen](assets/answer-screen.png)      |
| Take details dialog | ![take-details-card](assets/take-details-dialog.png) |

## Project Structure

This is a Kotlin Multiplatform project targeting Android, iOS, Desktop.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
