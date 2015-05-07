#!/bin/bash

A=A
conf=c1.conf

for N in 10 25 50 75 100 250 500 750 1000 2500 5000 7500 10000 25000 50000
do
for i in 1 2 3 4 5 6 7 8 9 10
do
for t in 1 2 3 4 5
do
java psi.Intersect $conf -q -i ../data/p1024/enc_phones${A}${N}.txt && break
sleep 1
done
done
done
