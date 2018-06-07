# Equinox
[![Apache License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

Data analysis and visualisation software for aircraft-digital-twin platform. Digital-twin platform aims at creating a digital fatigue representation of aircraft structure. This project contains the prototype desktop client application of the platform, named as Equinox.

## Screenshots
Screenshots below demonstrate some of the analysis and data visualization capabilities of Equinox.

<img width="1528" alt="shots" src="https://user-images.githubusercontent.com/13915745/40891639-b25e01c2-6789-11e8-9142-80a913a040b8.png">

## Video demonstrations
Here's a 5 minutes [screencast](https://youtu.be/k49bgTfAgVU) demonstrating some of the analysis and data visualization capabilities of Equinox. 3D data visualizations are demonstrated in this short [video](https://youtu.be/RM_ofreMsaQ).

## Frameworks used for development
The software is developed using the following major frameworks and libraries:
- Git for version control,
- Maven for dependency management,
- e(fx)clipse for generating Ant build files,
- InnoSetup for building Windows OS installation packages,
- JavaFX for user interface development,
- Apache Derby for embedded in-memory SQL database,
- HikariCP for database connection pooling,
- Kryonet for TCP networking with digital-twin server,
- VTK (Visualization Toolkit) for openGL 3D data visualization,
- JFreeChart for 2D charts and graphs,
- JSch (Java Secure Channel) for SMTP file server connection,
- iText for automatic PDF report generation

## Multi-threading in Equinox
Equinox is a highly multi-threaded application. All features of the application are performed by background tasks running in thread pools. In addition to multi-threading, it also supports peer-to-peer task sharing among Equinox clients.

![parallel](https://user-images.githubusercontent.com/13915745/40908265-85e1694c-67e6-11e8-9281-d936482992c9.gif)
