#!/bin/bash

DEST="openhabian@10.87.1.125:~/"

#for binding in $BINDINGS
#do
	echo "- copying [$binding] binding jar to $DEST ..."

	jar="build/libs/fcmservice-0.0.1-SNAPSHOT.jar"

	if [ -f "$jar" ]
	then
		scp $jar $DEST
	else
		echo "WARNING - jar [$jar] not found"
	fi
#done

exit 0
