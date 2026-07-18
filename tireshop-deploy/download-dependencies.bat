@echo off
echo Downloading required Java dependencies
echo =====================================
echo.

REM Create lib directory if it doesn't exist
if not exist "lib" mkdir lib

REM Set Maven repository URL
set MAVEN_REPO=https://repo1.maven.org/maven2

echo Setting up dependencies for Tire Shop POS System...
echo.

echo Step 1: Downloading Hibernate ORM and JPA dependencies...
curl -L -o lib\hibernate-core-5.6.15.Final.jar %MAVEN_REPO%/org/hibernate/hibernate-core/5.6.15.Final/hibernate-core-5.6.15.Final.jar
curl -L -o lib\hibernate-commons-annotations-5.1.2.Final.jar %MAVEN_REPO%/org/hibernate/common/hibernate-commons-annotations/5.1.2.Final/hibernate-commons-annotations-5.1.2.Final.jar
curl -L -o lib\javax.persistence-api-2.2.jar %MAVEN_REPO%/javax/persistence/javax.persistence-api/2.2/javax.persistence-api-2.2.jar
curl -L -o lib\jboss-logging-3.4.3.Final.jar %MAVEN_REPO%/org/jboss/logging/jboss-logging/3.4.3.Final/jboss-logging-3.4.3.Final.jar
curl -L -o lib\jaxb-api-2.3.1.jar %MAVEN_REPO%/javax/xml/bind/jaxb-api/2.3.1/jaxb-api-2.3.1.jar
curl -L -o lib\jaxb-runtime-2.3.1.jar %MAVEN_REPO%/org/glassfish/jaxb/jaxb-runtime/2.3.1/jaxb-runtime-2.3.1.jar
curl -L -o lib\javassist-3.29.0-GA.jar %MAVEN_REPO%/org/javassist/javassist/3.29.0-GA/javassist-3.29.0-GA.jar
curl -L -o lib\byte-buddy-1.12.18.jar %MAVEN_REPO%/net/bytebuddy/byte-buddy/1.12.18/byte-buddy-1.12.18.jar
curl -L -o lib\antlr-2.7.7.jar %MAVEN_REPO%/antlr/antlr/2.7.7/antlr-2.7.7.jar
curl -L -o lib\jandex-2.4.2.Final.jar %MAVEN_REPO%/org/jboss/jandex/2.4.2.Final/jandex-2.4.2.Final.jar
curl -L -o lib\classmate-1.5.1.jar %MAVEN_REPO%/com/fasterxml/classmate/1.5.1/classmate-1.5.1.jar
curl -L -o lib\javax.activation-api-1.2.0.jar %MAVEN_REPO%/javax/activation/javax.activation-api/1.2.0/javax.activation-api-1.2.0.jar
curl -L -o lib\txw2-2.3.1.jar %MAVEN_REPO%/org/glassfish/jaxb/txw2/2.3.1/txw2-2.3.1.jar
curl -L -o lib\istack-commons-runtime-3.0.7.jar %MAVEN_REPO%/com/sun/istack/istack-commons-runtime/3.0.7/istack-commons-runtime-3.0.7.jar
curl -L -o lib\FastInfoset-1.2.15.jar %MAVEN_REPO%/com/sun/xml/fastinfoset/FastInfoset/1.2.15/FastInfoset-1.2.15.jar
curl -L -o lib\stax-ex-1.8.jar %MAVEN_REPO%/org/jvnet/staxex/stax-ex/1.8/stax-ex-1.8.jar

echo Step 2: Downloading database driver...
curl -L -o lib\h2-2.1.214.jar %MAVEN_REPO%/com/h2database/h2/2.1.214/h2-2.1.214.jar

echo Step 3: Downloading JSON processing libraries...
curl -L -o lib\json-simple-1.1.1.jar %MAVEN_REPO%/com/googlecode/json-simple/json-simple/1.1.1/json-simple-1.1.1.jar

echo Step 4: Downloading ZXing barcode libraries...
curl -L -o lib\core-3.5.1.jar %MAVEN_REPO%/com/google/zxing/core/3.5.1/core-3.5.1.jar
curl -L -o lib\javase-3.5.1.jar %MAVEN_REPO%/com/google/zxing/javase/3.5.1/javase-3.5.1.jar

echo Step 5: Downloading additional utility libraries...
curl -L -o lib\slf4j-api-1.7.36.jar %MAVEN_REPO%/org/slf4j/slf4j-api/1.7.36/slf4j-api-1.7.36.jar
curl -L -o lib\slf4j-simple-1.7.36.jar %MAVEN_REPO%/org/slf4j/slf4j-simple/1.7.36/slf4j-simple-1.7.36.jar
curl -L -o lib\dom4j-2.1.3.jar %MAVEN_REPO%/org/dom4j/dom4j/2.1.3/dom4j-2.1.3.jar

echo Counting downloaded dependencies...
set JAR_COUNT=0
for %%F in (lib\*.jar) do (
    set /a JAR_COUNT+=1
)

echo.
echo Downloaded %JAR_COUNT% JAR files to the lib directory.
echo Dependencies are now ready for use.
echo.

echo To use these dependencies, make sure to include them in your classpath:
echo javac -cp ".;lib\*" ...
echo java -cp ".;lib\*" ...
echo.

echo Dependencies download completed.
echo.
pause 