#!/bin/bash

# Must be executed in the top-level source directory.

# Bouncy Castle

bouncycastle1=bcprov-ext-jdk15on-168.jar

rm -f $bouncycastle1
wget --progress=bar https://bouncycastle.org/download/$bouncycastle1

if [ -r "$bouncycastle1" ]; then
    mv $bouncycastle1 Lettera/app/libs/.
else
    echo "Cannot read $bouncycastle1."
fi

echo "Please review Lettera/app/build.gradle and Lettera/app/libs!"
