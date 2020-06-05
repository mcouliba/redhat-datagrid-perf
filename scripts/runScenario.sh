#!/bin/bash

endpoint=$1
type=$2
cache=spectrum_${type}

outputfile=report_${type}.txt

curl --silent "$endpoint/cache/remove?name=${cache}" > /dev/null
curl --silent "$endpoint/cache/create?name=${cache}" > /dev/null

echo > ${outputfile}
for numEntries in 50000 500000 5000000;do
    echo " ---- Scenario with ${numEntries} ----" >> ${outputfile}

    echo $(curl -w "\n" --silent "${endpoint}/cache/fill/${type}?name=${cache}&entries=${numEntries}&threadNum=4") >> ${outputfile}

    for numThreads in 1 4 8;do
        echo " > ${numThreads} threads <" >> ${outputfile}
        for try in $(seq 1 3); do
            echo $(curl -w "\n" --silent "${endpoint}/cache/dump?name=${cache}&threadNum=${numThreads}") >> ${outputfile}
        done
        echo ">>>>>>>><<<<<<<<" >> ${outputfile}
    done

    curl --silent "${endpoint}/cache/clear?name=${cache}" > /dev/null
    sleep 10
    echo " ---- End Scenario with ${numEntries} ----" >> ${outputfile}
done

curl --silent "$endpoint/cache/remove?name=${cache}" > /dev/null

cat ${outputfile}

# curl $1/cache/fill/string?name=spectrum_string&entries=100000&threadNum=4 > /dev/null
# curl $1/cache/dump/string?name=spectrum_string&threadNum=4 > /dev/null