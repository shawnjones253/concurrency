#!/bin/bash
#Shawn Jones
#CSCI 322, HW2
#Matrix Multiplication
#Threaded vs. non-threaded performance analysis
#creates CSV files from the run-times of my matrix multiplication code

#blank space in upper-left cell
echo -n " , " >> threads.txt
echo -n " , " >> nothreads.txt

#column headers
for ((reps=1; reps <= 32; reps*=2))
do
    echo -n "${reps}, " >> threads.txt
    echo -n "${reps}, " >> nothreads.txt
done

#remove trailing comma
echo -e "\b\b  " >> threads.txt
echo -e "\b\b  " >> nothreads.txt


for ((msize=200; msize <= 800; msize+=200))
do
    #row label
    echo -n "${msize}, " >> threads.txt
    echo -n "${msize}, " >> nothreads.txt

    #execution results
    for ((reps=1; reps <= 32; reps*=2))
    do
      echo -n "`./matrix-multiply ${reps} 1 ${msize} -q`, " >> threads.txt
      echo -n "`./matrix-multiply ${reps} 0 ${msize} -q`, " >> nothreads.txt
    done
    
    #remove trailing comma
    echo -e "\b\b  " >> threads.txt
    echo -e "\b\b  " >> nothreads.txt
done
