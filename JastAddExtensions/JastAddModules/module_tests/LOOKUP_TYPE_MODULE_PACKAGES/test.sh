#!/bin/sh
../testlib/pretest.sh
../testlib/ja-modules.sh -jastaddframework  -debug -d classes -instance-module m1 *.java *.module ../testlib/*.module ../testlib/*.java m2/*.java 2>&1 > out
../testlib/posttestclasses.sh
