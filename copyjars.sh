#!/bin/bash

DEST="openhabian@10.87.1.125:~/fcmservice"
JAR="fcmservice-0.0.1-SNAPSHOT.jar"

#for binding in $BINDINGS
#do
	echo "- copying [$JAR] to $DEST ..."

	jar="build/libs/${JAR}"

	if [ -f "$jar" ]
	then
		scp $jar $DEST
	else
		echo "WARNING - jar [$jar] not found"
	fi
#done

exit 0
