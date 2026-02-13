# Update versio in pom.xml files

    update-version.sh <new version number>

# Creating release files

## Unsigned release

    mvn clean package 

This command will create the release repository ZIP file in `update_site/target/com.github.ecd-plugin.update-<version>.zip`

* Rename created zip to `com.github.ecd-plugin.update-<version>.unsigned.zip`
* Move zip file to different folder (next build process will delete it otherwise)

# Creating a release with signed JAR files

(For ECD maintainers):

    mvn clean package -Dsigning.disabled=false -Dsigning.password=<password> -Dsigning.keystore=<absolute path-to-keystore>

This command will create the release repository ZIP file in `update_site/target/com.github.ecd-plugin.update-<version>.zip`

* Move zip file to different folder (next build process will delete it otherwise)

# Update site

1. Delete existing plugins and features directory in update site project. 
2. Copy `update_site/target/category.xml` to update site project
3. Extract the content of `com.github.ecd-plugin.update-<version>.zip` (signed version) to the update site project directory.

# Github release

1. Create [Release](https://github.com/ecd-plugin/ecd/releases) with new Git tag. The tag is ECD `<version>.<release-date>` - example: v3.5.0.20240613
2. Copy new changelog entries of the version from `Readme.md` to release description
3. Attach the two generated files `com.github.ecd-plugin.update-<version>.zip` `com.github.ecd-plugin.update-<version>.unsigned.zip`

# Eclipse Marketplace

Update the latest version number and the supported Eclipse versions and update the features list if features has been added/removed.
https://marketplace.eclipse.org/content/enhanced-class-decompiler/edit