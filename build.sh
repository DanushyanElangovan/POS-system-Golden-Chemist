#!/bin/sh
# Compiles the application and pre-compiles the Jasper report templates.
# Needs JDK 8 or newer (javac) on the PATH.
cd "$(dirname "$0")" || exit 1
rm -rf out && mkdir -p out
find src -name '*.java' > out/sources.txt
javac -encoding UTF-8 -nowarn -cp "lib/*" -d out @out/sources.txt || exit 1
java -cp "out:lib/*" com.goldenchemist.pos.report.CompileReports 2>&1 | grep -v -i "warning\|^Note\|\^\|new java\|value = " 
echo "Build finished. Start the program with ./run.sh"
