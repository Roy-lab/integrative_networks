#!/bin/sh 
# Creates GAMS run file and CPLEX opt files for specified parameters.
# Runs the solution pool method for acquiring multiple solutions.
# If you only ask for one solution, you won't necessarily get the optimal one.

# Usage: ./make_main.sh MODELDIR NSOL THREADS SETFN OUTDIR OUTPREF
# Arguments:
#	MODELDIR	location of model file, GAMS and CPLEX templates
#	RUNFILE	name of run file
#	NSOL	number of solutions 
#	SETFN	GAMS set file containing network information
#	OUTDIR	output directory 
#	OUTPREF	final GDX filename prefix

USAGE="./make_main.sh MODELDIR RUNFILE NSOL THREADS SET_FN OUTDIR OUTPREF\n
Arguments:\n
MODELDIR	location of model file, GAMS and CPLEX templates\n
RUNFILE	name/location of run file master\n
NSOL	number of solutions (should be > 1!)\n
THREADS	number of threads\n
SETFN	GAMS set file containing network information\n
OUTDIR	output directory\n
OUTPREF	final GDX filename prefix"

MODELDIR=$1
MAIN=$2
NSOL=$3
THREADS=$4
SETFN=$5
OUTDIR=$6
OUTPREF=$7

# Templates for GAMS/CPLEX files
CP1=$MODELDIR/cplex.opt_replace
CPPOOL=$MODELDIR/cplex.op2_replace

# Check legitimacy of arguments
fail=false

echo $#
if [[ $# -ne 7 ]]; then
	echo "Not enough arguments"
	fail=true
fi

if [[ ! -d ${MODELDIR} ]]; then
	echo "Cannot find model/template directory ${MODELDIR}"
	fail=true
else	
	for tf in $MAIN $CP1 $CPPOOL; do
		if [[ ! -e ${tf} ]]; then
			echo "Cannot find template file ${tf}; Please check the model directory!"
			fail=true
		fi
	done	
fi

if [[ ! -e ${SETFN} ]]; then
	echo "Cannot find set file ${SETFN}"
	fail=true
fi

if [[ $fail = true ]]; then
	echo -e $USAGE
	exit 2
fi

if [[ ! -e ${OUTDIR} ]]; then
	echo "Cannot find output directory; making it."
	mkdir ${OUTDIR}
fi

# Start filling in templates
wd=$(pwd)

# GAMS main file
sed "s|{NSOL}|$NSOL|;s|{SETFN}|$SETFN|;s|{OUTPREF}|${OUTPREF}|" $MAIN > ${OUTDIR}/main.gms

# CPLEX files - threads and solutions
sed "s/{THREADS}/$THREADS/" $CP1 > ${OUTDIR}/cplex.opt
sed "s/{THREADS}/$THREADS/;s/{NSOL}/${NSOL}/" $CPPOOL > ${OUTDIR}/cplex.op2

# Copy in model 
#cp ${MODELDIR}/model.gms ${OUTDIR}

echo "Set up cplex.opt, cplex.op2, model.gms, and main.gms in ${OUTDIR}. Main GAMS file: main.gms."
