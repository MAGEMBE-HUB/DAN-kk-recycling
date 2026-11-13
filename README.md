# DAN KK Recycling Centre System

A browser-based local app for recording recyclable-material intake. It stores
records in SQLite. Its dashboard records intake and shows centre totals and
recent transactions; a Suppliers / collectors page manages supplier names and
shows each supplier's intake totals and history. The browser app also includes
connected Home, About us, and Services pages, with live centre totals on Home
and links from services to the intake and supplier workflows.
The browser is the system's only user interface; the Java process runs the
local server and opens the Home page in the default browser.
The built-in Java HTTP server listens only on this computer (`127.0.0.1`); the
dashboard and its assets work offline and use no external services.

## Requirements

- Java 25 JDK
- The SQLite JDBC driver is included in the project under `lib/`

## Launch

### Windows

On this computer, DAN KK Recycling System starts automatically after you sign
in to Windows and opens the Home page in your default browser. Keep its console
window open while using the app; closing the window stops the local server.

To start it manually, double-click `run.bat` in the project folder. It compiles
the app, starts the local server, and opens the Home page in your default
browser. You can also launch it from PowerShell in the project folder:

```powershell
.\run.bat
```

Then open <http://127.0.0.1:8765/> if the browser does not open automatically.
Use the top navigation to switch among Home, About us, Services, Dashboard,
and Suppliers. The dashboard lets you record intakes and review totals and
recent transactions. Service links lead directly to intake or supplier
management. The suppliers page supports adding and renaming suppliers, searching the list,
and deleting suppliers that have no intake history. Names with saved intakes
cannot be deleted, and renaming updates the historical records.
The launcher compiles the application using the SQLite JDBC jar included in
the project. No internet connection or Maven installation is needed to launch
and use the app, as long as Java 25 JDK is installed and the project folder
still contains `lib/sqlite-jdbc-3.49.1.0.jar`.

The database is created automatically at `data/dankk-recycling.db` relative to
the project folder. Each intake requires a supplier/collector, material type,
weight in kilograms (up to 3 decimal places), and a positive rate per kilogram
(up to 2 decimal places). Values are stored in SQLite and remain available
after the app closes or the computer restarts.
