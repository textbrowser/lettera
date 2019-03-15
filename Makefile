# You must have at least one AVD defined.

ADB = ~/Android/Sdk/platform-tools/adb
EMULATOR = ~/Android/Sdk/tools/emulator
GRADLEW = ./Lettera/gradlew
JARSIGNER = "/snap/android-studio/current/android-studio/jre/bin/jarsigner"
JDK = "/snap/android-studio/current/android-studio/jre"
export JAVA_HOME = /snap/android-studio/current/android-studio/jre

all:
	$(GRADLEW) -Dorg.gradle.java.home=$(JDK) \
	--build-file Lettera/build.gradle assembleDebug \
	--configure-on-demand --daemon --parallel

clean:
	rm -f Lettera/app/src/main/assets/lettera.src.d.zip
	rm -f lettera.src.d.zip
	$(GRADLEW) --build-file Lettera/build.gradle clean

clear-lettera:
	./adb.bash shell pm clear org.purple.lettera

debug-with-source: all
	rm -rf Lettera/build Lettera/captures
	mkdir -p Lettera/app/src/main/assets
	zip -r lettera.src.d.zip \
	Android \
	Documentation \
	Makefile \
	Lettera \
	TO-DO \
	adb.bash \
	-x *.git* -x *.gradle* -x *.idea* \
	&& mv lettera.src.d.zip Lettera/app/src/main/assets/.
	$(GRADLEW) -Dorg.gradle.java.home=$(JDK) \
	--build-file Lettera/build.gradle assembleDebug \
	--configure-on-demand --daemon --parallel
	rm -f Lettera/app/src/main/assets/lettera.src.d.zip

distclean: clean kill-adb-server kill-gradle-daemon
	rm -f lettera.db

kill-adb-server:
	$(ADB) kill-server

kill-gradle-daemon:
	$(GRADLEW) --stop

launch-emulator-1:
	$(EMULATOR) -netdelay none -netspeed full -avd \
	`$(EMULATOR) -list-avds | sort | sed "1q;d"` &

launch-emulator-2:
	$(EMULATOR) -netdelay none -netspeed full -avd \
	`$(EMULATOR) -list-avds | sort | sed "2q;d"` &

list-devices:
	$(ADB) devices -l

list-files:
	./adb.bash shell run-as org.purple.lettera \
	ls -l /data/data/org.purple.lettera/databases

load-apk: all
	./adb.bash install -r \
	./Lettera/app/build/outputs/apk/debug/apk/lettera.apk
	./adb.bash shell am start -S -W \
	-n org.purple.lettera/org.purple.lettera.Lettera \
	-a android.intent.action.MAIN -c android.intent.category.LAUNCHER

load-apk-release: release
	$(JARSIGNER) -verbose -keystore \
	~/Android-Keys/lettera-release.keystore \
	./Lettera/app/build/outputs/apk/release/apk/lettera.apk lettera-release
	$(JARSIGNER) -verify \
	./Lettera/app/build/outputs/apk/release/apk/lettera.apk
	./adb.bash install -r \
	./Lettera/app/build/outputs/apk/release/apk/lettera.apk
	./adb.bash shell am start -S -W \
	-n org.purple.lettera/org.purple.lettera.Lettera \
	-a android.intent.action.MAIN -c android.intent.category.LAUNCHER

pull-database:
	./adb.bash exec-out run-as org.purple.lettera \
	cat /data/data/org.purple.lettera/databases/lettera.db > lettera.db

purge:
	find . -name '*~*' -exec rm -f {} \;

release: clean
	rm -rf Lettera/build Lettera/captures
	mkdir -p Lettera/app/src/main/assets
	zip -r lettera.src.d.zip \
	Android \
	Documentation \
	Makefile \
	Lettera \
	TO-DO \
	adb.bash \
	-x *.git* -x *.gradle* -x *.idea* \
	&& mv lettera.src.d.zip Lettera/app/src/main/assets/.
	$(GRADLEW) -Dorg.gradle.java.home=$(JDK) \
	--build-file Lettera/build.gradle assembleRelease \
	--configure-on-demand --daemon --parallel
	rm -f Lettera/app/src/main/assets/lettera.src.d.zip

remove-database:
	./adb.bash shell run-as org.purple.lettera \
	rm -f /data/data/org.purple.lettera/databases/lettera.db
	./adb.bash shell run-as org.purple.lettera \
	rm -f /data/data/org.purple.lettera/databases/lettera.db-journal

stop-lettera:
	./adb.bash shell am force-stop org.purple.lettera
