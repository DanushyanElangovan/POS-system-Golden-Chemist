#!/bin/sh
# Headless end-to-end check against the configured database (schema.sql + seed.sql must be loaded).
# NOTE: it creates a few test rows (a test cashier, a test medicine and two invoices).
cd "$(dirname "$0")" || exit 1
rm -rf out-test && mkdir -p out-test
find src test -name '*.java' > out-test/sources.txt
javac -encoding UTF-8 -nowarn -cp "lib/*" -d out-test @out-test/sources.txt || exit 1
java -Dpos.config="${POS_CONFIG:-config/app.properties}" -cp "out-test:lib/*" com.goldenchemist.pos.SmokeTest 2>&1 | grep -E "PASS|FAIL|passed|Exception|Caused"
