# Nami

1.21.8 and 1.21.5

<p align="left">
  <a href="https://github.com/Kiriyaga7615/Nami/releases">
    <img src="https://img.shields.io/github/downloads/Kiriyaga7615/Nami/total?color=green&label=Total%20Downloads" alt="Total Downloads" />
  </a>
  <a href="https://github.com/Kiriyaga7615/Nami/commits">
  <img src="https://img.shields.io/github/commit-activity/m/Kiriyaga7615/Nami?label=Commits%20(last%20month)&color=yellow" alt="month" />
  </a>
  <a href="https://github.com/Kiriyaga7615/Nami/releases">
    <img src="https://img.shields.io/github/v/release/Kiriyaga7615/Nami?color=blue&label=Latest%20Release" alt="Latest Release" />
  </a>
  <a href="https://github.com/Kiriyaga7615/Nami/blob/master/LICENSE">
    <img src="https://img.shields.io/github/license/Kiriyaga7615/Nami?color=blue" alt="License" />
  </a>
  <a href="https://discord.gg/qy3eS42beW">
    <img src="https://img.shields.io/discord/1298742596633497744?color=7289DB&label=Discord" alt="Discord" />
  </a>
</p>

![# badge](Assets/Readme/no-stops-no-regrets.svg)
![# badge](Assets/Readme/ensuring-code-integrity.svg)
![# badge](Assets/Readme/works-on-selfmerging.svg)


**Nami** is a modular and lightweight anarchy client base built for PVE and automation.  

Most popular Minecraft clients are closed-source, paid, and obfuscated, making them difficult to audit or trust. Some may include backdoors or malicious code.

This project started as a clean, open-source alternative aiming to be transparent, secure, and easy to extend without relying on unsafe third-party clients.

---

## Screenshots

<details>
<summary>View screenshots</summary>

<img width="1920" height="1080" alt="ClickGUI" src="Assets/clickgui.png" />
<img width="1920" height="1080" alt="HudEditor" src="Assets/hudeditor.png" />
<img width="1920" height="1080" alt="Friends" src="Assets/friends.png" />

</details>

---

## FAQ

<details>
<summary>How to open ClickGUI?</summary>

ClickGUI is not bound by default. Use the command:  
`-bind clickgui KEY`  
to bind it to your preferred key.

</details>

<details>
<summary>What is the command prefix?</summary>

The default command prefix is `-`.

</details>

---

## Requirements

- Java 21  
- Gradle 8+  
- Minecraft 1.21.8  
- Fabric loader, API

---

## How to Build

1. Clone the repository:

    ```bash
    git clone https://github.com/Kiriyaga7615/Nami.git  
    cd nami
    ```

2. Build with Gradle:

    Linux / macOS
    ```bash
    ./gradlew build
    ```

    Windows  
    ```bat
    gradlew.bat build
    ```

The compiled JAR will be located at:  
`build/libs/nami-<version>.jar`

Do not launch -unremapped!

---

## License

This project is licensed under the MIT License. You are free to contribute, distribute, fork, or reuse any part.

---

## Special Thanks

- [cattyngmd](https://github.com/cattyngmd)

- [CatFormat](https://github.com/cattyngmd/CatFormat)
