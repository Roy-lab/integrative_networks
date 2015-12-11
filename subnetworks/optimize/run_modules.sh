# MAIN SCRIPT FOR THIS STEP.
# Runs GAMS/CPLEX to find optimal subnetworks.
# Before running, you should copy/link the gams_input directory from the
# previous step.

pwd=$(pwd)
modules=$1 #test_modules.tab 

NSOLS=40	# gather this many optimal solutions
THREADS=12	# set max threads allowed for CPLEX usage

USAGE="bash run_final_modules.sh module_filename"

if [[ ! -e $modules ]]; then
	echo "Can't find module list file $modules"
	echo $USAGE
	exit
fi

outdir="output"

if [[ ! -d $outdir ]]; then
	mkdir -p $outdir
fi

echo "Running modules in $modules in serial. Putting results in $outdir."

while read i
do
	# if data file doesn't exist, skip
	gms=${pwd}/gams_input/influenza_m${i}.gms
	if [[ ! -e $gms ]]; then
		echo "Data file $gms doesn't exist. Skipping."
		continue
	fi
	
	# if already exists, skip
	rundir=${outdir}/run_m${i}
	if [[ -d $rundir ]]; then
		echo "Skipping module m${i}; directory exists."
		continue
	fi	
	
	outpref=run_m${i}
	
	# get 40 solutions
	# allow 12 threads
	bash scripts/make_main.sh model/ model/main_replace.gms $NSOLS $THREADS $gms $rundir $outpref
	
	# copy the model file over for archival purposes
	cp model/model.gms $rundir
	
	echo "running $i"
	
	# run GAMS
	cd $rundir
	gams main.gms lo=2 
	cd ${pwd}		
		
	if [[ ! -e ${rundir}/gdx ]]; then
		mkdir ${rundir}/gdx
	fi
	if [[ ! -e ${rundir}/summary ]]; then
		mkdir ${rundir}/summary
	fi
		
	mv ${rundir}/*.gdx ${rundir}/gdx 
			
	bash scripts/dump_information.sh ${rundir}/gdx $outpref ${rundir}/results
		
	mv ${rundir}/results/*.tab ${rundir}/summary
	
	# These will take up a lot of space - may want to get rid of them!
	#rm -f ${rundir}/gdx/*.gdx
	#rm -f ${rundir}/*.lst
	
done < ${modules}
