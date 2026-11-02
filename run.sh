#!/bin/sh
cd "$(dirname "$0")" || exit 1
[ -d out ] || ./build.sh || exit 1
exec java -cp "out:lib/*" com.goldenchemist.pos.App
