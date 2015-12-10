#!/bin/bash
# Extracts high-confidence regulators for modules in MODFILE

RESULTS=$1	# location of mtglasso/lasso output
MODFILE=$2	# module file (one per line)
LAM=$3	# lambda
CUTOFF=$4	# weight cutoff
FREQ=$5	# frequency cutoff
OUT=$6	# output location

# location of script
GETREGS=get_regulators_from_weights.py

if [[ ! -d $OUT ]]; then
	mkdir -p $OUT
fi

while read mod
do
	echo $mod
	# output filename
	fn=${OUT}/human_mtglasso_${mod}_${CUTOFF}_regs.tab 
	
	# get regs that pass the cutoff/significance test
	# cutoff : regression weight is higher than cutoff, and random reg weights are not
	# (bonferroni corrected p-value)
	#if [[ ! -e ${fn} ]]; then
	python $GETREGS ${RESULTS} $mod $CUTOFF > ${fn}
	#fi
	
	# get regs that pass the test
	grep ${mod}$'\t'$LAM ${fn} | awk '$7 > 0 {print $3}' | sort > temp_mod_significant
	
	# intersect with regs that pass the frequency cutoff (over the 10 folds)
	awk -v f=$FREQ '$2>=f' ${RESULTS}/module${mod}_consensus_regs_lam${LAM}.tab | cut -f 1 | sort > temp_mod_freq
	
	comm temp_mod_significant temp_mod_freq -2 -1 > ${OUT}/human_mtglasso_${mod}_regs_only.tab	
			
	#cat ${OUT}/human_mtglasso_${mod}_regs_only.tab	
	
	# skip if no proteins found - may be the case for sparse
	numprots=$(wc -l ${OUT}/human_mtglasso_${mod}_regs_only.tab)
		
	if [[ $numprots == 0* ]]; then
		echo "No regs for module ${mod}"
		continue;
	fi	
done < $MODFILE
