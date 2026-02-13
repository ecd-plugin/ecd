# Enhanced Class Decompiler
Enhanced Class Decompiler integrates **JD**, **Fernflower**, **Vineflower**, **CFR**, **Procyon** seamlessly with Eclipse and allows Java developers to **debug class files without source code directly**. It also integrates with the eclipse class editor, m2e plugin, supports **Javadoc**,  **reference search**, **library source attaching**, **byte code view** and the syntax of JDK8 **lambda** expression.

<p align="center"><img src="https://ecd-plugin.github.io/ecd/doc/o_debug_class.png"></p>

## Description
Enhanced Class Decompiler is a plug-in for the Eclipse platform. It integrates JD, Fernflower, Vineflower, CFR, Procyon seamlessly with Eclipse, allows you to display all the Java sources during your debugging process, even if you do not have them all, and you can debug these class files without source code directly.

## Why is this plug-in "enhanced"?
This is an ad-free fork of the Eclipse Decompiler Plugin. So we enhanced it by removing all code which might compromise your privacy or security (to the best of our knowledge).

## Supported Eclipse Versions

Enhanced Class Decompiler requires at least Eclipse 4.8 (Photon) or newer.

## How to install Enhanced Class Decompiler?

Drag and Drop installation: [![Drag to your running Eclipse workspace.](https://marketplace.eclipse.org/modules/custom/eclipsefdn/eclipsefdn_marketplace/images/btn-install.svg)](https://marketplace.eclipse.org/marketplace-client-intro?mpc_install=3644319 "Drag to your running Eclipse workspace.")


_If you have currently the "Eclipse" Class Decompiler installed, it is recommended to uninstall that plug-in first and remove the corresponding update site from your Eclipse installation._
  1. Launch _Eclipse_,
  2. Click on _"Help > Install New Software..."_,
  3. Click on button _"Add..."_ to add an new repository,
  4. Enter name as _"Enhanced Class Decompiler Update Site"_ and enter location as _"[https://ecd-plugin.github.io/update](https://ecd-plugin.github.io/update)"_, then click on button _"OK"_,
  5. Check _"Enhanced Class Decompiler"_,
  6. Next, next, next... and restart.

## How to check the file associations?
  1. Click on _"Window > Preferences > General > Editors > File Associations"_
  2. _"*.class"_ : _"Class Decompiler Viewer"_ is selected by default.
  3. _"*.class without source"_ : _"Class Decompiler Viewer"_ is selected by default.

## How to configure Enhanced Class Decompiler?
  1. Click on _"Window > Preferences > Java > Decompiler"_

## How to uninstall Enhanced Class Decompiler?
  1. Click on _"Help > About Eclipse > Installation Details > Installation Software"_,
  2. Select _"Enhanced Class Decompiler"_,
  3. Click on _"Uninstall..."_.

## How to build from source?

  Requiremnent: JDK 17 (make sure `JAVA_HOME` environment variable points to an JDK 17)

  If you want to test the latest features of this plugin, you have to build it from source. For this, proceed as following:

  1. `git clone https://github.com/ecd-plugin/ecd`
  2. Run `cd ecd`
  3. Run `mvn clean package`

  If you want to use Eclipse and help developing, continue like this:

  4. Install _Eclipse for RCP and RAP Developers_
  3. Import all projects into Eclipse by selecting _File_ > _Import_ > _General_ > _Existing Projects into Workspace_ > _Next_ and enter the parent of the cloned directory as "root directory".
  4. Open the _org.sf.feeling.decompiler.updatesite_ project in the Package Explorer
  5. Open the file _site.xml_ within the project
  6. Press "Build All"
  7. Copy the jar files generated in the _build/features_ and _build/plugins_ folder of the project into the correspondent folders of your normal Eclipse installation.

## Plugin Signature

Since version 3.3.0 ECD is signed by a self-signed 4096 bit RSA key:

* Subject: `CN=ECD Software Distribution,OU=ECD,O=ECD`
* SHA-1 fingerprint: 2D DB EE 7E 07 32 EB 0D 7C F2 FF C6 68 A0 C4 B8 B9 58 40 29
* SHA-256 fingerprint: 8A 68 55 D3 91 B7 6F 95 DA D1 1E DF 1C 38 8D 38 F1 8A 0C A2 97 E5 12 85 DD 5B 05 9C C3 21 1B D4
* Certificate file: [ecd.cer](ecd.cer)

## Eclipse Photon to Eclipse 2021-03

Due to a signature bug in all Eclipse versions up to (including) Eclipse 2021-03, the signed releases of Enhanced Class Decompiler (ECD) 3.4.1 and newer can no longer be used. Please download the latest unsigned repository from the [ECD releases](https://github.com/ecd-plugin/ecd/releases) and install it manually in Eclipse.

This affects the release version published on https://ecd-plugin.github.io/update/ and also the version installable via Eclipse Marketplace as it uses this update site.

## Changelog
* Version 3.5.2
  * Reworked output formatter that uses the line number in comments from the decompiler to build source code with correct line numbers for debugging
  * Do not set Fernflower as default decompiler if it's plugin is not installed
  * Fix plugin.xml referencing nonexistent class `org.sf.feeling.decompiler.actions.DecompileWithFernflowerHandler`
  * Remove unused `org.apache.commons.codec` feature dependency
  * Fix NullPointerException in UIUtils.getPathLocation()

* Version 3.5.1
  * Use AST to remove block comments
  * For newer Eclipse versions use methods to get the latest supported Java version (required for Eclipse 2024-06) 

* Version 3.5.0
  * Moved Fernflower into own plugin/feature
  * Quiltflower plugin and feature removed
  * Vineflower plugin and feature added (replaces Quiltflower)
  * Removed "Source" (source.attach) plugin and feature
  * Updated build environment to use Java 17

* Version 3.4.0
  * Updated Embedded Decompilers
    * Fernflower updated to 232.10203.10
     
* Version 3.3.0
  * ECD releases are now signed with a self-signed certificate
  * Removed Jad decompiler (decompiler was released 2001)
  * Quiltflower plugin and feature added
  * Updated Embedded Decompilers
    * CFR to version 0.152 (JDK 14 support)
    * Procyon to version 0.6.0

* Version 3.2.2
  * Fixed some inconsistencies regarding version numbers and the update site
  * Upgraded some service URL to HTTPS

* Version 3.2.1
  * Updated Embedded Decompilers
    * CFR to version 0.15.1 (JDK 14 support)
    * Procyon to version 0.5.36

* Version 3.2.0
  * Updated Embedded Decompilers
    * JD-Core to version 1.1.3 (JDK 12 support)

* Version 3.1.1
  * #44 Fixed that setting breakpoints might not work.
  * #52 Fixed that hyperlinks might have oddly.

* Version 3.1.0
  * Updated Embedded Decompilers
    * CFR to version 0_130
  * Verified and updated embedded decompilers
  * Fixed that activating a tab with a decompiled source would reset the selected line location
  * Fixed and tested compatibility with Eclipse Photon
  * Tested compatibility with JDK11.

* Version 3.0.0
  * Removed adware code fragments
  * Removed self-update feature (Eclipse handles updates fine)
  * Removed extra-header in code (Issue [upstream#2](https://github.com/cnfree/Eclipse-Class-Decompiler/issues/28))
  * Display information about class being decompiled in editor title (Issue #3)
  * Made plug-in work with Java9 BETA JDT core

## Licenses

The main plugin and the _org.sf.feeling.decompiler.jd_ project are licensed under [GPL 3](https://www.gnu.org/licenses/gpl-3.0-standalone.html), the other feature plugins are licensed under the [Eclipse Public License v1.0](https://www.eclipse.org/legal/epl-v10.html)

Code partially based on:
  * JD-Eclipse: Copyright Emmanuel Dupuy, [GPL 3](https://www.gnu.org/licenses/gpl-3.0-standalone.html)
  * Java Source Attacher: Copyright Thai Ha, [Apache License V2.0](https://www.apache.org/licenses/LICENSE-2.0.html)

Used libraries:
  * Dr. Garbage Tools: Copyright (c) Dr. Garbage Ltd. & Co KG, [Apache License V2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
  * Apache commons: Copyright (c) Apache Software Foundation, [Apache License V2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
  * Fernflower: Copyright (c) JetBrains, [Apache License V2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
  * Minimal JSON: Copyright (c) 2013, 2014 EclipseSource, [MIT License](https://opensource.org/licenses/MIT)
  * CFR: Copyright Leee Benfield, [MIT License](https://opensource.org/licenses/MIT)
  * Procyon: Copyright Mike Strobel, [Apache License V2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
  * Netbeans CVSClient: Copyright (c) NetBeans Community, [Eclipse Public License v1.0](https://www.eclipse.org/legal/epl-v10.html) and [Apache License V2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
  * Maven SCM: Copyright (c) Apache Software Foundation, [Apache License V2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
  * Nexus Indexer: [Apache License V2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
  * Nexus Restlet1x Model: [Eclipse Public License v1.0](https://www.eclipse.org/legal/epl-v10.html)
  * Plexus Utils: Copyright The Codehaus Foundation, [Apache License V2.0](https://www.apache.org/licenses/LICENSE-2.0.html)
  * JD-Core: Copyright Emmanuel Dupuy, [GPL 3](https://www.gnu.org/licenses/gpl-3.0-standalone.html)
  * Vineflower: [Apache License V2.0](https://www.apache.org/licenses/LICENSE-2.0.html)

## Contributors

* Chen Chao (cnfree2000@hotmail.com) - initial API and implementation
* Robert Zenz
* Pascal Bihler
* Nick Lombard
* Jan Peter Stotz
* Nicolas Baumann (@nbauma109)
