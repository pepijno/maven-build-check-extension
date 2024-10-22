# maven-build-check-extension

Maven extension which checks if maven should rebuild modules.

## Installation

To install locally, run the following:

```shell
git clone https://github.com/pepijno/maven-build-check-extension.git
cd maven-build-check-extension
mvn clean install
```

### Maven (mvn)

If you use `mvn` add the following to your `-/.mavenrc` (run `touch ~/.mavenrc` if it does not exist): 

```shell
BUILD_CHECK="$MAVEN_REPO_HOME/nl/pepijno/maven-build-check-extension/0.0.1/maven-build-check-extension-0.0.1.jar"
if [ -z "${MAVEN_OPTS}" ]; then
        MAVEN_OPTS="-Dmaven.ext.class.path=$BUILD_CHECK"
else
        MAVEN_OPTS="$MAVEN_OPTS -Dmaven.ext.class.path=$BUILD_CHECK"
fi
```

### Maven deamon (mvnd)

If you use `mvnd`, run

```shell
echo "mvnd.extClasspath=$MAVEN_REPO_HOME/nl/pepijno/maven-build-check-extension/0.0.1/maven-build-check-extension-0.0.1.jar" >> ~/.m2/mvnd.properties
```

## Usage

To use the extension add `-Dbuild.check.enabled` to your maven commands. For example

```shell
mvn install -Dbuild.check.enabled
```

or 

```shell
mvnd install -Dbuild.check.enabled
```

When using the `clean` lifecycle, the extension does not check and will always execute the next maven lifecycles.