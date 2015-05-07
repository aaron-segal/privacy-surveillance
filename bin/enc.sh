#!/bin/bash
for X in A B C
do
for N in 2500 5000 7500 10000 50000
do
echo java psi.Enc ../data/p1024/Keys_pub.txt ../data/plaintext/phones${X}${N}.txt ../data/p1024/enc_phones${X}${N}.txt
done
done
