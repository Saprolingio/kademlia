declare -a arr=(
##              "-m16 -k10 -n 16384 -l 3 -a 1" 
##              "-m16 -k5  -n 16384 -l 3 -a 1" 
##              "-m16 -k20 -n 16384 -l 3 -a 1" 
##              "-m16 -k20 -n 3276  -l 3 -a 1" 
##              "-m16 -k10 -n 3276  -l 3 -a 1" 
##              "-m16 -k5  -n 3276  -l 3 -a 1" 
##              "-m16 -k20 -n 655   -l 3 -a 1" 
##              "-m16 -k10 -n 655   -l 3 -a 1" 
##              "-m16 -k5  -n 655   -l 3 -a 1" 

##              "-m10 -k20 -n 1024  -l 3 -a 1" 
##              "-m10 -k10 -n 1024  -l 3 -a 1" 
##              "-m10 -k5  -n 1024  -l 3 -a 1" 
##              "-m10 -k20 -n 512   -l 3 -a 1" 
##              "-m10 -k10 -n 512   -l 3 -a 1" 
##              "-m10 -k5  -n 512   -l 3 -a 1" 
##              "-m10 -k20 -n 256   -l 3 -a 1" 
##              "-m10 -k10 -n 256   -l 3 -a 1" 
##              "-m10 -k5  -n 256   -l 3 -a 1" 
##              "-m10 -k20 -n 51    -l 3 -a 1" 
##              "-m10 -k10 -n 51    -l 3 -a 1" 
##              "-m10 -k5  -n 51    -l 3 -a 1" 


##              "-m8 -k20 -n 256   -l 3 -a 1" 
##              "-m8 -k10 -n 256   -l 3 -a 1" 
##              "-m8 -k5  -n 256   -l 3 -a 1" 
##              "-m8 -k20 -n 128   -l 3 -a 1" 
##              "-m8 -k10 -n 128   -l 3 -a 1" 
##              "-m8 -k5  -n 128   -l 3 -a 1" 

##              "-m4 -k3  -n 8     -l 2 -a 1" 

##              "-m10 -k20 -n 1024    -l 3 -a 1" 
##              "-m20 -k20 -n 1024    -l 3 -a 1" 
##              "-m40 -k20 -n 1024    -l 3 -a 1" 
                
##              "-m10 -k10 -n 1024    -l 3 -a 1" 
##              "-m20 -k10 -n 1024    -l 3 -a 1" 
##              "-m40 -k10 -n 1024    -l 3 -a 1" 

                "-m16 -k20 -n 10000   -l 3 -a 1" 
                "-m16 -k20 -n 4096    -l 3 -a 1" 
                "-m16 -k20 -n 2048    -l 3 -a 1" 
                "-m16 -k20 -n 1024    -l 3 -a 1" 
                "-m16 -k20 -n 512     -l 3 -a 1" 
                "-m16 -k20 -n 256     -l 3 -a 1" 
                "-m16 -k20 -n 56      -l 3 -a 1" 
)

## now loop through the above array
for (( idx=${#arr[@]}-1 ; idx>=0 ; idx-- )) ; do
   echo "time java -jar ./MulasKademlia.jar "${arr[idx]}
   time java -jar ./MulasKademlia.jar ${arr[idx]}
done
